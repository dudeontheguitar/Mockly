package com.mockly.core.mapper;

import com.mockly.core.dto.session.ArtifactResponse;
import com.mockly.core.dto.session.SessionInterviewResponse;
import com.mockly.core.dto.session.SessionParticipantResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.data.entity.Artifact;
import com.mockly.data.entity.InterviewSlot;
import com.mockly.data.entity.Profile;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import com.mockly.data.repository.InterviewSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class SessionMapper {

    private final InterviewSlotRepository interviewSlotRepository;

    public SessionResponse toResponse(Session session) {
        return new SessionResponse(
                session.getId(),
                session.getCreatedBy(),
                displayName(session.getCreator() != null ? session.getCreator().getProfile() : null),
                session.getStatus(),
                session.getStartsAt(),
                session.getEndsAt(),
                session.getRoomProvider(),
                session.getRoomId(),
                session.getRecordingId(),
                toInterviewResponse(session),
                session.getCreatedAt(),
                session.getUpdatedAt(),
                toParticipantResponseList(session.getParticipants()),
                toArtifactResponseList(session.getArtifacts())
        );
    }

    public SessionParticipantResponse toResponse(SessionParticipant participant) {
        Profile profile = participant.getUser() != null ? participant.getUser().getProfile() : null;
        return new SessionParticipantResponse(
                participant.getId(),
                participant.getUserId(),
                displayName(profile),
                participant.getUser() != null ? participant.getUser().getEmail() : null,
                profile != null ? profile.getAvatarUrl() : null,
                participant.getRoleInSession(),
                participant.getJoinedAt(),
                participant.getLeftAt()
        );
    }

    public ArtifactResponse toResponse(Artifact artifact) {
        return new ArtifactResponse(
                artifact.getId(),
                artifact.getSessionId(),
                artifact.getType(),
                artifact.getStorageUrl(),
                artifact.getDurationSec(),
                artifact.getSizeBytes(),
                artifact.getCreatedAt()
        );
    }

    public List<SessionResponse> toResponseList(List<Session> sessions) {
        return sessions.stream().map(this::toResponse).toList();
    }

    public List<SessionParticipantResponse> toParticipantResponseList(List<SessionParticipant> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream().map(this::toResponse).toList();
    }

    public List<ArtifactResponse> toArtifactResponseList(List<Artifact> artifacts) {
        if (artifacts == null) {
            return List.of();
        }
        return artifacts.stream().map(this::toResponse).toList();
    }

    private SessionInterviewResponse toInterviewResponse(Session session) {
        return interviewSlotRepository.findBySessionId(session.getId())
                .map(this::toInterviewResponse)
                .orElse(null);
    }

    private SessionInterviewResponse toInterviewResponse(InterviewSlot slot) {
        return new SessionInterviewResponse(
                slot.getId(),
                slot.getTitle(),
                slot.getCompany(),
                slot.getLocation(),
                slot.getDescription(),
                slot.getDurationMinutes()
        );
    }

    private String displayName(Profile profile) {
        if (profile == null || profile.getDisplayName() == null || profile.getDisplayName().isBlank()) {
            return null;
        }
        return profile.getDisplayName();
    }
}
