package com.market.api;

import com.market.data.BankStorage;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BankBalanceHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        String q = exchange.getRequestURI().getQuery();
        UUID uuid = UUID.fromString(q.substring(q.indexOf("uuid=")+5));
        int balance = BankStorage.getAccount(uuid).balance;
        String resp = "{\"balance\":" + balance + "}";
        byte[] data = resp.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
