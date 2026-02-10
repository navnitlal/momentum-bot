package com.trading.ib;

public interface AccountListener {
    void onAccountUpdate(double availableFunds);
}
