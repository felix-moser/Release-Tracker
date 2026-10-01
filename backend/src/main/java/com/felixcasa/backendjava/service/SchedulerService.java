package com.felixcasa.backendjava.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
@Configuration
public class SchedulerService implements SchedulingConfigurer {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);

    private final RepoService repoService;
    private final SystemService systemService;
    private boolean isRunning = true;

    public SchedulerService(RepoService repoService, SystemService systemService) {
        this.repoService = repoService;
        this.systemService = systemService;
    }

    public int getIntervalMinutes() {
        String val = systemService.getSecret("CHECK_INTERVAL_MINUTES");
        if (val != null) {
            try {
                return Math.max(1, Integer.parseInt(val));
            } catch (NumberFormatException ignored) {}
        }
        return 30;
    }

    public boolean isRunning() {
        return isRunning;
    }

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.addTriggerTask(
            () -> {
                try {
                    logger.debug("Starting scheduled check cycle for all repositories");
                    repoService.checkAllRepos();
                    logger.debug("Finished scheduled check cycle");
                } catch (Exception e) {
                    logger.error("Scheduler: unexpected error during check cycle", e);
                }
            },
            triggerContext -> {
                Optional<Date> lastCompletionTime = Optional.ofNullable(triggerContext.lastCompletionTime());
                Instant nextExecutionTime = lastCompletionTime.orElseGet(Date::new).toInstant()
                        .plusSeconds(getIntervalMinutes() * 60L);
                return nextExecutionTime;
            }
        );
    }
}
