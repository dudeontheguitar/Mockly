package com.mockly.core.dto.session;

import java.util.List;




public record SessionListResponse(
        List<SessionResponse> sessions,
        long total,
        int page,
        int size
) {}

