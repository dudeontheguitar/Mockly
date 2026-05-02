package com.mockly.api.websocket;

import com.mockly.core.dto.report.ReportResponse;
import com.mockly.core.dto.session.SessionParticipantResponse;
import com.mockly.core.dto.session.SessionResponse;
import com.mockly.data.entity.Session;
import com.mockly.data.entity.SessionParticipant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;





@Component
@RequiredArgsConstructor
@Slf4j
public class SessionEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    



    public void publishSessionCreated(Session session, SessionResponse sessionResponse) {
        publishSessionCreated(session.getId(), session.getCreatedBy(), sessionResponse);
    }

    public void publishSessionCreated(UUID sessionId, UUID userId, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + sessionId;

        SessionEvent event = new SessionEvent("SESSION_CREATED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        if (sessionResponse != null
                && sessionResponse.participants() != null
                && !sessionResponse.participants().isEmpty()) {
            notifyParticipants(event, sessionResponse);
        } else {
            String userTopic = "/topic/users/" + userId + "/sessions";
            messagingTemplate.convertAndSend(userTopic, event);
        }

        log.info("Published SESSION_CREATED event for session: {}", sessionId);
    }

    



    public void publishSessionUpdated(Session session, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId();

        SessionEvent event = new SessionEvent("SESSION_UPDATED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        notifyParticipants(event, sessionResponse);

        log.info("Published SESSION_UPDATED event for session: {}", session.getId());
    }

    



    public void publishParticipantJoined(Session session, SessionParticipant participant, SessionResponse sessionResponse) {
        publishParticipantJoined(session.getId(), participant.getUserId(), sessionResponse);
    }

    public void publishParticipantJoined(UUID sessionId, UUID userId, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + sessionId;
        String userTopic = "/topic/users/" + userId + "/sessions";

        SessionEvent event = new SessionEvent("PARTICIPANT_JOINED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        messagingTemplate.convertAndSend(userTopic, event);

        log.info("Published PARTICIPANT_JOINED event for session: {}, participant: {}",
                sessionId, userId);
    }

    



    public void publishParticipantLeft(Session session, UUID userId, SessionResponse sessionResponse) {
        publishParticipantLeft(session.getId(), userId, sessionResponse);
    }

    public void publishParticipantLeft(UUID sessionId, UUID userId, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + sessionId;
        String userTopic = "/topic/users/" + userId + "/sessions";

        SessionEvent event = new SessionEvent("PARTICIPANT_LEFT", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        messagingTemplate.convertAndSend(userTopic, event);

        log.info("Published PARTICIPANT_LEFT event for session: {}, participant: {}",
                sessionId, userId);
    }

    



    public void publishSessionEnded(Session session, SessionResponse sessionResponse) {
        publishSessionEnded(session.getId(), sessionResponse);
    }

    public void publishSessionEnded(UUID sessionId, SessionResponse sessionResponse) {
        String sessionTopic = "/topic/sessions/" + sessionId;

        SessionEvent event = new SessionEvent("SESSION_ENDED", sessionResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        notifyParticipants(event, sessionResponse);

        log.info("Published SESSION_ENDED event for session: {}", sessionId);
    }

    



    public void publishReportReady(Session session, ReportResponse reportResponse) {
        String sessionTopic = "/topic/sessions/" + session.getId() + "/report";
        String sessionEventTopic = "/topic/sessions/" + session.getId();

        ReportEvent event = new ReportEvent("REPORT_READY", reportResponse);

        messagingTemplate.convertAndSend(sessionTopic, event);
        messagingTemplate.convertAndSend(sessionEventTopic, event);

        
        if (session.getParticipants() != null) {
            for (SessionParticipant participant : session.getParticipants()) {
                String userTopic = "/topic/users/" + participant.getUserId() + "/sessions";
                messagingTemplate.convertAndSend(userTopic, event);
            }
        }

        log.info("Published REPORT_READY event for session: {}", session.getId());
    }

    private void notifyParticipants(SessionEvent event, SessionResponse sessionResponse) {
        if (sessionResponse == null || sessionResponse.participants() == null) {
            return;
        }

        for (SessionParticipantResponse participant : sessionResponse.participants()) {
            if (participant == null || participant.userId() == null) {
                continue;
            }
            String userTopic = "/topic/users/" + participant.userId() + "/sessions";
            messagingTemplate.convertAndSend(userTopic, event);
        }
    }

    


    public record SessionEvent(
            String type,
            SessionResponse data
    ) {}

    


    public record ReportEvent(
            String type,
            ReportResponse data
    ) {}
}
