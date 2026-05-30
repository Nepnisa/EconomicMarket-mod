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

public class AuctionSellHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());
            int slot = json.get("slot").getAsInt();
            int startPrice = json.get("startPrice").getAsInt();
            int minInc = json.get("minInc").getAsInt();
            int durationMin = json.get("duration").getAsInt();

            MarketMod.server.execute(() -> {
                ServerPlayerEntity player = MarketMod.server.getPlayerManager().getPlayer(playerUuid);
                if (player == null) {
                    try { sendError(exchange, "Player offline"); } catch (IOException ignored) {}
                    return;
                }
                ItemStack stack = player.getInventory().getStack(slot);
                if (stack.isEmpty()) {
                    try { sendError(exchange, "Empty slot"); } catch (IOException ignored) {}
                    return;
                }
                ItemStack toSell = stack.copy();
                player.getInventory().removeStack(slot);
                player.getInventory().markDirty();

                AuctionItem a = new AuctionItem();
                a.id = UUID.randomUUID();
                a.sellerUuid = playerUuid;
                a.sellerName = player.getName().getString();
                a.item = toSell;
                a.startingPrice = startPrice;
                a.minBidIncrement = minInc;
                a.endTime = System.currentTimeMillis() + durationMin * 60000L;
                a.currentBid = 0;
                AuctionManager.auctions.add(a);
                AuctionManager.save();
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