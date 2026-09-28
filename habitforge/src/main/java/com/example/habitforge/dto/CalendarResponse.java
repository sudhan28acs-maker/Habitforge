package com.example.habitforge.dto;

import java.time.LocalDate;
import java.util.List;

public record CalendarResponse(Long habitId, String habitName, int year, int month,
                               List<Day> days, int requiredDays, int completedDays, int completionRatePercent) {

    /** status: COMPLETED, MISSED, PENDING (today, not done yet), UPCOMING, NOT_REQUIRED, BEFORE_START */
    public record Day(LocalDate date, boolean required, boolean completed, String status) {}
}
