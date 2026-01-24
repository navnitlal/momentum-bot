package com.trading.bot;

import com.trading.ib.LockManager;
import com.trading.services.*;
import com.trading.datafeed.BarManager;
import com.trading.ib.IBConnector;
import com.trading.indicators.IndicatorManager;
import com.trading.orders.ExecutionHandler;
import com.trading.orders.TradeExecutor;
import com.trading.scanner.IBKRScanner;
import com.trading.settings.ConfigThreshold;
import com.trading.signals.SignalManager;
import com.trading.strategy.StrategyManager;
import com.trading.console.Dashboard;

public class ScalperBot {
    private final ConnectionService connectionService;
    private final SchedulerService schedulerService;
    private final TradeService tradeService;

    public ScalperBot(boolean liveMode) throws Exception {
        IBConnector ib = new IBConnector();
        ConfigThreshold configThreshold = new ConfigThreshold();
        LockManager lockManager = new LockManager();
        BarManager barManager = new BarManager(ib, lockManager);
        IndicatorManager indicatorManager = new IndicatorManager(configThreshold, ib, lockManager);
        SignalManager signalManager = new SignalManager(indicatorManager, configThreshold);
        ExecutionHandler executionHandler = new ExecutionHandler(ib, lockManager);
        TradeExecutor tradeExecutor = new TradeExecutor(ib, executionHandler, lockManager);
        StrategyManager strategyManager = new StrategyManager(ib, tradeExecutor, executionHandler, signalManager, lockManager);
        IBKRScanner scanner = new IBKRScanner(ib)
                .setPriceRange(2.0, 20)
                .setScanCode("TOP_PERC_GAIN")
                .setMinVolumeFilter(100_000)
                .setScanLimit(5);
        BarPipelineService barPipelineService = new BarPipelineService(barManager, indicatorManager);
        ScannerService scannerService = new ScannerService(ib, strategyManager, scanner, tradeExecutor, executionHandler,barPipelineService);
        StrategyService strategyService = new StrategyService(strategyManager, scannerService);
        Dashboard dashboard = new Dashboard(ib);
        this.connectionService = new ConnectionService(ib, liveMode);
        this.schedulerService = new SchedulerService(scannerService, strategyService, dashboard);
        this.tradeService = new TradeService(executionHandler, "trades.csv");
    }

    public void start() {
        connectionService.connect();
        schedulerService.start();
        Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        System.out.println("ScalperBot started. Scanning, filtering, and applying strategy dynamically...");
    }

    public void stop() {
        System.out.println("Stopping ScalperBot...");
        connectionService.disconnect();
        schedulerService.stop();
        tradeService.removeCustomListener("logger");
    }
}