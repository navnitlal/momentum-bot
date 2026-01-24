package com.trading.services;

import com.trading.strategy.StrategyManager;
import com.trading.strategy.StrategyType;

import java.time.LocalTime;

public class StrategyService {

    private final StrategyManager strategyManager;
    private final ScannerService scannerService;

    public StrategyService(StrategyManager strategyManager, ScannerService scannerService) {
        this.strategyManager = strategyManager;
        this.scannerService = scannerService;
    }

    public void runEvaluation() {
        StrategyType activeStrategy = strategyManager.getActiveStrategy(LocalTime.now());
        scannerService.getActiveSymbols().forEach(symbol -> strategyManager.evaluateMarketForSymbol(symbol, activeStrategy));
    }
}