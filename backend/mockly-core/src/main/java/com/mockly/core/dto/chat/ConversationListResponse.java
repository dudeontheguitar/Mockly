package com.mockly.core.dto.chat;

import java.util.List;

public record ConversationListResponse(
        List<ConversationResponse> items
) {}
