package com.pi.job.dto;

public record JobStatsResponse(
    long total,
    long draft,
    long published,
    long paused,
    long closed,
    long expired
) {}
