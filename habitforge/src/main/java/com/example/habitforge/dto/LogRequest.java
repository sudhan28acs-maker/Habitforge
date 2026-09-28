package com.example.habitforge.dto;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/** Both fields optional: date defaults to today. */
public record LogRequest(
        LocalDate date,
        @Size(max = 255, message = "note must be at most 255 characters") String note) {}
