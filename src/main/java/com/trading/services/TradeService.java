package com.trading.services;

import com.trading.orders.ExecutionHandler;
import com.trading.orders.TradeLogger;

public class TradeService {
    private final ExecutionHandler executionHandler;

    public TradeService(ExecutionHandler executionHandler, String logFile) {
        this.executionHandler = executionHandler;
        TradeLogger tradeLogger = new TradeLogger(logFile);

        executionHandler.addTradeListener("logger", tradeLogger);
    }

    public void addCustomListener(String key, ExecutionHandler.TradeListener listener) {
        executionHandler.addTradeListener(key, listener);
    }

    public void removeCustomListener(String key) {
        executionHandler.removeTradeListener(key);
    }
}