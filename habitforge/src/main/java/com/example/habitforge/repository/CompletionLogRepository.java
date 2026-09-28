package com.example.habitforge.repository;

import com.example.habitforge.entity.CompletionLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface CompletionLogRepository extends JpaRepository<CompletionLog, Long> {
    List<CompletionLog> findByHabitIdOrderByLogDateAsc(Long habitId);
    List<CompletionLog> findByHabitIdAndLogDateBetween(Long habitId, LocalDate from, LocalDate to);
    Optional<CompletionLog> findByHabitIdAndLogDate(Long habitId, LocalDate date);
    boolean existsByHabitIdAndLogDate(Long habitId, LocalDate date);
}
