package com.market.data;

public class StockItem {
    public String code;
    public String name;
    public int price;
    public int change;
    public double changePercent;

    public StockItem() {}
    public StockItem(String code, String name, int price) { this.code = code; this.name = name; this.price = price; }
    public String toJson() {
        return String.format("{\"code\":\"%s\",\"name\":\"%s\",\"price\":%d,\"change\":%d,\"changePercent\":%.2f}",
                code, name, price, change, changePercent);
    }
}