package com.example.habitforge.repository;

import com.example.habitforge.entity.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    Optional<Habit> findByNameIgnoreCase(String name);
}
