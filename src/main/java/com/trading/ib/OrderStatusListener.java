package com.trading.ib;

public interface OrderStatusListener {
    void onOrderStatus(String symbol, int orderId, String status,
                       int filled, int remaining, double avgFillPrice);
}
