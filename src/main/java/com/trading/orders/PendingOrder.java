package com.trading.orders;

import com.trading.ib.LockManager;

public class PendingOrder {

    private final LockManager lockManager;
    public final String symbol;
    public final boolean isBuy;
    private int remainingQty;
    private double avgPrice;
    public final int orderId;

    public PendingOrder(String symbol, boolean isBuy, int qty, int orderId, LockManager lockManager) {
        this.symbol = symbol;
        this.isBuy = isBuy;
        this.remainingQty = qty;
        this.avgPrice = 0.0;
        this.orderId = orderId;
        this.lockManager = lockManager;
    }

    public int getOrderId(){
        return orderId;
    }

    public String getSymbol(){
        return symbol;
    }

    public void addFill(double price, int qty) {
        lockManager.lock(symbol);
        try {
            if (qty <= 0) return;
            avgPrice = (avgPrice * (this.remainingQty - qty) + price * qty) / this.remainingQty;
            remainingQty -= qty;
        } finally {
            lockManager.unlock(symbol);
        }
    }

    public boolean isFilled() {
        lockManager.lock(symbol);
        try {
            return remainingQty <= 0;
        } finally {
            lockManager.unlock(symbol);
        }
    }

    public int getRemainingQty() {
        lockManager.lock(symbol);
        try {
            return remainingQty;
        } finally {
            lockManager.unlock(symbol);
        }
    }

    public double getAvgPrice() {
        lockManager.lock(symbol);
        try {
            return avgPrice;
        } finally {
            lockManager.unlock(symbol);
        }
    }
}