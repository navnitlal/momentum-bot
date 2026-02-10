package com.trading.services;

import com.trading.console.Dashboard;
import com.trading.settings.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.*;

public class SchedulerService {

    private static final Logger log = LoggerFactory.getLogger(SchedulerService.class);

    private final ScannerService scannerService;
    private final StrategyService strategyService;
    private final ScheduledExecutorService executor;
    private final Dashboard dashboard;
    private final AppConfig.SchedulerConfig config;

    public SchedulerService(ScannerService scannerService, StrategyService strategyService,
                            Dashboard dashboard, AppConfig.SchedulerConfig config) {
        this.scannerService = scannerService;
        this.strategyService = strategyService;
        this.dashboard = dashboard;
        this.config = config;
        this.executor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    public void start() {
        executor.scheduleAtFixedRate(
                scannerService::scanAndUpdate,
                config.scanStartDelayMs(),
                config.scanIntervalMs(),
                TimeUnit.MILLISECONDS
        );
        log.info("Started scanning loop.");

        executor.scheduleAtFixedRate(
                strategyService::runEvaluation,
                config.strategyStartDelayMs(),
                config.strategyIntervalMs(),
                TimeUnit.MILLISECONDS
        );
        log.info("Started market evaluation loop.");

        executor.scheduleAtFixedRate(
                Objects.requireNonNull(dashboard.printDashboard()),
                config.printStartDelayMs(),
                config.printIntervalMs(),
                TimeUnit.MILLISECONDS
        );
        log.info("Live dashboard updates enabled.");
    }

    public void stop() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Stopped all scheduled tasks.");
    }
}
