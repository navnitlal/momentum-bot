package com.trading.bot;

import com.trading.ib.LockManager;
import com.trading.services.*;
import com.trading.datafeed.BarManager;
import com.trading.ib.IBConnector;
import com.trading.indicators.IndicatorManager;
import com.trading.orders.ExecutionHandler;
import com.trading.orders.TradeExecutor;
import com.trading.scanner.IBKRScanner;
import com.trading.settings.AppConfig;
import com.trading.settings.ConfigThreshold;
import com.trading.signals.SignalManager;
import com.trading.strategy.StrategyManager;
import com.trading.console.Dashboard;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

public class ScalperBot {

    private static final Logger log = LoggerFactory.getLogger(ScalperBot.class);

    private final ConnectionService connectionService;
    private final SchedulerService schedulerService;
    private final TradeService tradeService;
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public ScalperBot(boolean liveMode) throws Exception {
        AppConfig appConfig = AppConfig.load();
        AppConfig.ScannerConfig sc = appConfig.scanner();
        AppConfig.TradingConfig tc = appConfig.trading();

        IBConnector ib = new IBConnector();
        ConfigThreshold configThreshold = new ConfigThreshold();
        LockManager lockManager = new LockManager();
        BarManager barManager = new BarManager(ib, lockManager);
        IndicatorManager indicatorManager = new IndicatorManager(configThreshold, ib, lockManager);
        SignalManager signalManager = new SignalManager(indicatorManager, configThreshold);
        ExecutionHandler executionHandler = new ExecutionHandler(ib, lockManager);
        TradeExecutor tradeExecutor = new TradeExecutor(ib, executionHandler, lockManager);
        StrategyManager strategyManager = new StrategyManager(ib, tradeExecutor, executionHandler, signalManager, lockManager,
                tc.maxAllocationFraction(), tc.maxSharesPerStock(), tc.riskPerTradeFraction());
        IBKRScanner scanner = new IBKRScanner(ib)
                .setPriceRange(sc.minPrice(), sc.maxPrice())
                .setScanCode(sc.scanCode())
                .setMinVolumeFilter(sc.minVolume())
                .setScanLimit(sc.scanLimit());
        BarPipelineService barPipelineService = new BarPipelineService(barManager, indicatorManager);
        ScannerService scannerService = new ScannerService(ib, strategyManager, scanner, tradeExecutor, executionHandler,
                barPipelineService, sc.timeoutMs());
        StrategyService strategyService = new StrategyService(strategyManager, scannerService);
        Dashboard dashboard = new Dashboard(ib);
        this.connectionService = new ConnectionService(ib, liveMode, appConfig.connection());
        this.schedulerService = new SchedulerService(scannerService, strategyService, dashboard, appConfig.scheduler());
        this.tradeService = new TradeService(executionHandler, tc.tradeLogPath());
    }

    public void start() {
        connectionService.connect();
        schedulerService.start();
        log.info("ScalperBot started. Scanning, filtering, and applying strategy dynamically...");
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;
        log.info("Stopping ScalperBot...");
        schedulerService.stop();
        connectionService.disconnect();
        tradeService.removeCustomListener("logger");
    }
}
