package com.example.habitforge.service;

import com.example.habitforge.dto.HabitResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;

/** Daily jobs: detect missed days (streak resets) and send "timely nudges" (console notifications). */
@Component
public class HabitScheduler {

    private static final Logger log = LoggerFactory.getLogger(HabitScheduler.class);

    @Autowired private HabitService habitService;

    /** 00:05 every day - the day after a required day is missed, the streak resets. */
    @Scheduled(cron = "0 5 0 * * *")
    public void detectMissedDays() {
        Map<String, Integer> r = habitService.refreshAllStreaks();
        log.info("[NOTIFY] Daily streak check finished: {}", r);
    }

    /** 20:00 every day - nudge for habits still not done today. */
    @Scheduled(cron = "0 0 20 * * *")
    public void sendReminders() {
        for (HabitResponse h : habitService.dueToday()) {
            if (!h.completedToday()) {
                log.info("[NOTIFY] \uD83D\uDD14 Reminder: '{}' is not done yet today. Keep your {}-day streak alive!",
                        h.name(), h.currentStreak());
            }
        }
    }
}
