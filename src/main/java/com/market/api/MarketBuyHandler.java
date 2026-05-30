package com.market.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.MarketMod;
import com.market.data.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MarketBuyHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());
            String itemId = json.get("itemId").getAsString();

            var items = MarketStorage.load();
            MarketItem target = items.stream().filter(i -> i.id.toString().equals(itemId)).findFirst().orElse(null);
            if (target == null) { sendError(exchange, "Item not found"); return; }

            MarketMod.server.execute(() -> {
                ServerPlayerEntity buyer = MarketMod.server.getPlayerManager().getPlayer(playerUuid);
                if (buyer == null) {
                    try { sendError(exchange, "Player offline"); } catch (IOException ignored) {}
                    return;
                }
                if (!CurrencyUtils.deductEmeralds(buyer, target.price)) {
                    try { sendError(exchange, "Not enough emeralds"); } catch (IOException ignored) {}
                    return;
                }

                ServerPlayerEntity seller = MarketMod.server.getPlayerManager().getPlayer(target.sellerUuid);
                if (seller != null) {
                    CurrencyUtils.giveEmeralds(seller, target.price);
                }

                ItemStack boughtItem = target.item.copy();
                if (!buyer.getInventory().insertStack(boughtItem)) {
                    buyer.dropItem(boughtItem, false);
                }
                buyer.getInventory().markDirty();

                items.remove(target);
                MarketStorage.save(items);
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