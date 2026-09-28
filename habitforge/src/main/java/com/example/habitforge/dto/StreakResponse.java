package com.example.habitforge.dto;

import java.time.LocalDate;

public record StreakResponse(Long habitId, String habitName, int currentStreak, int bestStreak,
                             LocalDate lastCompletedDate) {}
