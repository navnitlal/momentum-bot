package com.trading.scanner;

import com.trading.ib.IBConnector;
import com.ib.client.ScannerSubscription;

import java.util.*;
import java.util.concurrent.*;

public class IBKRScanner {

    private final IBConnector ib;

    private double minPrice;
    private double maxPrice;
    private int minVolume;
    private String scanCode;
    private int scanLimit;

    public IBKRScanner(IBConnector ib) {this.ib = ib; }

    public IBKRScanner setPriceRange(double minPrice, double maxPrice) { this.minPrice = minPrice; this.maxPrice = maxPrice; return this; }
    public IBKRScanner setMinVolumeFilter(int minVolume) { this.minVolume = minVolume; return this; }
    public IBKRScanner setScanCode(String scanCode) { this.scanCode = scanCode; return this; }
    public IBKRScanner setScanLimit(int scanLimit) { this.scanLimit = scanLimit; return this; }

    public List<String> scanAndFilter(int timeout) throws Exception {
        List<String> scannedSymbols = scanRawSymbols(timeout);
        int limit = Math.min(scanLimit, scannedSymbols.size());
        return scannedSymbols.subList(0, limit);
    }

    private List<String> scanRawSymbols(int timeout) throws Exception {
        List<String> scannedSymbols = Collections.synchronizedList(new ArrayList<>());
        ScannerSubscription subscription = new ScannerSubscription();
        subscription.instrument("STK");
        subscription.locationCode("STK.US");
        subscription.scanCode(scanCode);
        subscription.abovePrice(minPrice);
        subscription.belowPrice(maxPrice);
        subscription.aboveVolume(minVolume);

        int scannerTickerId = ib.getNextTickerId();
        CompletableFuture<Void> scannerFuture = new CompletableFuture<>();

        ib.startScannerListener(scannerTickerId, scanData -> scannedSymbols.add(scanData.contract().symbol()),
                () -> scannerFuture.complete(null));

        try {
            ib.getEClient().reqScannerSubscription(scannerTickerId, subscription, null, null);
            scannerFuture.get(timeout, TimeUnit.MILLISECONDS);
        } finally {
            ib.getEClient().cancelScannerSubscription(scannerTickerId);
        }

        return scannedSymbols;
    }
}