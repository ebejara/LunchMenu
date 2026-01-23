
package com.enrique.lunchmenu;

import java.time.*;
import java.util.concurrent.*;

public class Scheduler {
    private final ScheduledExecutorService exec = Executors.newSingleThreadScheduledExecutor();

    public void scheduleDailyAt(Runnable task, LocalTime time, ZoneId zone) {
        Runnable wrapped = () -> { try { task.run(); } catch (Exception e) { e.printStackTrace(); } };
        long initialDelay = Duration.between(Instant.now(), nextRunInstant(time, zone)).toMillis();
        exec.scheduleAtFixedRate(wrapped, initialDelay, Duration.ofDays(1).toMillis(), TimeUnit.MILLISECONDS);
    }

    private Instant nextRunInstant(LocalTime time, ZoneId zone) {
        ZonedDateTime now = ZonedDateTime.now(zone);
        ZonedDateTime target = now.with(time);
        if (!target.isAfter(now)) target = target.plusDays(1);
        return target.toInstant();
    }
}
``
