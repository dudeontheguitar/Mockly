package com.mockly.core.dto.user;

import java.util.List;

public record UserListResponse(
        List<UserResponse> items
) {}
