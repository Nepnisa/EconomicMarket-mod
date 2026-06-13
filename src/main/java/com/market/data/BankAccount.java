package com.market.data;

public class BankAccount {
    public int balance;
    public long lastInterestTime;

    public BankAccount(int balance, long lastInterestTime) {
        this.balance = balance;
        this.lastInterestTime = lastInterestTime;
    }
}
