# HabitForge - Personal Habit Streak Tracker with Reminders
Spring Boot 3.3 | Java 21 | Maven | MySQL | Swagger

1. Set your MySQL password in `src/main/resources/application.properties` (database `habitforgedb` is auto-created)
2. `mvn spring-boot:run`
3. App: http://localhost:8080  |  Swagger: http://localhost:8080/swagger-ui.html

## Endpoints
| Method | URL | Purpose |
|---|---|---|
| POST | /api/habits | Create habit (DAILY or SPECIFIC_WEEKDAYS) |
| GET | /api/habits, /api/habits/{id}, /api/habits/today | List / get / due today |
| PUT / DELETE | /api/habits/{id} | Update / delete |
| POST | /api/habits/{id}/logs | Log done (body optional: {"date":"yyyy-MM-dd","note":"..."}) |
| GET / DELETE | /api/habits/{id}/logs | List logs / undo (?date=yyyy-MM-dd) |
| GET | /api/habits/{id}/streak | Current + best streak (missed days detected) |
| POST | /api/habits/refresh-streaks | Detect missed days for all habits |
| GET | /api/habits/{id}/calendar?year=&month= | Monthly completion calendar |

## Business rules (enforced in HabitService)
- A streak resets to zero the day after a required day is missed (today is never "missed" until it has passed)
- Best streak only increases, when a streak exceeds the stored best
- Cannot log future dates, dates before the habit start, non-scheduled weekdays, or the same day twice
- Console notifications: log / streak-reset / new-best messages, nightly missed-day check (00:05), 20:00 reminders
