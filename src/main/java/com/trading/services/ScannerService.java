package com.trading.services;

import com.trading.ib.IBConnector;
import com.trading.orders.ExecutionHandler;
import com.trading.orders.TradeExecutor;
import com.trading.scanner.IBKRScanner;
import com.trading.strategy.StrategyManager;
import com.trading.strategy.StrategyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ScannerService {

    private static final Logger log = LoggerFactory.getLogger(ScannerService.class);

    private final IBKRScanner scanner;
    private final IBConnector ib;
    private final TradeExecutor tradeExecutor;
    private final StrategyManager strategyManager;
    private final ExecutionHandler executionHandler;
    private final BarPipelineService barPipelineService;
    private final int scannerTimeoutMs;

    private final Set<String> activeSymbols = ConcurrentHashMap.newKeySet();

    public ScannerService(IBConnector ib,
                          StrategyManager strategyManager,
                          IBKRScanner scanner,
                          TradeExecutor tradeExecutor,
                          ExecutionHandler executionHandler,
                          BarPipelineService barPipelineService,
                          int scannerTimeoutMs) {
        this.ib = ib;
        this.strategyManager = strategyManager;
        this.scanner = scanner;
        this.tradeExecutor = tradeExecutor;
        this.executionHandler = executionHandler;
        this.barPipelineService = barPipelineService;
        this.scannerTimeoutMs = scannerTimeoutMs;
    }


    public void scanAndUpdate() {
        try {
            List<String> scannedSymbols = scanner.scanAndFilter(scannerTimeoutMs);
            Set<String> scannedSet = new HashSet<>(scannedSymbols);

            for (String added : scannedSet) {
                if (activeSymbols.contains(added)) continue;
                StrategyType strategyType = strategyManager.getActiveStrategy(LocalTime.now());
                ib.subscribeSymbol(added, strategyType);
                barPipelineService.registerSymbol(added, strategyType);
                activeSymbols.add(added);
            }

            for (String removed : new HashSet<>(activeSymbols)) {
                if (scannedSet.contains(removed)) continue;
                tradeExecutor.forceSell(removed);
                executionHandler.removePendingOrder(removed);
                ib.unsubscribeSymbol(removed);
                barPipelineService.clearSymbol(removed);
                activeSymbols.remove(removed);
            }

        } catch (Exception e) {
            log.error("Error during scanning", e);
        }
    }

    public Set<String> getActiveSymbols() {
        return activeSymbols;
    }
}
