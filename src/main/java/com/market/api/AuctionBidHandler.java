package com.market.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.MarketMod;
import com.market.data.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class AuctionBidHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        try {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            UUID playerUuid = UUID.fromString(json.get("playerUuid").getAsString());
            String auctionId = json.get("auctionId").getAsString();
            int amount = json.get("amount").getAsInt();

            AuctionItem a = AuctionManager.auctions.stream().filter(x -> x.id.toString().equals(auctionId)).findFirst().orElse(null);
            if (a == null || a.isExpired()) { sendError(exchange, "Auction not found"); return; }
            int min = a.currentBid == 0 ? a.startingPrice : a.currentBid + a.minBidIncrement;
            if (amount < min) { sendError(exchange, "Bid too low"); return; }

            MarketMod.server.execute(() -> {
                ServerPlayerEntity bidder = MarketMod.server.getPlayerManager().getPlayer(playerUuid);
                if (bidder == null) {
                    try { sendError(exchange, "Player offline"); } catch (IOException ignored) {}
                    return;
                }
                if (!CurrencyUtils.deductEmeralds(bidder, amount)) {
                    try { sendError(exchange, "Not enough emeralds"); } catch (IOException ignored) {}
                    return;
                }

                if (a.currentBidderUuid != null && a.currentBid > 0) {
                    ServerPlayerEntity prev = MarketMod.server.getPlayerManager().getPlayer(a.currentBidderUuid);
                    if (prev != null) {
                        CurrencyUtils.giveEmeralds(prev, a.currentBid);
                        prev.sendMessage(Text.literal("You have been outbid, refunded " + a.currentBid), false);
                    }
                }

                a.currentBid = amount;
                a.currentBidderUuid = playerUuid;
                a.currentBidderName = bidder.getName().getString();
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