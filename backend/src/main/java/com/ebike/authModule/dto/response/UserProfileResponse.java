package com.ebike.authModule.dto.response;

import java.util.Set;

public record UserProfileResponse(
    Long userId,
    String username,
    String email,
    String firstName,
    String lastName,
    String phoneNumber,
    String avatarUrl,
    Boolean active,
    Boolean verified,
    Set<String> roles,
    Set<String> permissions
) {
}
