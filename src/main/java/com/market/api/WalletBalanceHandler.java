package com.market.api;

import com.market.MarketMod;
import com.market.data.CurrencyUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class WalletBalanceHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("uuid=")) {
            sendJson(exchange, "{\"balance\":0}");
            return;
        }
        String uuidStr = query.substring(query.indexOf("uuid=")+5);
        UUID uuid = UUID.fromString(uuidStr);
        var player = MarketMod.server.getPlayerManager().getPlayer(uuid);
        int balance = player != null ? CurrencyUtils.getEmeraldCount(player) : 0;
        sendJson(exchange, "{\"balance\":" + balance + "}");
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp); os.close();
    }
}