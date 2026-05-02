package com.mockly.core.service;

import com.mockly.core.dto.interview.*;
import com.mockly.core.dto.user.UserSummaryResponse;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ForbiddenException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.InterviewSlot;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.entity.User;
import com.mockly.data.enums.InterviewSlotStatus;
import com.mockly.data.enums.ParticipantRole;
import com.mockly.data.enums.SessionStatus;
import com.mockly.data.repository.InterviewSlotRepository;
import com.mockly.data.repository.ProfileRepository;
import com.mockly.data.repository.SessionParticipantRepository;
import com.mockly.data.repository.SessionRepository;
import com.mockly.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewSlotService {

    private final InterviewSlotRepository slotRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final SessionRepository sessionRepository;
    private final SessionParticipantRepository participantRepository;
    private final LiveKitService liveKitService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public InterviewSlotListResponse listOpenSlots(UUID userId) {
        requireRole(userId, Profile.ProfileRole.CANDIDATE);
        return new InterviewSlotListResponse(slotRepository.findByStatusOrderByScheduledAtAsc(InterviewSlotStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional
    public InterviewSlotResponse createSlot(UUID userId, CreateInterviewSlotRequest request) {
        requireRole(userId, Profile.ProfileRole.INTERVIEWER);

        InterviewSlot slot = InterviewSlot.builder()
                .title(request.title().trim())
                .company(request.company().trim())
                .location(request.location())
                .description(request.description())
                .scheduledAt(request.scheduledAt())
                .durationMinutes(request.durationMinutes())
                .status(InterviewSlotStatus.OPEN)
                .interviewerId(userId)
                .build();

        return toResponse(slotRepository.save(slot));
    }

    @Transactional(readOnly = true)
    public InterviewSlotListResponse getMySlots(UUID userId) {
        requireRole(userId, Profile.ProfileRole.INTERVIEWER);
        return new InterviewSlotListResponse(slotRepository.findByInterviewerIdOrderByScheduledAtDesc(userId)
                .stream()
                .map(this::toResponse)
                .toList());
    }

    @Transactional(readOnly = true)
    public InterviewSlotResponse getSlot(UUID userId, UUID slotId) {
        ensureCandidateOrInterviewer(userId);
        InterviewSlot slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview slot not found: " + slotId));
        return toResponse(slot);
    }

    @Transactional
    public BookInterviewSlotResponse bookSlot(UUID userId, UUID slotId) {
        requireRole(userId, Profile.ProfileRole.CANDIDATE);

        InterviewSlot slot = slotRepository.findWithLockById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview slot not found: " + slotId));

        if (slot.getStatus() != InterviewSlotStatus.OPEN) {
            throw new BadRequestException("Interview slot is not open");
        }
        if (slot.getInterviewerId().equals(userId)) {
            throw new BadRequestException("Candidate and interviewer must be different users");
        }

        userRepository.findById(slot.getInterviewerId())
                .orElseThrow(() -> new ResourceNotFoundException("Interviewer not found: " + slot.getInterviewerId()));

        Session session = Session.builder()
                .createdBy(userId)
                .status(SessionStatus.SCHEDULED)
                .startsAt(slot.getScheduledAt())
                .roomProvider("livekit")
                .build();
        session = sessionRepository.save(session);
        session.setRoomId(liveKitService.createRoom(session.getId()));
        session = sessionRepository.save(session);

        participantRepository.save(SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(userId)
                .roleInSession(ParticipantRole.CANDIDATE)
                .build());
        participantRepository.save(SessionParticipant.builder()
                .sessionId(session.getId())
                .userId(slot.getInterviewerId())
                .roleInSession(ParticipantRole.INTERVIEWER)
                .build());

        slot.setBookedBy(userId);
        slot.setSessionId(session.getId());
        slot.setStatus(InterviewSlotStatus.BOOKED);
        slotRepository.save(slot);

        String candidateName = profileRepository.findByUserId(userId)
                .map(Profile::getDisplayName)
                .filter(name -> !name.isBlank())
                .orElse("Candidate");
        notificationService.createNotification(
                slot.getInterviewerId(),
                "INTERVIEW_BOOKED",
                "New interview booked",
                candidateName + " booked your " + slot.getTitle() + " interview.",
                Map.of("sessionId", session.getId().toString(), "slotId", slot.getId().toString())
        );

        return new BookInterviewSlotResponse(slot.getId(), session.getId(), slot.getStatus());
    }

    @Transactional
    public CancelInterviewSlotResponse cancelSlot(UUID userId, UUID slotId) {
        ensureCandidateOrInterviewer(userId);
        InterviewSlot slot = slotRepository.findWithLockById(slotId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview slot not found: " + slotId));

        boolean interviewerOwnsSlot = slot.getInterviewerId().equals(userId);
        boolean candidateBookedSlot = slot.getBookedBy() != null && slot.getBookedBy().equals(userId);
        if (!interviewerOwnsSlot && !candidateBookedSlot) {
            throw new ForbiddenException("Only the interviewer or booked candidate can cancel this slot");
        }

        slot.setStatus(InterviewSlotStatus.CANCELED);
        if (slot.getSessionId() != null) {
            sessionRepository.findById(slot.getSessionId()).ifPresent(session -> {
                if (session.getStatus() != SessionStatus.ENDED && session.getStatus() != SessionStatus.CANCELED) {
                    session.setStatus(SessionStatus.CANCELED);
                    session.setEndsAt(OffsetDateTime.now());
                    sessionRepository.save(session);
                }
            });
        }
        slotRepository.save(slot);

        UUID notifyUserId = interviewerOwnsSlot ? slot.getBookedBy() : slot.getInterviewerId();
        if (notifyUserId != null) {
            notificationService.createNotification(
                    notifyUserId,
                    "INTERVIEW_CANCELED",
                    "Interview canceled",
                    slot.getTitle() + " interview was canceled.",
                    Map.of("slotId", slot.getId().toString())
            );
        }

        return new CancelInterviewSlotResponse(slot.getId(), slot.getStatus());
    }

    private void ensureCandidateOrInterviewer(UUID userId) {
        Profile.ProfileRole role = profileRepository.findByUserId(userId)
                .map(Profile::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        if (role != Profile.ProfileRole.CANDIDATE && role != Profile.ProfileRole.INTERVIEWER) {
            throw new ForbiddenException("Unsupported role");
        }
    }

    private void requireRole(UUID userId, Profile.ProfileRole expectedRole) {
        Profile.ProfileRole actualRole = profileRepository.findByUserId(userId)
                .map(Profile::getRole)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found for user: " + userId));
        if (actualRole != expectedRole) {
            throw new ForbiddenException("Required role: " + expectedRole);
        }
    }

    private InterviewSlotResponse toResponse(InterviewSlot slot) {
        return new InterviewSlotResponse(
                slot.getId(),
                slot.getTitle(),
                slot.getCompany(),
                slot.getLocation(),
                slot.getDescription(),
                slot.getScheduledAt(),
                slot.getDurationMinutes(),
                slot.getStatus(),
                userSummary(slot.getInterviewerId()),
                slot.getBookedBy() == null ? null : userSummary(slot.getBookedBy()),
                slot.getSessionId()
        );
    }

    private UserSummaryResponse userSummary(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
        Profile profile = profileRepository.findByUserId(userId)
                .orElse(null);
        return new UserSummaryResponse(
                user.getId(),
                displayName(profile, user),
                user.getEmail(),
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getRole() : null,
                profile != null ? profile.getLevel() : null
        );
    }

    private String displayName(Profile profile, User user) {
        if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        return user.getEmail();
    }
}
