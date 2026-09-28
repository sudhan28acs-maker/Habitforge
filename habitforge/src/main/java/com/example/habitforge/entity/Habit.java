package com.example.habitforge.entity;

import jakarta.persistence.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** A habit the user wants to build. Owns its completion logs and its streak. */
@Entity
@Table(name = "habit")
public class Habit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Frequency frequency;

    /** Only used when frequency = SPECIFIC_WEEKDAYS. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "habit_target_days", joinColumns = @JoinColumn(name = "habit_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week")
    private Set<DayOfWeek> targetDays = new HashSet<>();

    @Column(nullable = false)
    private LocalDate startDate;

    @OneToMany(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CompletionLog> logs = new ArrayList<>();

    @OneToOne(mappedBy = "habit", cascade = CascadeType.ALL, orphanRemoval = true)
    private Streak streak;

    public boolean isRequiredOn(LocalDate date) {
        return frequency == Frequency.DAILY || targetDays.contains(date.getDayOfWeek());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Frequency getFrequency() { return frequency; }
    public void setFrequency(Frequency frequency) { this.frequency = frequency; }
    public Set<DayOfWeek> getTargetDays() { return targetDays; }
    public void setTargetDays(Set<DayOfWeek> targetDays) { this.targetDays = targetDays; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public List<CompletionLog> getLogs() { return logs; }
    public Streak getStreak() { return streak; }
    public void setStreak(Streak streak) { this.streak = streak; }
}
