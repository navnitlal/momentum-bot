package com.trading.orders;

import com.trading.ib.IBConnector;
import com.trading.ib.ExecutionListener;
import com.trading.ib.LockManager;
import com.trading.ib.OrderStatusListener;
import com.trading.ib.SymbolData;
import com.trading.strategy.StrategyType;

import java.util.concurrent.ConcurrentHashMap;

public class ExecutionHandler implements ExecutionListener, OrderStatusListener {

    private final IBConnector ib;

    private final ConcurrentHashMap<String, PendingOrder> pendingOrders = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, TradeListener> tradeListeners = new ConcurrentHashMap<>();
    private final LockManager lockManager;

    public interface TradeListener {
        void onTradeExecuted(String symbol, String action, double price, int qty, double realizedPnL);
    }

    public ExecutionHandler(IBConnector ib, LockManager lockManager) {
        this.ib = ib;
        this.lockManager = lockManager;
        ib.addExecutionListener(this);
        ib.addOrderStatusListener((symbol, orderId, status, filled, remaining, avgFillPrice) -> {
            PendingOrder po = getPendingOrderById(orderId);
            String resolvedSymbol = (po != null) ? po.getSymbol() : symbol;
            onOrderStatus(resolvedSymbol, orderId, status, filled, remaining, avgFillPrice);
        });
    }

    @Override
    public void onExecutionConfirmed(String symbol, int filledQty, double fillPrice, StrategyType strategyType) {
        String action;
        double realizedPnL;

        lockManager.lock(symbol);
        try {
            PendingOrder po = pendingOrders.get(symbol);
            if (po == null) return;

            po.addFill(fillPrice, filledQty);

            SymbolData symbolData = ib.getSymbolData().computeIfAbsent(symbol, s -> new SymbolData());
            int prevPos = symbolData.getPosition();
            double peakPrice = symbolData.getPeakPrice();
            int newPos;

            if (po.isBuy) {
                newPos = prevPos + filledQty;
                symbolData.setPosition(newPos);

                double prevEntry = symbolData.getEntryPrice();
                double avgEntry = (prevPos == 0)
                        ? fillPrice
                        : (prevEntry * prevPos + fillPrice * filledQty) / newPos;
                symbolData.setEntryPrice(avgEntry);
                symbolData.setPeakPrice(Math.max(peakPrice, fillPrice));
                symbolData.setStopLossDelta(DynamicRiskManager.getDynamicStopLoss(avgEntry, strategyType));
                symbolData.setStopTrailingPriceDelta(DynamicRiskManager.getDynamicTrailingStop(avgEntry, strategyType));

                action = "BUY";
                realizedPnL = symbolData.getRealizedPnl();
            } else {
                newPos = Math.max(0, prevPos - filledQty);
                double prevEntry = symbolData.getEntryPrice();
                realizedPnL = filledQty * (fillPrice - prevEntry);
                symbolData.addRealizedPnl(realizedPnL);

                if (newPos > 0) {
                    double remainingCost = prevEntry * prevPos - fillPrice * filledQty;
                    double avgEntry = remainingCost / newPos;
                    symbolData.setEntryPrice(avgEntry);
                    symbolData.setPosition(newPos);
                } else {
                    symbolData.setEntryPrice(0.0);
                    symbolData.setPosition(0);
                    symbolData.setPeakPrice(0.0);
                    symbolData.setStopLossDelta(0.0);
                    symbolData.setStopTrailingPriceDelta(0.0);
                }
                action = "SELL";
            }

            if (po.isFilled()) pendingOrders.remove(symbol);
        }finally {
            lockManager.unlock(symbol);
        }

        fireTradeListener(symbol, action, fillPrice, filledQty, realizedPnL);
    }

    @Override
    public void onOrderStatus(String symbol, int orderId, String status,
                              int filled, int remaining, double avgFillPrice) {
        lockManager.lock(symbol);
        try {
            PendingOrder po = getPendingOrderById(orderId);
            String normStatus = status.toUpperCase();
            if (normStatus.equals("CANCELLED") ||
                    normStatus.equals("REJECTED") ||
                    normStatus.equals("INACTIVE")) {
                if (po != null) pendingOrders.remove(symbol);
            }
        }finally {
            lockManager.unlock(symbol);
        }
    }

    private void fireTradeListener(String symbol, String action, double price, int qty, double pnl) {
        tradeListeners.forEach((s, listener) -> listener.onTradeExecuted(symbol, action, price, qty, pnl));
    }

    public void registerPendingOrder(String symbol, PendingOrder po) {
        pendingOrders.put(symbol, po);
    }

    public PendingOrder getPendingOrder(String symbol){
        return pendingOrders.get(symbol);
    }

    public PendingOrder getPendingOrderById(int orderId) {
        for (PendingOrder po : pendingOrders.values()) {
            if (po.getOrderId() == orderId) return po;
        }
        return null;
    }

    public void removePendingOrder(String symbol){
        pendingOrders.remove(symbol);
    }

    public void addTradeListener(String key, TradeListener listener){
        tradeListeners.put(key, listener);
    }

    public void removeTradeListener(String key){
        tradeListeners.remove(key);
    }
}