package com.example.habitforge.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record LogResponse(Long id, Long habitId, LocalDate date, String note, LocalDateTime loggedAt) {}
