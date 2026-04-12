package com.scrutinizer.api.dto;

import java.time.Instant;

public record UpdateExceptionRequest(
    String status,
    String approvedBy,
    Instant expiresAt
) {}
