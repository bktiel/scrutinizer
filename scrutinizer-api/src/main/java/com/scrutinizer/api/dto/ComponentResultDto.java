package com.scrutinizer.api.dto;

import java.util.UUID;

public record ComponentResultDto(
    UUID id,
    String componentRef,
    String componentName,
    String componentVersion,
    String purl,
    boolean isDirect,
    String decision
) {}
