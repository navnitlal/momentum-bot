package com.trading.orders;

import com.ib.client.Contract;
import com.trading.ib.IBConnector;
import com.trading.ib.LockManager;
import com.trading.ib.SymbolData;

import java.time.LocalTime;

public class TradeExecutor {

    private final IBConnector ib;
    private final ExecutionHandler executionHandler;
    private final LockManager lockManager;

    private static final LocalTime REGULAR_START = LocalTime.of(9, 30);
    private static final LocalTime REGULAR_END   = LocalTime.of(16, 0);

    public TradeExecutor(IBConnector ib, ExecutionHandler executionHandler, LockManager lockManager) {
        this.ib = ib;
        this.executionHandler = executionHandler;
        this.lockManager = lockManager;
    }

    private boolean isRegularMarketHours() {
        LocalTime now = LocalTime.now();
        return !now.isBefore(REGULAR_START) && !now.isAfter(REGULAR_END);
    }

    public void placeOrder(String symbol, int qty, String action) {
        if (qty <= 0) return;

        SymbolData data = ib.getSymbolData().get(symbol);
        if (data == null) return;

        double lastPrice = data.getLastPrice();
        if (lastPrice <= 0 || Double.isNaN(lastPrice)) return;

        int orderId = ib.getNextOrderId();
        Contract contract = ib.getNewStockContract(symbol);

        boolean isBuy = action.equalsIgnoreCase("BUY");
        var client = ib.getEClient();

        if (isRegularMarketHours()) {
            var order = OrderBuilder.buildMKTOrder(action, qty);
            client.placeOrder(orderId, contract, order);
        } else {
            double offset = Math.min(0.10, lastPrice * 0.01);
            double limitPrice = isBuy ? lastPrice + offset : lastPrice - offset;
            var order = OrderBuilder.buildLimitOrder(action, limitPrice, qty);
            client.placeOrder(orderId, contract, order);
        }

        PendingOrder po = new PendingOrder(symbol, isBuy, qty, orderId, lockManager);
        executionHandler.registerPendingOrder(symbol, po);
    }

    public void forceSell(String symbol) {
        SymbolData symbolData = ib.getSymbolData().get(symbol);
        if (symbolData == null) return;

        int currentPos = symbolData.getPosition();
        if (currentPos <= 0) return;

        double lastPrice = symbolData.getLastPrice();

        Contract contract = ib.getNewStockContract(symbol);
        int orderId = ib.getNextOrderId();

        var client = ib.getEClient();

        if (isRegularMarketHours()) {
            var order = OrderBuilder.buildMKTOrder("SELL", currentPos);
            client.placeOrder(orderId, contract, order);
        } else {
            double offset = Math.min(0.10, lastPrice * 0.01);
            double limitPrice = lastPrice - offset;
            var order = OrderBuilder.buildLimitOrder("SELL", limitPrice, currentPos);
            client.placeOrder(orderId, contract, order);
        }

        PendingOrder po = new PendingOrder(symbol, false, currentPos, orderId, lockManager);
        executionHandler.registerPendingOrder(symbol, po);
    }
}