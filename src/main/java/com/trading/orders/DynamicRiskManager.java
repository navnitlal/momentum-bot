package com.trading.orders;

import com.trading.strategy.StrategyType;

public class DynamicRiskManager {

    public static double getDynamicStopLoss(double price, StrategyType strategy) {
        double sl;
        if (price < 2) sl = Math.max(price * 0.03, 0.05);
        else if (price < 5) sl = Math.max(price * 0.02, 0.10);
        else if (price < 20) sl = Math.max(price * 0.015, 0.20);
        else sl = Math.max(price * 0.01, 0.25);

        return switch (strategy) {
            case MOMENTUM -> sl;
            case PULLBACK -> sl * 1.2;
            case RANGE -> sl * 0.8;
            case NEWS -> sl * 1.5;
        };
    }

    public static double getDynamicTrailingStop(double price, StrategyType strategy) {
        double ts;
        if (price < 2) ts = Math.max(price * 0.02, 0.04);
        else if (price < 5) ts = Math.max(price * 0.015, 0.07);
        else if (price < 20) ts = Math.max(price * 0.01, 0.10);
        else ts = Math.max(price * 0.0075, 0.15);

        return switch (strategy) {
            case MOMENTUM -> ts;
            case PULLBACK -> ts * 1.1;
            case RANGE -> ts * 0.9;
            case NEWS -> ts * 1.3;
        };
    }
}