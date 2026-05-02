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

/**
 * Service for managing interview sessions.
 * Handles session lifecycle, participant management, and validation.
 */
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

    /**
     * Create a new interview session.
     * Validates that the user doesn't have an active session.
     *
     * @param userId ID of the user creating the session (candidate)
     * @param request Create session request
     * @return Created session response
     */
    @Transactional
    public SessionResponse createSession(UUID userId, CreateSessionRequest request) {
        log.info("Creating session for user: {} with interviewer: {}", userId, request.interviewerId());

        if (userId.equals(request.interviewerId())) {
            throw new BadRequestException("Candidate and interviewer must be different users");
        }

        // Validate users exist
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User not found: " + userId);
        }

        if (!userRepository.existsById(request.interviewerId())) {
            throw new ResourceNotFoundException("Interviewer not found: " + request.interviewerId());
        }

        // Validate role model: creator must be candidate, selected user must be interviewer
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

        // Validation: Check if user already has an active session
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

        // Create session
        Session session = Session.builder()
                .createdBy(userId)
                .status(SessionStatus.SCHEDULED)
                .startsAt(request.scheduledAt())
                .roomProvider("livekit")
                .build();

        session = sessionRepository.save(session);

        // Create LiveKit room (after session is saved to get ID)
        String roomId = liveKitService.createRoom(session.getId());
        session.setRoomId(roomId);
        session = sessionRepository.save(session);

        // Add creator as candidate participant
        SessionParticipant candidateParticipant = SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(userId)
                .roleInSession(ParticipantRole.CANDIDATE)
                .build();

        participantRepository.save(candidateParticipant);

        // Add interviewer as participant
        SessionParticipant interviewerParticipant = SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(request.interviewerId())
                .roleInSession(ParticipantRole.INTERVIEWER)
                .build();

        participantRepository.save(interviewerParticipant);

        log.info("Session created successfully: {}", session.getId());

        // Load session with participants for response
        session = sessionRepository.findById(session.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Session not found after creation"));

        return sessionMapper.toResponse(session);
    }

    /**
     * Join an existing session.
     * Authorizes the caller to connect to the LiveKit room.
     *
     * @param sessionId Session ID
     * @param userId User ID joining the session
     * @return Updated session response
     */
    @Transactional
    public SessionResponse joinSession(UUID sessionId, UUID userId) {
        log.info("User {} joining session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Validate session status
        if (session.getStatus() == SessionStatus.ENDED || session.getStatus() == SessionStatus.CANCELED) {
            throw new BadRequestException("Cannot join a session that has ended or been canceled");
        }

        // Check if user is already a participant. REST join only authorizes the
        // client to enter the interview; LiveKit webhooks mark real RTC presence.
        Optional<SessionParticipant> existingParticipant = participantRepository
                .findBySessionIdAndUserId(sessionId, userId);

        if (existingParticipant.isEmpty()) {
            // New participant - should not happen if session was created correctly
            // But handle it gracefully
            log.warn("User {} joining session {} but not in participant list", userId, sessionId);
            throw new BadRequestException("User is not authorized to join this session");
        }

        log.info("User {} authorized to join session {}", userId, sessionId);

        // Reload session with participants
        session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found"));

        return sessionMapper.toResponse(session);
    }

    /**
     * Leave a session.
     * Updates participant's left_at timestamp.
     *
     * @param sessionId Session ID
     * @param userId User ID leaving the session
     */
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

    /**
     * End a session.
     * Only the session creator or a participant can end the session.
     *
     * @param sessionId Session ID
     * @param userId User ID ending the session
     */
    @Transactional
    public void endSession(UUID sessionId, UUID userId) {
        log.info("User {} ending session {}", userId, sessionId);

        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Validate user can end session (creator or participant)
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

        // Mark all participants as left
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

    /**
     * Get session by ID.
     * User must be the creator or a participant.
     *
     * @param sessionId Session ID
     * @param userId User ID requesting the session
     * @return Session response
     */
    @Transactional(readOnly = true)
    public SessionResponse getSession(UUID sessionId, UUID userId) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        // Validate user has access (creator or participant)
        boolean isCreator = session.getCreatedBy().equals(userId);
        boolean isParticipant = participantRepository.existsBySessionIdAndUserId(sessionId, userId);

        if (!isCreator && !isParticipant) {
            throw new BadRequestException("You don't have access to this session");
        }

        return sessionMapper.toResponse(session);
    }

    /**
     * List sessions for a user.
     * Returns sessions where user is creator or participant.
     *
     * @param userId User ID
     * @param page Page number (0-indexed)
     * @param size Page size
     * @param status Optional status filter
     * @return Paginated session list
     */
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

    /**
     * Get active session for a user.
     *
     * @param userId User ID
     * @return Optional session response (empty if no active session)
     */
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

    /**
     * Get user display name from profile.
     * Falls back to email if display name is not set.
     *
     * @param userId User ID
     * @return Display name or email
     */
    @Transactional(readOnly = true)
    public String getUserDisplayName(UUID userId) {
        return profileRepository.findByUserId(userId)
                .map(profile -> {
                    if (profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
                        return profile.getDisplayName();
                    }
                    // Fallback to email if display name is not set
                    return userRepository.findById(userId)
                            .map(user -> user.getEmail())
                            .orElse("User");
                })
                .orElseGet(() -> {
                    // If profile doesn't exist, use email
                    return userRepository.findById(userId)
                            .map(user -> user.getEmail())
                            .orElse("User");
                });
    }
}
