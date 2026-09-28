package com.example.habitforge.controller;

import com.example.habitforge.dto.*;
import com.example.habitforge.service.HabitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/habits")
@CrossOrigin(origins = "*")
public class HabitController {

    @Autowired private HabitService habitService;

    // ---- habits ----
    @PostMapping
    public ResponseEntity<HabitResponse> create(@Valid @RequestBody HabitRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(habitService.create(request));
    }

    @GetMapping
    public List<HabitResponse> list() { return habitService.list(); }

    @GetMapping("/today")
    public List<HabitResponse> dueToday() { return habitService.dueToday(); }

    @GetMapping("/{id}")
    public HabitResponse get(@PathVariable Long id) { return habitService.get(id); }

    @PutMapping("/{id}")
    public HabitResponse update(@PathVariable Long id, @Valid @RequestBody HabitRequest request) {
        return habitService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        habitService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ---- completion logs ----
    @PostMapping("/{id}/logs")
    public ResponseEntity<StreakResponse> log(@PathVariable Long id,
                                              @Valid @RequestBody(required = false) LogRequest request) {
        LocalDate date = request == null ? null : request.date();
        String note = request == null ? null : request.note();
        return ResponseEntity.status(HttpStatus.CREATED).body(habitService.logCompletion(id, date, note));
    }

    @GetMapping("/{id}/logs")
    public List<LogResponse> logs(@PathVariable Long id) { return habitService.logs(id); }

    @DeleteMapping("/{id}/logs")
    public StreakResponse undo(@PathVariable Long id,
                               @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return habitService.removeLog(id, date);
    }

    // ---- streaks ----
    @GetMapping("/{id}/streak")
    public StreakResponse streak(@PathVariable Long id) { return habitService.getStreak(id); }

    /** Manually trigger the missed-day check (the scheduler also does this every night). */
    @PostMapping("/refresh-streaks")
    public Map<String, Integer> refresh() { return habitService.refreshAllStreaks(); }

    // ---- monthly calendar ----
    @GetMapping("/{id}/calendar")
    public CalendarResponse calendar(@PathVariable Long id,
                                     @RequestParam(required = false) Integer year,
                                     @RequestParam(required = false) Integer month) {
        LocalDate now = LocalDate.now();
        return habitService.calendar(id, year == null ? now.getYear() : year, month == null ? now.getMonthValue() : month);
    }
}
