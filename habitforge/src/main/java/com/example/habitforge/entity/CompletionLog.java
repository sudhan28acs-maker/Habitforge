package com.example.habitforge.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** One "done" record for a habit on a given day. At most one per habit per day. */
@Entity
@Table(name = "completion_log",
       uniqueConstraints = @UniqueConstraint(columnNames = {"habit_id", "log_date"}))
public class CompletionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "habit_id")
    private Habit habit;

    @Column(name = "log_date", nullable = false)
    private LocalDate logDate;

    @Column(length = 255)
    private String note;

    private LocalDateTime loggedAt;

    public CompletionLog() {}

    public CompletionLog(Habit habit, LocalDate logDate, String note) {
        this.habit = habit;
        this.logDate = logDate;
        this.note = note;
        this.loggedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Habit getHabit() { return habit; }
    public LocalDate getLogDate() { return logDate; }
    public String getNote() { return note; }
    public LocalDateTime getLoggedAt() { return loggedAt; }
}
