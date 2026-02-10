package com.trading.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TradeLogger implements ExecutionHandler.TradeListener {

    private static final Logger log = LoggerFactory.getLogger(TradeLogger.class);

    private final String logFile;
    private final Lock lock = new ReentrantLock();

    public TradeLogger(String logFile) {
        this.logFile = logFile;
        File parent = new File(logFile).getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    @Override
    public void onTradeExecuted(String symbol, String action, double price, int qty, double realizedPnL) {
        lock.lock();
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(Instant.now().toEpochMilli() + "," +
                    symbol + "," +
                    action + "," +
                    String.format("%.2f", price) + "," +
                    qty + "," +
                    String.format("%.2f", realizedPnL) + "\n");
        } catch (IOException e) {
            log.error("Failed to write trade log", e);
        } finally {
            lock.unlock();
        }
    }
}
