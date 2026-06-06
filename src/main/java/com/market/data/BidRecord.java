package com.market.data;

public class BidRecord {
    public String playerName;
    public int amount;
    public long time;

    public BidRecord() {}
    public BidRecord(String playerName, int amount, long time) {
        this.playerName = playerName;
        this.amount = amount;
        this.time = time;
    }

    public String toJson() {
        return String.format("{\"player\":\"%s\",\"amount\":%d,\"time\":%d}", playerName, amount, time);
    }
}
