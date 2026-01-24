package com.trading.strategy;

import com.trading.ib.IBConnector;
import com.trading.ib.LockManager;
import com.trading.ib.SymbolData;
import com.trading.orders.*;
import com.trading.signals.SignalManager;

import java.time.LocalTime;

public class StrategyManager {

    private final TradeExecutor tradeExecutor;
    private final ExecutionHandler executionHandler;
    private final SignalManager signalManager;
    private final IBConnector ib;
    private final LockManager lockManager;

    private volatile double accountBalance = 0.0;

    private static final double MAX_TOTAL_ALLOCATION_FRACTION = 0.7;
    private static final int MAX_TOTAL_SHARES_PER_STOCK = 1000;

    public StrategyManager(IBConnector ib,
                           TradeExecutor tradeExecutor,
                           ExecutionHandler executionHandler,
                           SignalManager signalManager,
                           LockManager lockManager) {
        this.tradeExecutor = tradeExecutor;
        this.executionHandler = executionHandler;
        this.signalManager = signalManager;
        this.ib = ib;
        this.lockManager = lockManager;

        ib.addAccountListener(balance -> this.accountBalance = balance);
    }

    public StrategyType getActiveStrategy(LocalTime now) {
        if ((now.isAfter(LocalTime.of(4, 30)) && now.isBefore(LocalTime.of(11, 0))) ||
                (now.isAfter(LocalTime.of(15, 30)) && now.isBefore(LocalTime.of(17, 0)))) return StrategyType.MOMENTUM;
        if (now.isAfter(LocalTime.of(11, 0)) && now.isBefore(LocalTime.of(13, 30))) return StrategyType.PULLBACK;
        if (now.isAfter(LocalTime.of(13, 30)) && now.isBefore(LocalTime.of(15, 30))) return StrategyType.RANGE;
        return StrategyType.NEWS;
    }

    public void evaluateMarketForSymbol(String symbol, StrategyType strategy) {
        lockManager.lock(symbol);
        try {
            SymbolData symbolData = ib.getSymbolData().computeIfAbsent(symbol, s -> new SymbolData());
            double tickPrice = symbolData.getLastPrice();
            if (tickPrice <= 0) return;

            int currentPos = symbolData.getPosition();
            PendingOrder pending = executionHandler.getPendingOrder(symbol);

            boolean hasPendingBuy = pending != null && pending.isBuy;
            boolean hasPendingSell = pending != null && !pending.isBuy;


            boolean sellConfirmed = sellConfirmed(symbolData, symbol);
            if (currentPos > 0 && !hasPendingSell && sellConfirmed) {
                System.out.printf("symbol: %s, sellConfirmed: %s, currentPos: %s, hasPendingSell: %s\n", symbol, sellConfirmed, currentPos, hasPendingSell);
                tradeExecutor.placeOrder(symbol, currentPos, "SELL");

            }
            boolean buyConfirmed = signalManager.isBuySignal(symbol, strategy);

            if (buyConfirmed && currentPos == 0 && !hasPendingBuy) {
                System.out.printf("symbol: %s, buyConfirmed: %s, currentPos: %s, hasPendingBuy: %s\n",symbol, buyConfirmed, currentPos, hasPendingBuy);
                int qty = calculateBuyQuantity(tickPrice, strategy);
                if (qty > 0) tradeExecutor.placeOrder(symbol, qty, "BUY");
            }

        } finally {
            lockManager.unlock(symbol);
        }
    }

    private int calculateBuyQuantity(double price, StrategyType strategy) {
        if (price <= 0) return 0;

        double desiredAlloc = accountBalance * MAX_TOTAL_ALLOCATION_FRACTION;
        double minAlloc = accountBalance * 0.01;
        double allocation = Math.max(minAlloc, desiredAlloc);
        double perShareRisk = DynamicRiskManager.getDynamicStopLoss(price, strategy);
        if (perShareRisk <= 1e-8) return 0;

        double maxRiskPerTrade = accountBalance * 0.01;
        int riskQty = (int) Math.floor(maxRiskPerTrade / perShareRisk);
        int allocQty = (int) Math.floor(allocation / price);
        int qty = Math.min(riskQty, allocQty);

        return Math.max(0, Math.min(qty, MAX_TOTAL_SHARES_PER_STOCK));
    }

    private boolean sellConfirmed(SymbolData symbolData, String symbol) {
        double entryPrice = symbolData.getEntryPrice();
        double latestPrice = symbolData.getLastPrice();
        double stopLossDelta = symbolData.getStopLossDelta();
        double trailingDelta = symbolData.getStopTrailingPriceDelta();

        if (entryPrice <= 0 || latestPrice <= 0 || stopLossDelta <= 0) return false;

        double peakPrice = symbolData.getPeakPrice();
        if (latestPrice > peakPrice) {
            symbolData.setPeakPrice(latestPrice);
            peakPrice = latestPrice;
        }

        double stopPrice = entryPrice - stopLossDelta;
        boolean sellSignal;

        if (peakPrice > entryPrice) {
            double trailingStopPrice = peakPrice - trailingDelta;
            if (trailingStopPrice < entryPrice) sellSignal = latestPrice <= stopPrice;
            else sellSignal = latestPrice <= stopPrice || latestPrice <= trailingStopPrice;

            System.out.printf(
                    "symbol: %s | entryPrice: %.2f | peakPrice: %.2f | latestPrice: %.2f | stopPrice: %.2f | trailingStopPrice: %.2f | sellSignal: %s%n",
                    symbol, entryPrice, peakPrice, latestPrice, stopPrice, trailingStopPrice, sellSignal
            );

        } else {
            sellSignal = latestPrice <= stopPrice;
        }

        return sellSignal;
    }
}