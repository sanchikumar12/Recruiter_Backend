package com.pi.interview.dto;

import com.pi.interview.enums.SlotStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record SlotResponse(
        UUID slotId,
        UUID interviewerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        SlotStatus status
) {
}
