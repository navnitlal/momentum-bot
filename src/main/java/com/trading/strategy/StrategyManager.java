package com.trading.strategy;

import com.trading.ib.IBConnector;
import com.trading.ib.LockManager;
import com.trading.ib.SymbolData;
import com.trading.orders.*;
import com.trading.signals.SignalManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;

public class StrategyManager {

    private static final Logger log = LoggerFactory.getLogger(StrategyManager.class);

    private final TradeExecutor tradeExecutor;
    private final ExecutionHandler executionHandler;
    private final SignalManager signalManager;
    private final IBConnector ib;
    private final LockManager lockManager;

    private volatile double accountBalance = 0.0;

    private final double maxAllocationFraction;
    private final int maxSharesPerStock;
    private final double riskPerTradeFraction;

    public StrategyManager(IBConnector ib,
                           TradeExecutor tradeExecutor,
                           ExecutionHandler executionHandler,
                           SignalManager signalManager,
                           LockManager lockManager,
                           double maxAllocationFraction,
                           int maxSharesPerStock,
                           double riskPerTradeFraction) {
        this.tradeExecutor = tradeExecutor;
        this.executionHandler = executionHandler;
        this.signalManager = signalManager;
        this.ib = ib;
        this.lockManager = lockManager;
        this.maxAllocationFraction = maxAllocationFraction;
        this.maxSharesPerStock = maxSharesPerStock;
        this.riskPerTradeFraction = riskPerTradeFraction;

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
                log.info("symbol: {}, sellConfirmed: {}, currentPos: {}, hasPendingSell: {}", symbol, sellConfirmed, currentPos, hasPendingSell);
                tradeExecutor.placeOrder(symbol, currentPos, "SELL");

            }
            boolean buyConfirmed = signalManager.isBuySignal(symbol, strategy);

            if (buyConfirmed && currentPos == 0 && !hasPendingBuy) {
                log.info("symbol: {}, buyConfirmed: {}, currentPos: {}, hasPendingBuy: {}", symbol, buyConfirmed, currentPos, hasPendingBuy);
                int qty = calculateBuyQuantity(tickPrice, strategy);
                if (qty > 0) tradeExecutor.placeOrder(symbol, qty, "BUY");
            }

        } finally {
            lockManager.unlock(symbol);
        }
    }

    private int calculateBuyQuantity(double price, StrategyType strategy) {
        if (price <= 0) return 0;

        double desiredAlloc = accountBalance * maxAllocationFraction;
        double minAlloc = accountBalance * riskPerTradeFraction;
        double allocation = Math.max(minAlloc, desiredAlloc);
        double perShareRisk = DynamicRiskManager.getDynamicStopLoss(price, strategy);
        if (perShareRisk <= 1e-8) return 0;

        double maxRiskPerTrade = accountBalance * riskPerTradeFraction;
        int riskQty = (int) Math.floor(maxRiskPerTrade / perShareRisk);
        int allocQty = (int) Math.floor(allocation / price);
        int qty = Math.min(riskQty, allocQty);

        return Math.max(0, Math.min(qty, maxSharesPerStock));
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

            log.trace(
                    "symbol: {} | entryPrice: {} | peakPrice: {} | latestPrice: {} | stopPrice: {} | trailingStopPrice: {} | sellSignal: {}",
                    symbol, entryPrice, peakPrice, latestPrice, stopPrice, trailingStopPrice, sellSignal
            );

        } else {
            sellSignal = latestPrice <= stopPrice;
        }

        return sellSignal;
    }
}
