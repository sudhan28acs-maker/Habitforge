package com.example.habitforge.dto;

import com.example.habitforge.entity.Frequency;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public record HabitResponse(Long id, String name, String description, Frequency frequency,
                            Set<DayOfWeek> targetDays, LocalDate startDate,
                            int currentStreak, int bestStreak,
                            boolean requiredToday, boolean completedToday) {}
