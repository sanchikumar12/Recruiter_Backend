package com.pi.application.dto;

public record ApplicationStatsResponse(
    long total,
    long applied,
    long underReview,
    long shortlisted,
    long interviewScheduled,
    long hired,
    long rejected,
    long withdrawn
) {}
