package com.mockly.core.dto.chat;

import java.util.List;

public record MessageListResponse(
        List<MessageResponse> items,
        int page,
        int size,
        boolean hasNext
) {}
