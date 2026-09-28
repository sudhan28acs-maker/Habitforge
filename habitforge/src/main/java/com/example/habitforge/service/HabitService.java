package com.example.habitforge.service;

import com.example.habitforge.dto.*;
import com.example.habitforge.entity.*;
import com.example.habitforge.exception.BusinessRuleException;
import com.example.habitforge.exception.DuplicateException;
import com.example.habitforge.exception.NotFoundException;
import com.example.habitforge.repository.CompletionLogRepository;
import com.example.habitforge.repository.HabitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class HabitService {

    private static final Logger log = LoggerFactory.getLogger(HabitService.class);

    @Autowired private HabitRepository habitRepository;
    @Autowired private CompletionLogRepository logRepository;

    // ======================= Habit CRUD =======================

    public HabitResponse create(HabitRequest r) {
        assertNameFree(r.name(), null);
        Habit h = new Habit();
        apply(h, r);
        LocalDate start = r.startDate() == null ? LocalDate.now() : r.startDate();
        if (start.isAfter(LocalDate.now())) {
            throw new BusinessRuleException("startDate cannot be in the future");
        }
        h.setStartDate(start);
        h.setStreak(new Streak(h));
        habitRepository.save(h);
        log.info("Habit created: {} (id={})", h.getName(), h.getId());
        return toResponse(h);
    }

    public HabitResponse update(Long id, HabitRequest r) {
        Habit h = find(id);
        assertNameFree(r.name(), id);
        apply(h, r);
        if (r.startDate() != null) {
            if (r.startDate().isAfter(LocalDate.now())) {
                throw new BusinessRuleException("startDate cannot be in the future");
            }
            h.setStartDate(r.startDate());
        }
        recompute(h);
        return toResponse(h);
    }

    public void delete(Long id) {
        habitRepository.delete(find(id));
    }

    @Transactional(readOnly = true)
    public HabitResponse get(Long id) {
        return toResponse(find(id));
    }

    @Transactional(readOnly = true)
    public List<HabitResponse> list() {
        return habitRepository.findAll().stream().map(this::toResponse).toList();
    }

    /** Habits that are due today (used for the reminders banner). */
    @Transactional(readOnly = true)
    public List<HabitResponse> dueToday() {
        return list().stream().filter(HabitResponse::requiredToday).toList();
    }

    // ======================= Logging =======================

    public StreakResponse logCompletion(Long habitId, LocalDate date, String note) {
        Habit h = find(habitId);
        LocalDate today = LocalDate.now();
        LocalDate d = date == null ? today : date;

        // Business rules are checked BEFORE saving, so bad requests never touch the data.
        if (d.isAfter(today)) {
            throw new BusinessRuleException("Cannot log a habit for a future date (" + d + ")");
        }
        if (d.isBefore(h.getStartDate())) {
            throw new BusinessRuleException("Cannot log " + d + ": habit '" + h.getName()
                    + "' starts on " + h.getStartDate());
        }
        if (!h.isRequiredOn(d)) {
            throw new BusinessRuleException(d + " (" + d.getDayOfWeek() + ") is not a scheduled day for habit '"
                    + h.getName() + "'");
        }
        if (logRepository.existsByHabitIdAndLogDate(habitId, d)) {
            throw new DuplicateException("Habit '" + h.getName() + "' is already logged as done on " + d);
        }

        logRepository.save(new CompletionLog(h, d, note));
        int previousBest = h.getStreak() == null ? 0 : h.getStreak().getBestStreak();
        Streak s = recompute(h);
        log.info("[NOTIFY] \u2705 {} done on {} - current streak {} (best {})", h.getName(), d, s.getCurrentStreak(), s.getBestStreak());
        if (s.getBestStreak() > previousBest) {
            log.info("[NOTIFY] \uD83C\uDFC6 New best streak for {}: {}", h.getName(), s.getBestStreak());
        }
        return toStreakResponse(h, s);
    }

    public StreakResponse removeLog(Long habitId, LocalDate date) {
        Habit h = find(habitId);
        CompletionLog cl = logRepository.findByHabitIdAndLogDate(habitId, date)
                .orElseThrow(() -> new NotFoundException("No completion logged for '" + h.getName() + "' on " + date));
        h.getLogs().remove(cl);
        logRepository.delete(cl);
        logRepository.flush();
        return toStreakResponse(h, recompute(h));
    }

    @Transactional(readOnly = true)
    public List<LogResponse> logs(Long habitId) {
        find(habitId);
        return logRepository.findByHabitIdOrderByLogDateAsc(habitId).stream()
                .map(l -> new LogResponse(l.getId(), habitId, l.getLogDate(), l.getNote(), l.getLoggedAt())).toList();
    }

    // ======================= Streaks =======================

    /** Re-evaluates the streak first (so a missed day is detected), then returns it. */
    public StreakResponse getStreak(Long habitId) {
        Habit h = find(habitId);
        return toStreakResponse(h, recompute(h));
    }

    /** Detects missed days for every habit and resets streaks. Called by the daily scheduler. */
    public Map<String, Integer> refreshAllStreaks() {
        int checked = 0, reset = 0;
        for (Habit h : habitRepository.findAll()) {
            int before = h.getStreak() == null ? 0 : h.getStreak().getCurrentStreak();
            Streak s = recompute(h);
            checked++;
            if (before > 0 && s.getCurrentStreak() == 0) reset++;
        }
        Map<String, Integer> result = new LinkedHashMap<>();
        result.put("habitsChecked", checked);
        result.put("streaksReset", reset);
        return result;
    }

    /**
     * Core streak algorithm. Walks every REQUIRED day from the start date to today:
     *  - completed  -> run + 1
     *  - missed (a required day before today with no log) -> run resets to 0
     *  - today and not yet done -> still pending, does NOT break the streak
     *    (the streak resets the day AFTER a required day is missed)
     * Best streak is only raised when a streak exceeds the previously stored best.
     */
    private Streak recompute(Habit h) {
        Streak s = h.getStreak();
        if (s == null) { s = new Streak(h); h.setStreak(s); }

        LocalDate today = LocalDate.now();
        Set<LocalDate> done = logRepository.findByHabitIdOrderByLogDateAsc(h.getId()).stream()
                .map(CompletionLog::getLogDate).collect(Collectors.toSet());

        int run = 0, longest = 0;
        LocalDate lastDone = null;
        for (LocalDate d = h.getStartDate(); !d.isAfter(today); d = d.plusDays(1)) {
            if (!h.isRequiredOn(d)) continue;
            if (done.contains(d)) {
                run++;
                longest = Math.max(longest, run);
                lastDone = d;
            } else if (d.isBefore(today)) {
                run = 0;                       // required day missed -> reset
            }                                  // else: today still pending
        }

        int previousCurrent = s.getCurrentStreak();
        s.setCurrentStreak(run);
        s.setLastCompletedDate(lastDone);
        if (Math.max(run, longest) > s.getBestStreak()) {   // only ever increases
            s.setBestStreak(Math.max(run, longest));
        }
        if (previousCurrent > 0 && run == 0) {
            log.info("[NOTIFY] \uD83D\uDCA4 Streak of {} reset for habit {} (a required day was missed)", previousCurrent, h.getName());
        }
        return s;
    }

    // ======================= Calendar =======================

    @Transactional(readOnly = true)
    public CalendarResponse calendar(Long habitId, int year, int month) {
        Habit h = find(habitId);
        YearMonth ym;
        try {
            ym = YearMonth.of(year, month);
        } catch (DateTimeException e) {
            throw new BusinessRuleException("Invalid year/month: " + year + "/" + month + " (month must be 1-12)");
        }
        LocalDate today = LocalDate.now();
        Set<LocalDate> done = logRepository.findByHabitIdAndLogDateBetween(habitId, ym.atDay(1), ym.atEndOfMonth())
                .stream().map(CompletionLog::getLogDate).collect(Collectors.toSet());

        List<CalendarResponse.Day> days = new ArrayList<>();
        int required = 0, completed = 0;
        for (int i = 1; i <= ym.lengthOfMonth(); i++) {
            LocalDate d = ym.atDay(i);
            boolean req = h.isRequiredOn(d) && !d.isBefore(h.getStartDate());
            boolean isDone = done.contains(d);
            String status;
            if (d.isBefore(h.getStartDate())) status = "BEFORE_START";
            else if (!req) status = "NOT_REQUIRED";
            else if (isDone) status = "COMPLETED";
            else if (d.isBefore(today)) status = "MISSED";
            else if (d.equals(today)) status = "PENDING";
            else status = "UPCOMING";

            if (req && !d.isAfter(today)) {
                required++;
                if (isDone) completed++;
            }
            days.add(new CalendarResponse.Day(d, req, isDone, status));
        }
        int rate = required == 0 ? 0 : Math.round(completed * 100f / required);
        return new CalendarResponse(h.getId(), h.getName(), year, month, days, required, completed, rate);
    }

    // ======================= helpers =======================

    private Habit find(Long id) {
        return habitRepository.findById(id).orElseThrow(() -> new NotFoundException("Habit not found: " + id));
    }

    private void assertNameFree(String name, Long ownId) {
        habitRepository.findByNameIgnoreCase(name.trim()).ifPresent(existing -> {
            if (!existing.getId().equals(ownId)) {
                throw new DuplicateException("A habit named '" + name.trim() + "' already exists");
            }
        });
    }

    private void apply(Habit h, HabitRequest r) {
        if (r.frequency() == Frequency.SPECIFIC_WEEKDAYS && (r.targetDays() == null || r.targetDays().isEmpty())) {
            throw new BusinessRuleException("targetDays must contain at least one weekday when frequency is SPECIFIC_WEEKDAYS");
        }
        h.setName(r.name().trim());
        h.setDescription(r.description());
        h.setFrequency(r.frequency());
        h.getTargetDays().clear();
        if (r.frequency() == Frequency.SPECIFIC_WEEKDAYS) {
            h.getTargetDays().addAll(r.targetDays());
        }
    }

    private HabitResponse toResponse(Habit h) {
        LocalDate today = LocalDate.now();
        Streak s = h.getStreak();
        return new HabitResponse(h.getId(), h.getName(), h.getDescription(), h.getFrequency(),
                new TreeSet<DayOfWeek>(h.getTargetDays()), h.getStartDate(),
                s == null ? 0 : s.getCurrentStreak(), s == null ? 0 : s.getBestStreak(),
                h.isRequiredOn(today) && !today.isBefore(h.getStartDate()),
                logRepository.existsByHabitIdAndLogDate(h.getId(), today));
    }

    private StreakResponse toStreakResponse(Habit h, Streak s) {
        return new StreakResponse(h.getId(), h.getName(), s.getCurrentStreak(), s.getBestStreak(), s.getLastCompletedDate());
    }
}
