package com.market.api;

import com.google.gson.JsonArray;
import com.market.data.PlayerStocks;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StockHoldingsHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String q = exchange.getRequestURI().getQuery();
        UUID uuid = UUID.fromString(q.substring(q.indexOf("uuid=") + 5));
        Map<UUID, Map<String, PlayerStocks.Holding>> all = PlayerStocks.loadAll();
        Map<String, PlayerStocks.Holding> holds = all.getOrDefault(uuid, new HashMap<>());
        JsonArray arr = new JsonArray();
        for (String code : holds.keySet()) {
            arr.add(code);
        }
        String resp = "{\"stocks\":" + arr.toString() + "}";
        byte[] data = resp.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, data.length);
        exchange.getResponseBody().write(data);
        exchange.close();
    }
}
