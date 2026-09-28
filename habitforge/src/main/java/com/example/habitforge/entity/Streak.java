package com.example.habitforge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;

/** Cached streak numbers for a habit (one row per habit). */
@Entity
@Table(name = "streak")
public class Streak {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "habit_id", unique = true)
    private Habit habit;

    private int currentStreak = 0;
    private int bestStreak = 0;
    private LocalDate lastCompletedDate;

    public Streak() {}
    public Streak(Habit habit) { this.habit = habit; }

    public Long getId() { return id; }
    public Habit getHabit() { return habit; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int currentStreak) { this.currentStreak = currentStreak; }
    public int getBestStreak() { return bestStreak; }
    public void setBestStreak(int bestStreak) { this.bestStreak = bestStreak; }
    public LocalDate getLastCompletedDate() { return lastCompletedDate; }
    public void setLastCompletedDate(LocalDate lastCompletedDate) { this.lastCompletedDate = lastCompletedDate; }
}
