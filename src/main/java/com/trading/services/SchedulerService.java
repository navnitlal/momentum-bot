package com.trading.services;

import com.trading.console.Dashboard;

import java.util.Objects;
import java.util.concurrent.*;

public class SchedulerService {

    private final ScannerService scannerService;
    private final StrategyService strategyService;
    private final ScheduledExecutorService executor;
    private final  Dashboard dashboard;

    private static final int SCAN_INTERVAL = 5_000;
    private static final int SCAN_START_DELAY = 0;
    private static final int STRATEGY_INTERVAL = 1_000;
    private static final int STRATEGY_START_DELAY = 1_000;
    private static final int PRINT_INTERVAL = 1_000;
    private static final int PRINT_START_DELAY = 2_000;

    public SchedulerService(ScannerService scannerService, StrategyService strategyService, Dashboard dashboard) {
        this.scannerService = scannerService;
        this.strategyService = strategyService;
        this.dashboard = dashboard;
        this.executor = Executors.newScheduledThreadPool(
                Runtime.getRuntime().availableProcessors()
        );
    }

    public void start() {
        executor.scheduleAtFixedRate(
                scannerService::scanAndUpdate,
                SCAN_START_DELAY,
                SCAN_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        System.out.println("[SchedulerService] Started scanning loop.");

        executor.scheduleAtFixedRate(
                strategyService::runEvaluation,
                STRATEGY_START_DELAY,
                STRATEGY_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        System.out.println("[StrategyService] Started market evaluation loop.");

        executor.scheduleAtFixedRate(
                Objects.requireNonNull(dashboard.printDashboard()),
                PRINT_START_DELAY,
                PRINT_INTERVAL,
                TimeUnit.MILLISECONDS
        );
        System.out.println("[Dashboard] Live updates enabled.");
    }

    public void stop() {
        executor.shutdownNow();
        System.out.println("[SchedulerService] Stopped all scheduled tasks.");
    }
}