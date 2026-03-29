package com.scrutinizer.api.dto;

public record ReviewRequest(
        String reviewStatus,
        String reviewerNotes
) {}
