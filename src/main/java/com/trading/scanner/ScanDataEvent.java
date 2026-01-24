package com.trading.scanner;

import com.ib.client.Contract;

public record ScanDataEvent(Contract contract, int rank, String distance, String benchmark, String projection, String legsStr) { }