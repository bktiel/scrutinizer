package com.scrutinizer.api.dto;

public record CreateRunRequest(
        String applicationName,
        String sbomPath,
        String policyPath
) {}
