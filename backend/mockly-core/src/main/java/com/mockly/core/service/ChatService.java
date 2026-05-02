package com.mockly.core.service;

import com.mockly.core.dto.chat.*;
import com.mockly.core.exception.BadRequestException;
import com.mockly.core.exception.ForbiddenException;
import com.mockly.core.exception.ResourceNotFoundException;
import com.mockly.data.entity.*;
import com.mockly.data.enums.ConversationType;
import com.mockly.data.enums.MessageStatus;
import com.mockly.data.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final ConversationParticipantRepository participantRepository;
    private final ConversationReadStateRepository readStateRepository;
    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ConversationListResponse listConversations(UUID userId) {
        Page<Conversation> conversations = conversationRepository.findByParticipant(
                userId,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
        return new ConversationListResponse(
                conversations.getContent().stream()
                        .map(conversation -> toConversationResponse(conversation, userId, true))
                        .toList()
        );
    }

    @Transactional
    public ConversationResponse createOrGetDirectConversation(UUID currentUserId, UUID otherUserId) {
        if (currentUserId.equals(otherUserId)) {
            throw new BadRequestException("Cannot create a direct conversation with yourself");
        }
        userRepository.findById(currentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUserId));
        userRepository.findById(otherUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + otherUserId));

        Conversation conversation = conversationRepository.findDirectConversation(currentUserId, otherUserId)
                .orElseGet(() -> createDirectConversation(currentUserId, otherUserId));

        return toConversationResponse(conversation, currentUserId, false);
    }

    @Transactional(readOnly = true)
    public ConversationResponse getConversation(UUID currentUserId, UUID conversationId) {
        requireParticipant(conversationId, currentUserId);
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        return toConversationResponse(conversation, currentUserId, false);
    }

    @Transactional(readOnly = true)
    public MessageListResponse listMessages(UUID currentUserId, UUID conversationId, int page, int size) {
        requireParticipant(conversationId, currentUserId);
        Page<Message> messages = messageRepository.findByConversationIdAndDeletedFalse(
                conversationId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"))
        );
        return new MessageListResponse(
                messages.getContent().stream().map(this::toMessageResponse).toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.hasNext()
        );
    }

    @Transactional
    public MessageResponse sendMessage(UUID currentUserId, UUID conversationId, SendMessageRequest request) {
        requireParticipant(conversationId, currentUserId);

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(currentUserId)
                .text(request.text().trim())
                .type(request.type())
                .status(MessageStatus.SENT)
                .deleted(false)
                .build();
        message = messageRepository.save(message);

        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found: " + conversationId));
        conversation.setUpdatedAt(OffsetDateTime.now());
        conversationRepository.save(conversation);

        MessageResponse response = toMessageResponse(message);
        notifyOtherParticipants(currentUserId, conversationId, response);
        return response;
    }

    @Transactional
    public ReadConversationResponse markRead(UUID currentUserId, UUID conversationId, UUID lastReadMessageId) {
        requireParticipant(conversationId, currentUserId);
        Message message = messageRepository.findById(lastReadMessageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + lastReadMessageId));
        if (!message.getConversationId().equals(conversationId)) {
            throw new BadRequestException("Message does not belong to this conversation");
        }

        ConversationReadState state = readStateRepository.findByConversationIdAndUserId(conversationId, currentUserId)
                .orElseGet(() -> ConversationReadState.builder()
                        .conversationId(conversationId)
                        .userId(currentUserId)
                        .build());
        state.setLastReadMessageId(message.getId());
        state.setLastReadAt(message.getCreatedAt());
        readStateRepository.save(state);

        messageRepository.findByConversationIdAndSenderIdNotAndDeletedFalse(conversationId, currentUserId)
                .stream()
                .filter(m -> !m.getCreatedAt().isAfter(message.getCreatedAt()))
                .forEach(m -> {
                    if (m.getStatus() != MessageStatus.READ) {
                        m.setStatus(MessageStatus.READ);
                        messageRepository.save(m);
                    }
                });

        return new ReadConversationResponse(conversationId, 0);
    }

    @Transactional
    public DeleteMessageResponse deleteOwnMessage(UUID currentUserId, UUID messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found: " + messageId));
        if (!message.getSenderId().equals(currentUserId)) {
            throw new ForbiddenException("Only the sender can delete this message");
        }
        message.setDeleted(true);
        messageRepository.save(message);
        return new DeleteMessageResponse(message.getId(), true);
    }

    private Conversation createDirectConversation(UUID currentUserId, UUID otherUserId) {
        Conversation conversation = conversationRepository.save(Conversation.builder()
                .type(ConversationType.DIRECT)
                .build());

        participantRepository.save(ConversationParticipant.builder()
                .conversationId(conversation.getId())
                .userId(currentUserId)
                .build());
        participantRepository.save(ConversationParticipant.builder()
                .conversationId(conversation.getId())
                .userId(otherUserId)
                .build());

        return conversationRepository.findById(conversation.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found after creation"));
    }

    private void requireParticipant(UUID conversationId, UUID userId) {
        if (!participantRepository.existsByConversationIdAndUserId(conversationId, userId)) {
            throw new ForbiddenException("You do not have access to this conversation");
        }
    }

    private ConversationResponse toConversationResponse(Conversation conversation, UUID viewerId, boolean includeLastMessage) {
        List<ConversationParticipant> participants = participantRepository.findByConversationId(conversation.getId());
        List<ConversationParticipantResponse> participantResponses = participants.stream()
                .map(this::toParticipantResponse)
                .toList();
        ConversationLastMessageResponse lastMessage = includeLastMessage
                ? messageRepository.findFirstByConversationIdAndDeletedFalseOrderByCreatedAtDesc(conversation.getId())
                .map(this::toLastMessageResponse)
                .orElse(null)
                : null;

        return new ConversationResponse(
                conversation.getId(),
                conversation.getType(),
                titleFor(conversation, viewerId, participants),
                lastMessage,
                unreadCount(conversation.getId(), viewerId),
                conversation.getUpdatedAt(),
                participantResponses,
                conversation.getCreatedAt()
        );
    }

    private ConversationParticipantResponse toParticipantResponse(ConversationParticipant participant) {
        User user = participant.getUser();
        Profile profile = user != null ? user.getProfile() : null;
        return new ConversationParticipantResponse(
                participant.getUserId(),
                displayName(profile, user),
                user != null ? user.getEmail() : null,
                profile != null ? profile.getAvatarUrl() : null,
                profile != null ? profile.getRole() : null
        );
    }

    private MessageResponse toMessageResponse(Message message) {
        User sender = message.getSender() != null
                ? message.getSender()
                : userRepository.findById(message.getSenderId()).orElse(null);
        Profile profile = sender != null ? sender.getProfile() : null;
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                displayName(profile, sender),
                profile != null ? profile.getAvatarUrl() : null,
                message.getText(),
                message.getType(),
                message.getStatus(),
                message.getCreatedAt(),
                message.getUpdatedAt()
        );
    }

    private ConversationLastMessageResponse toLastMessageResponse(Message message) {
        User sender = message.getSender() != null
                ? message.getSender()
                : userRepository.findById(message.getSenderId()).orElse(null);
        Profile profile = sender != null ? sender.getProfile() : null;
        return new ConversationLastMessageResponse(
                message.getId(),
                message.getText(),
                message.getSenderId(),
                displayName(profile, sender),
                message.getCreatedAt()
        );
    }

    private String titleFor(Conversation conversation, UUID viewerId, List<ConversationParticipant> participants) {
        if (conversation.getType() == ConversationType.DIRECT) {
            return participants.stream()
                    .filter(participant -> !participant.getUserId().equals(viewerId))
                    .findFirst()
                    .map(participant -> displayName(
                            participant.getUser() != null ? participant.getUser().getProfile() : null,
                            participant.getUser()))
                    .orElse("Conversation");
        }
        return "Conversation";
    }

    private long unreadCount(UUID conversationId, UUID userId) {
        OffsetDateTime lastReadAt = readStateRepository.findByConversationIdAndUserId(conversationId, userId)
                .map(ConversationReadState::getLastReadAt)
                .orElse(null);
        if (lastReadAt == null) {
            return messageRepository.countByConversationIdAndSenderIdNotAndDeletedFalse(conversationId, userId);
        }
        return messageRepository.countUnread(conversationId, userId, lastReadAt);
    }

    private void notifyOtherParticipants(UUID senderId, UUID conversationId, MessageResponse message) {
        String senderName = message.senderDisplayName() == null ? "User" : message.senderDisplayName();
        participantRepository.findByConversationId(conversationId).stream()
                .filter(participant -> !participant.getUserId().equals(senderId))
                .forEach(participant -> notificationService.createNotification(
                        participant.getUserId(),
                        "MESSAGE_RECEIVED",
                        "New message",
                        senderName + ": " + message.text(),
                        Map.of("conversationId", conversationId.toString(), "messageId", message.id().toString())
                ));
    }

    private String displayName(Profile profile, User user) {
        if (profile != null && profile.getDisplayName() != null && !profile.getDisplayName().isBlank()) {
            return profile.getDisplayName();
        }
        return user != null ? user.getEmail() : "User";
    }
}
