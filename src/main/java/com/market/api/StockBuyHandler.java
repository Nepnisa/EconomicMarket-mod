package com.market.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.MarketMod;
import com.market.data.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class StockBuyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            UUID uuid = UUID.fromString(json.get("playerUuid").getAsString());
            String code = json.get("code").getAsString();
            int qty = json.get("qty").getAsInt();

            StockItem stock = StockMarket.getByCode(code);
            if (stock == null) { sendError(exchange, "Stock not found"); return; }
            int cost = stock.price * qty;

            MarketMod.server.execute(() -> {
                ServerPlayerEntity player = MarketMod.server.getPlayerManager().getPlayer(uuid);
                if (player == null) {
                    try { sendError(exchange, "Player offline"); } catch (IOException ignored) {}
                    return;
                }
                if (!CurrencyUtils.deductEmeralds(player, cost)) {
                    try { sendError(exchange, "Not enough emeralds"); } catch (IOException ignored) {}
                    return;
                }

                Map<UUID, Map<String, PlayerStocks.Holding>> all = PlayerStocks.loadAll();
                Map<String, PlayerStocks.Holding> holdings = all.computeIfAbsent(uuid, k -> new HashMap<>());
                PlayerStocks.Holding old = holdings.getOrDefault(code, new PlayerStocks.Holding(0,0));
                int total = old.qty + qty;
                int avg = (old.qty * old.avg + cost) / total;
                holdings.put(code, new PlayerStocks.Holding(total, avg));
                PlayerStocks.saveAll(all);
                try { sendJson(exchange, "{\"success\":true}"); } catch (IOException ignored) {}
            });
        } catch (Exception e) {
            sendError(exchange, "Invalid request");
        }
    }

    private void sendJson(HttpExchange exchange, String json) throws IOException {
        byte[] resp = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp); os.close();
    }
    private void sendError(HttpExchange exchange, String msg) throws IOException {
        sendJson(exchange, "{\"error\":\"" + msg + "\"}");
    }
}