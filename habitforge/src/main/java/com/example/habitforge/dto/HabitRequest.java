package com.example.habitforge.dto;

import com.example.habitforge.entity.Frequency;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record HabitRequest(
        @NotBlank(message = "name is required") @Size(max = 100, message = "name must be at most 100 characters") String name,
        @Size(max = 500, message = "description must be at most 500 characters") String description,
        @NotNull(message = "frequency is required (DAILY or SPECIFIC_WEEKDAYS)") Frequency frequency,
        Set<DayOfWeek> targetDays,
        LocalDate startDate) {}
