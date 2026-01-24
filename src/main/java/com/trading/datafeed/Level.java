package com.trading.datafeed;

public class Level {
    private double price;
    private long size;

    public Level(double price, long size) {
        this.price = price;
        this.size = size;
    }

    public double getPrice() {
        return price;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}