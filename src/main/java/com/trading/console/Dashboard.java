package com.trading.console;

import com.trading.ib.IBConnector;
import com.trading.ib.SymbolData;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Dashboard {
    private final IBConnector ib;

    private static final String RESET = "\u001B[0m";
    private static final String RED = "\u001B[31m";
    private static final String GREEN = "\u001B[32m";
    private static final String YELLOW = "\u001B[33m";
    private static final String CYAN = "\u001B[36m";

    private final Map<String, Integer> lastPositions = new ConcurrentHashMap<>();
    private final Map<String, Double> lastPrices = new ConcurrentHashMap<>();
    private volatile double accountBalance = 0.0;

    public Dashboard(IBConnector ib) {
        this.ib = ib;
        ib.addAccountListener(balance -> {
            accountBalance = balance;
        });
    }

    public Runnable printDashboard() {
        return () -> {
            double totalUnrealized = 0;
            double totalRealized = 0;

            System.out.println("=============================================== SCALPER DASHBOARD ==============================================");
            System.out.printf("%-6s | %-7s | %-7s | %-7s | %-9s | %-9s | %-9s | Last 5%n",
                    "SYM", "Price", "Entry", "Pos", "UnrealPnL", "RealPnL", "TotalPnL");
            System.out.println("================================================================================================================");

            Map<String, SymbolData> symbolDataMap = ib.getSymbolData();

            for (String symbol : symbolDataMap.keySet()) {
                SymbolData data = symbolDataMap.get(symbol);
                double price = data.getLastPrice();
                int position = data.getPosition();
                double entry = data.getEntryPrice();
                double realized = data.getRealizedPnl();
                Deque<Double> priceHistory = data.getPricesSnapshot();

                double unrealized = position * (price - entry);
                double pnl = unrealized + realized;

                String unrealizedColor = unrealized > 0 ? GREEN : (unrealized < 0 ? RED : YELLOW);
                String realizedColor = realized > 0 ? GREEN : (realized < 0 ? RED : YELLOW);
                String totalColor = pnl > 0 ? GREEN : (pnl < 0 ? RED : YELLOW);

                double prevPrice = lastPrices.getOrDefault(symbol, price);
                String priceColor;
                if (price > prevPrice) priceColor = GREEN;
                else if (price < prevPrice) priceColor = RED;
                else priceColor = YELLOW;
                lastPrices.put(symbol, price);

                int lastPos = lastPositions.getOrDefault(symbol, 0);
                String posColor;
                if (position > lastPos) posColor = CYAN;
                else if (position < lastPos) posColor = RED;
                else posColor = RESET;
                lastPositions.put(symbol, position);

                StringBuilder lastPricesStr = new StringBuilder();
                priceHistory.stream().skip(Math.max(0, priceHistory.size() - 5))
                        .forEach(p -> lastPricesStr.append(String.format("%.2f ", p)));

                System.out.printf(
                        "%-6s | %s%-7.2f%s | %-7.2f | %s%-7d%s | %s%-9.2f%s | %s%-9.2f%s | %s%-9.2f%s | %s%n",
                        symbol,
                        priceColor, price, RESET,
                        entry,
                        posColor, position, RESET,
                        unrealizedColor, unrealized, RESET,
                        realizedColor, realized, RESET,
                        totalColor, pnl, RESET,
                        lastPricesStr.toString().trim()
                );

                totalUnrealized += unrealized;
                totalRealized += realized;
            }

            double equity = accountBalance + totalUnrealized + totalRealized;

            String totalUnrealColor = totalUnrealized > 0 ? GREEN : (totalUnrealized < 0 ? RED : YELLOW);
            String totalRealColor = totalRealized > 0 ? GREEN : (totalRealized < 0 ? RED : YELLOW);
            String equityColor = equity > accountBalance ? GREEN : (equity < accountBalance ? RED : YELLOW);

            System.out.printf(
                    "PORTFOLIO SUMMARY | Equity: %s%.2f%s | Unrealized: %s%.2f%s | Realized: %s%.2f%s | Cash: %.2f%n",
                    equityColor, equity, RESET,
                    totalUnrealColor, totalUnrealized, RESET,
                    totalRealColor, totalRealized, RESET,
                    accountBalance
            );

            System.out.println("================================================================================================================\n");
        };
    }
}