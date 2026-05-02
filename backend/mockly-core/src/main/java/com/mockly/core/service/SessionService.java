package com.mockly.core.service;

import com.mockly.core.dto.session.CreateSessionRequest;
import com.mockly.core.dto.session.SessionListResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.core.mapper.SessionMapper;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.enums.ParticipantRole;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.SessionParticipantRepository;
import com.mockly.data.repository.SessionRepository;
import com.mockly.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;





@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SessionMapper sessionMapper;
    private final LiveKitService liveKitService;
    private final ReportService reportService;

    







    @Transactional
    public SessionResponse createSession(UUID userId, CreateSessionRequest request) {
        log.info("Creating session for user: {} with interviewer: {}", userId, request.interviewerId());

        if (userId.equals(request.interviewerId())) {
            throw new BadRequestException("Candidate and interviewer must be different users");
        }

        
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        if (!userRepository.existsById(request.interviewerId())) {
            throw new ResourceNotFoundException("Interviewer not found: " + request.interviewerId());
        }

        
        Profile creatorProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new BadRequestException("Profile not found for user: " + userId));
        if (creatorProfile.getRole() != Profile.ProfileRole.CANDIDATE) {
            throw new BadRequestException("Only users with CANDIDATE role can create interview sessions");
        }

        Profile interviewerProfile = profileRepository.findByUserId(request.interviewerId())
                .orElseThrow(() -> new BadRequestException("Profile not found for interviewer: " + request.interviewerId()));
        if (interviewerProfile.getRole() != Profile.ProfileRole.INTERVIEWER) {
            throw new BadRequestException("Selected user must have INTERVIEWER role");
        }

        
        Optional<Session> activeSession = sessionRepository
                .findFirstByCreatedByAndStatusInOrderByCreatedAtDesc(
                        userId,
                        List.of(SessionStatus.SCHEDULED, SessionStatus.ACTIVE)
                );

        if (activeSession.isPresent()) {
            throw new BadRequestException(
                    "User already has an active session. Please end the current session before creating a new one."
            );
        }

        
        Session session = Session.builder()
                .createdBy(userId)
                .status(SessionStatus.SCHEDULED)
                .startsAt(request.scheduledAt())
                .roomProvider("livekit")
                .build();

        session = sessionRepository.save(session);

        
        String roomId = liveKitService.createRoom(session.getId());
        session.setRoomId(roomId);
        session = sessionRepository.save(session);

        
        SessionParticipant candidateParticipant = SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(userId)
                .roleInSession(ParticipantRole.CANDIDATE)
                .build();

        participantRepository.save(candidateParticipant);

        
        SessionParticipant interviewerParticipant = SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(request.interviewerId())
                .roleInSession(ParticipantRole.INTERVIEWER)
                .build();

        participantRepository.save(interviewerParticipant);

        log.info("Session created successfully: {}", session.getId());

        
        session = sessionRepository.findById(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found after creation"));

        return sessionMapper.toResponse(session);
    }

    







    @Transactional
    public SessionResponse joinSession(UUID sessionId, UUID userId) {
        log.info("User {} joining session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        
        if (session.getStatus() == SessionStatus.ENDED || session.getStatus() == SessionStatus.CANCELED) {
            throw new BadRequestException("Cannot join a session that has ended or been canceled");
        }

        
        
        Optional<SessionParticipant> existingParticipant = participantRepository
                .findBySessionIdAndUserId(sessionId, userId);

        if (existingParticipant.isEmpty()) {
            
            
            log.warn("User {} joining session {} but not in participant list", userId, sessionId);
            throw new BadRequestException("User is not authorized to join this session");
        }

        if (session.getStatus() == SessionStatus.SCHEDULED) {
            session.setStatus(SessionStatus.ACTIVE);
            session = sessionRepository.save(session);
        }

        log.info("User {} authorized to join session {}", userId, sessionId);

        
        session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        return sessionMapper.toResponse(session);
    }

    






    @Transactional
    public void leaveSession(UUID sessionId, UUID userId) {
        log.info("User {} leaving session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        SessionParticipant participant = participantRepository
                .findBySessionIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found in session"));

        if (participant.getLeftAt() == null) {
            participant.setLeftAt(OffsetDateTime.now());
            participantRepository.save(participant);
            log.info("User {} left session {}", userId, sessionId);

            long activeParticipants = participantRepository
                    .countBySessionIdAndJoinedAtIsNotNullAndLeftAtIsNull(sessionId);
            if (activeParticipants == 0) {
                maybeStopSessionRecording(session, "last participant left");
            }
        } else {
            log.info("User {} already left session {}", userId, sessionId);
        }
    }

    






    @Transactional
    public void endSession(UUID sessionId, UUID userId) {
        log.info("User {} ending session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        
        boolean isCreator = session.getCreatedBy().equals(userId);
        boolean isParticipant = participantRepository.existsBySessionIdAndUserId(sessionId, userId);

        if (!isCreator && !isParticipant) {
            throw new BadRequestException("Only session creator or participants can end the session");
        }

        if (session.getStatus() == SessionStatus.ENDED) {
            log.info("Session {} already ended", sessionId);
            return;
        }

        session.setStatus(SessionStatus.ENDED);
        session.setEndsAt(OffsetDateTime.now());
        sessionRepository.save(session);

        maybeStopSessionRecording(session, "session ended");

        if (session.getRoomId() != null && !session.getRoomId().isBlank()) {
            liveKitService.deleteRoom(session.getRoomId());
        }

        
        List<SessionParticipant> participants = participantRepository.findBySessionId(sessionId);
        for (SessionParticipant participant : participants) {
            if (participant.getLeftAt() == null) {
                participant.setLeftAt(OffsetDateTime.now());
                participantRepository.save(participant);
            }
        }

        try {
            reportService.triggerReportGeneration(sessionId, userId);
            log.info("Auto-triggered report generation on session end: {}", sessionId);
        } catch (BadRequestException | ResourceNotFoundException e) {
            log.info("Skipping auto report trigger for session {}: {}", sessionId, e.getMessage());
        } catch (Exception e) {
            log.error("Failed to auto-trigger report generation for session {}", sessionId, e);
        }

        log.info("Session {} ended successfully", sessionId);
    }

    private void maybeStartSessionRecording(Session session) {
        if (!liveKitService.isEgressAutoRecordEnabled()) {
            return;
        }
        if (session.getRecordingId() != null && !session.getRecordingId().isBlank()) {
            return;
        }

        long activeParticipants = participantRepository
                .countBySessionIdAndJoinedAtIsNotNullAndLeftAtIsNull(session.getId());
        if (activeParticipants < 2) {
            return;
        }

        try {
            String egressId = liveKitService.startRoomAudioRecording(session.getId());
            session.setRecordingId(egressId);
            sessionRepository.save(session);
            log.info("Auto-started recording for session {}: {}", session.getId(), egressId);
        } catch (Exception e) {
            log.error("Failed to auto-start recording for session {}", session.getId(), e);
        }
    }

    private void maybeStopSessionRecording(Session session, String reason) {
        if (!liveKitService.isEgressAutoRecordEnabled()) {
            return;
        }
        if (session.getRecordingId() == null || session.getRecordingId().isBlank()) {
            return;
        }

        try {
            liveKitService.stopEgress(session.getRecordingId());
            log.info("Requested recording stop for session {} (reason: {}), egress={}",
                    session.getId(), reason, session.getRecordingId());
        } catch (Exception e) {
            log.error("Failed to stop recording for session {} (reason: {})", session.getId(), reason, e);
        }
    }

    







    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        
        boolean isCreator = session.getCreatedBy().equals(userId);
        boolean isParticipant = participantRepository.existsBySessionIdAndUserId(sessionId, userId);

        if (!isCreator && !isParticipant) {
            throw new BadRequestException("You don't have access to this session");
        }

        return sessionMapper.toResponse(session);
    }

    









    @Transactional(readOnly = true)
    public SessionListResponse listSessions(UUID userId, int page, int size, SessionStatus status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Session> sessionsPage = status == null
                ? sessionRepository.findVisibleToUser(userId, pageable)
                : sessionRepository.findVisibleToUserAndStatus(userId, status, pageable);

        List<SessionResponse> sessionResponses = sessionMapper.toResponseList(sessionsPage.getContent());

        return new SessionListResponse(
                sessionResponses,
                sessionsPage.getTotalElements(),
                sessionsPage.getNumber(),
                sessionsPage.getSize()
        );
    }

    





    @Transactional(readOnly = true)
    public Optional<SessionResponse> getActiveSession(UUID userId) {
        Pageable firstActiveSession = PageRequest.of(
                0,
                1,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Optional<Session> activeSession = sessionRepository
                .findVisibleToUserAndStatusIn(
                        userId,
                        List.of(SessionStatus.SCHEDULED, SessionStatus.ACTIVE),
                        firstActiveSession
                )
                .getContent()
                .stream()
                .findFirst();

        return activeSession.map(sessionMapper::toResponse);
    }

    






    @Transactional(readOnly = true)
    public String getUserDisplayName(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
                        return profile.getDisplayName();
                    }
                    
                    return userRepository.findById(userId)
                            .map(user -> user.getEmail())
                            .orElse("User");
                })
                .orElseGet(() -> {
                    
                    return userRepository.findById(userId)
                            .map(user -> user.getEmail())
                            .orElse("User");
                });
    }
}
