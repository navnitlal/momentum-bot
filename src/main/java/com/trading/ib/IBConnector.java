package com.trading.ib;

import com.ib.client.*;
import com.trading.scanner.ScanDataEvent;
import com.trading.strategy.StrategyType;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class IBConnector extends IBWrapperAdapter {

    private final EClientSocket client;
    private final EReaderSignal signal;

    // Symbol data storage
    private final Map<String, SymbolData> symbolDataMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> tickerIdToSymbol = new ConcurrentHashMap<>();
    private final AtomicInteger nextTickerId = new AtomicInteger(1);
    private final AtomicInteger orderId = new AtomicInteger(-1);
    private volatile double accountBalance = 0.0;

    private static final int DEPTH_OFFSET = 10000;
    private static final int RTBAR_OFFSET = 40000;

    // Listeners
    private final List<TickListener> tickListeners = new CopyOnWriteArrayList<>();
    private final List<OrderBookListener> orderBookListeners = new CopyOnWriteArrayList<>();
    private final List<AccountListener> accountListeners = new CopyOnWriteArrayList<>();
    private final List<ExecutionListener> executionListeners = new CopyOnWriteArrayList<>();
    private final List<OrderStatusListener> orderStatusListeners = new CopyOnWriteArrayList<>();
    private final List<RealTimeBarListener> realTimeBarListeners = new CopyOnWriteArrayList<>();
    private final Map<Integer, CopyOnWriteArrayList<Consumer<ScanDataEvent>>> scannerDataListeners = new ConcurrentHashMap<>();
    private final Map<Integer, Runnable> scannerCompleteListeners = new ConcurrentHashMap<>();

    private Thread readerThread;

    public IBConnector() {
        this.signal = new EJavaSignal();
        this.client = new EClientSocket(this, signal);
    }

    // ======================== CONNECTION ========================
    public void connect(String host, int port, int clientId) {
        client.eConnect(host, port, clientId);
        if (client.isConnected()) System.out.println("Connected to TWS at " + host + ":" + port);
        startReaderThread();
        subscribeAccountSummary();
    }

    public void disconnect() {
        try {
            if (readerThread != null) {
                readerThread.interrupt();
                readerThread = null;
            }
        } catch (Exception ignored) {}
        client.eDisconnect();
        System.out.println("Disconnected from IBKR.");
    }

    private void startReaderThread() {
        EReader reader = new EReader(client, signal);
        reader.start();

        readerThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted() && client.isConnected()) {
                    signal.waitForSignal();
                    try {
                        reader.processMsgs();
                    } catch (IOException e) {
                        System.err.println("[IBConnector] Reader processMsgs IOException: " + e.getMessage());
                        e.printStackTrace();
                    } catch (Throwable t) {
                        System.err.println("[IBConnector] Reader thread fatal: " + t.getMessage());
                        t.printStackTrace();
                    }
                }
            } finally {
                System.out.println("[IBConnector] Reader thread exiting.");
            }
        }, "ib-reader-thread");

        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void subscribeAccountSummary() {
        client.reqAccountSummary(9001, "All", "AvailableFunds,BuyingPower,NetLiquidation,TotalCashValue");
    }

    // ======================== SUBSCRIBE/UNSUBSCRIBE SYMBOL ========================

    public void subscribeSymbol(String symbol, StrategyType strategyType) {
        int tickerId = getNextTickerId();
        Contract contract = createStockContract(symbol);

        SymbolData data = symbolDataMap.computeIfAbsent(symbol, s -> new SymbolData());
        data.setStrategyType(strategyType);

        tickerIdToSymbol.put(tickerId, symbol);
        client.reqMktData(tickerId, contract, "", false, false, null);
        client.reqMktDepth(tickerId + DEPTH_OFFSET, contract, 10, true, null);
        client.reqRealTimeBars(tickerId + RTBAR_OFFSET, contract, 5, "TRADES", false, null);

        System.out.printf("[IBConnector] Subscribed %s (tickerId = %d) strategy = %s%n", symbol, tickerId, strategyType);
    }

    public void unsubscribeSymbol(String symbol) {
        tickerIdToSymbol.entrySet().removeIf(e -> {
            if (e.getValue().equals(symbol)) {
                int tid = e.getKey();
                try { client.cancelMktData(tid); } catch (Exception ignored) {}
                try { client.cancelMktDepth(tid + DEPTH_OFFSET, false); } catch (Exception ignored) {}
                try { client.cancelRealTimeBars(tid + RTBAR_OFFSET); } catch (Exception ignored) {}
                return true;
            }
            return false;
        });

        symbolDataMap.remove(symbol);
        System.out.println("Unsubscribed: " + symbol);
    }

    private Contract createStockContract(String symbol) {
        Contract contract = new Contract();
        contract.symbol(symbol);
        contract.secType("STK");
        contract.currency("USD");
        contract.exchange("SMART");
        return contract;
    }

    // ======================== EVENT LISTENER REGISTRATION (ADD/REMOVE) ========================
    public void addOrderBookListener(OrderBookListener listener) { orderBookListeners.add(listener); }
    public void removeOrderBookListener(OrderBookListener listener) { orderBookListeners.remove(listener); }

    public void addAccountListener(AccountListener listener) { accountListeners.add(listener); }
    public void removeAccountListener(AccountListener listener) { accountListeners.remove(listener); }

    public void addExecutionListener(ExecutionListener listener) { executionListeners.add(listener); }
    public void removeExecutionListener(ExecutionListener listener) { executionListeners.remove(listener); }

    public void addOrderStatusListener(OrderStatusListener listener) { orderStatusListeners.add(listener); }
    public void removeOrderStatusListener(OrderStatusListener listener) { orderStatusListeners.remove(listener); }

    public void addRealTimeBarListener(RealTimeBarListener listener) { realTimeBarListeners.add(listener); }
    public void removeRealTimeBarListener(RealTimeBarListener listener) { realTimeBarListeners.remove(listener); }

    public void startScannerListener(int tickerId, Consumer<ScanDataEvent> dataConsumer, Runnable completion) {
        scannerDataListeners.computeIfAbsent(tickerId, (k) -> new CopyOnWriteArrayList<>()).add(dataConsumer);
        if (completion != null) scannerCompleteListeners.put(tickerId, completion);
    }

    // ======================== GETTERS ========================
    public EClientSocket getEClient() { return client; }
    public Map<String, SymbolData> getSymbolData() { return symbolDataMap; }
    public int getNextTickerId() { return nextTickerId.getAndIncrement(); }
    public int getNextOrderId() { return orderId.getAndIncrement(); }
    public Contract getNewStockContract(String symbol){ return createStockContract(symbol); }

    // ======================== CALLBACKS FROM IB ========================
    @Override
    public void nextValidId(int id) {
        orderId.set(id);
        System.out.println("[IBConnector] nextValidId: " + id);
    }

    @Override
    public void tickPrice(int tickerId, int field, double price, TickAttrib attrib) {
        String symbol = tickerIdToSymbol.get(tickerId);
        if (symbol == null) return;
        if (field != TickType.LAST.ordinal() && field != TickType.CLOSE.ordinal()) return;

        SymbolData data = symbolDataMap.get(symbol);
        if (data == null) return;

        data.addPrice(price);
        StrategyType currentStrategy = data.getStrategyType();

        tickListeners.forEach(l -> l.onTick(symbol, data.getLastPrice(), data.getLastVolume(), System.currentTimeMillis(), currentStrategy));
    }

    @Override
    public void tickSize(int tickerId, int field, Decimal size) {
        if (field != TickType.LAST_SIZE.ordinal()) return;

        String symbol = tickerIdToSymbol.get(tickerId);
        if (symbol == null) return;

        long tradeSize = size.longValue();
        if (tradeSize < 0) return;

        SymbolData data = symbolDataMap.get(symbol);
        if (data == null) return;

        data.addVolume(tradeSize);
        StrategyType currentStrategy = data.getStrategyType();

        tickListeners.forEach(l -> l.onTick(symbol, data.getLastPrice(), data.getLastVolume(), System.currentTimeMillis(), currentStrategy));
    }

    @Override
    public void realtimeBar(int reqId, long time, double open, double high, double low,
                            double close, Decimal volume, Decimal wap, int count) {
        String symbol = tickerIdToSymbol.get(reqId - RTBAR_OFFSET);
        if (symbol == null) return;

        long vol = (volume == null) ? 0L : volume.longValue();
        realTimeBarListeners.forEach(l->l.onRealTimeBar(symbol, time, open, high, low, close, vol));
    }

    @Override
    public void updateMktDepthL2(int tickerId, int position, String marketMaker,
                                 int side, int operation, double price, Decimal size, boolean isSmartDepth) {
        String symbol = tickerIdToSymbol.get(tickerId - DEPTH_OFFSET);
        if (symbol == null) return;

        boolean isBid = side == 0;
        long qty = size == null ? 0L : size.longValue();
        SymbolData data = symbolDataMap.get(symbol);
        if (data == null) return;
        StrategyType currentStrategy = data.getStrategyType();

        orderBookListeners.forEach(l -> l.onOrderBookUpdate(symbol, isBid, price, qty, operation, currentStrategy));
    }

    @Override
    public void accountSummary(int reqId, String account, String tag, String value, String currency) {
        if ("AvailableFunds".equals(tag)) {
            try { accountBalance = Double.parseDouble(value); }
            catch (NumberFormatException ignored) {}
            accountListeners.forEach(l -> l.onAccountUpdate(accountBalance));
        }
    }

    @Override
    public void position(String account, Contract contract, Decimal pos, double avgCost) {
        String symbol = contract.symbol();
        int position = (int) pos.longValue();
        SymbolData data = symbolDataMap.computeIfAbsent(symbol, s -> new SymbolData());
        data.setPosition(position);
    }

    @Override
    public void execDetails(int reqId, Contract contract, Execution execution) {
        if (execution == null) return;
        long shares = execution.shares() == null ? 0L : execution.shares().longValue();
        if (shares <= 0) return;
        String symbol = contract.symbol();
        int filledQty = (int) shares;
        double fillPrice = execution.price();

        SymbolData data = symbolDataMap.get(symbol);
        if (data == null) return;
        StrategyType currentStrategy = data.getStrategyType();
        executionListeners.forEach(l -> l.onExecutionConfirmed(symbol, filledQty, fillPrice, currentStrategy));
    }

    @Override
    public void scannerData(int reqId, int rank, ContractDetails contractDetails,
                            String distance, String benchmark, String projection, String legsStr) {
        List<Consumer<ScanDataEvent>> consumers = scannerDataListeners.get(reqId);
        if (consumers != null) {
            ScanDataEvent event = new ScanDataEvent(contractDetails.contract(), rank, distance, benchmark, projection, legsStr);
            consumers.forEach(c -> c.accept(event));
        }
    }

    @Override
    public void scannerDataEnd(int reqId) {
        Runnable completion = scannerCompleteListeners.remove(reqId);
        if (completion != null) completion.run();
        scannerDataListeners.remove(reqId);
    }

    @Override
    public void error(int i, long l, int i1, String s, String s1) {
//        System.err.printf("IB Error: i=%d l=%d i1=%d s=%s s1=%s %n", i, l, i1, s, s1);
    }

    @Override
    public void orderStatus(int orderId, String status, Decimal filled, Decimal remaining,
                            double avgFillPrice, long permId, int parentId,
                            double lastFillPrice, int clientId,
                            String whyHeld, double mktCapPrice) {

        int filledQty = (filled == null) ? 0 : Integer.getInteger(filled.toString());
        int remainingQty = (remaining == null) ? 0 : Integer.getInteger(remaining.toString());

        orderStatusListeners.forEach(l -> l.onOrderStatus(null, orderId, status, filledQty, remainingQty, avgFillPrice));
    }

    // ======================== LISTENER INTERFACES ========================
    public interface TickListener {
        void onTick(String symbol, double price, long volume, long timestamp, StrategyType currentStrategy);
    }

    public interface OrderBookListener {
        void onOrderBookUpdate(String symbol, boolean isBid, double price, long size, int operation, StrategyType currentStrategy);
    }

    public interface AccountListener {
        void onAccountUpdate(double availableFunds);
    }

    public interface RealTimeBarListener {
        void onRealTimeBar(String symbol, long startTime, double open, double high,
                           double low, double close, long volume);
    }

    public interface ExecutionListener {
        void onExecutionConfirmed(String symbol, int filledQty, double fillPrice, StrategyType strategyType);
    }

    public interface OrderStatusListener {
        void onOrderStatus(String symbol, int orderId, String status,
                           int filled, int remaining, double avgFillPrice);
    }
}