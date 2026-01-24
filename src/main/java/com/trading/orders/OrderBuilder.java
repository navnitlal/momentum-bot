package com.trading.orders;

import com.ib.client.Decimal;
import com.ib.client.Order;

public class OrderBuilder {

    public static Order buildLimitOrder(String action, double price, int qty) {
        Order order = new Order();
        order.action(action);
        order.totalQuantity(Decimal.get(qty));
        order.orderType("LMT");
        order.lmtPrice(price);
        order.tif("DAY");
        order.outsideRth(true);
        order.transmit(true);
        return order;
    }

    public static Order buildMKTOrder(String action, int qty) {
        Order order = new Order();
        order.action(action);
        order.totalQuantity(Decimal.get(qty));
        order.orderType("MKT");
        order.tif("DAY");
        order.transmit(true);
        return order;
    }

    public static Order buildSellOrder(String action, int qty, int orderId, double price) {
        Order order = new Order();
        order.action(action);
        order.orderType("LMT");
        order.lmtPrice(price - 0.05);
        order.totalQuantity(Decimal.get(qty));
        order.orderId(orderId);
        order.tif("DAY");
        order.outsideRth(true);
        order.transmit(true);
        return order;
    }
}