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
            if (a == null || a.isExpired()) { sendError(exchange, "Auction not found or expired"); return; }
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

                // 退还前一位出价者
                if (a.currentBidderUuid != null && a.currentBid > 0) {
                    ServerPlayerEntity prev = MarketMod.server.getPlayerManager().getPlayer(a.currentBidderUuid);
                    if (prev != null) {
                        CurrencyUtils.giveEmeralds(prev, a.currentBid);
                        prev.sendMessage(Text.literal("你的出价已被超越，已退还 " + a.currentBid + " 绿宝石"), false);
                    }
                }

                // 记录出价历史
                a.bidHistory.add(new BidRecord(bidder.getName().getString(), amount, System.currentTimeMillis()));

                a.currentBid = amount;
                a.currentBidderUuid = playerUuid;
                a.currentBidderName = bidder.getName().getString();

                // 加价延时逻辑（timeIncrement > 0，且剩余时间小于等于20秒时生效）
                long remain = a.endTime - System.currentTimeMillis();
                if (a.timeIncrement > 0 && remain <= 20000) {
                    a.endTime += a.timeIncrement * 1000L;
                    if (a.endTime > System.currentTimeMillis() + 120000) // 最多延长2分钟
                        a.endTime = System.currentTimeMillis() + 120000;
                    bidder.sendMessage(Text.literal("出价成功，拍卖时间延长 " + a.timeIncrement + " 秒"), false);
                }

                // 点天灯处理（如果开启且当前不是点天灯玩家，则点天灯玩家自动加价）
                if (a.enableSkyLantern && a.skyLanternUuid != null && !a.skyLanternUuid.equals(playerUuid)) {
                    // 触发自动加价逻辑，可在下次tick或立即执行
                    applySkyLantern(a, bidder.getServer());
                }

                AuctionManager.save();
                try { sendJson(exchange, "{\"success\":true}"); } catch (IOException ignored) {}
            });
        } catch (Exception e) {
            sendError(exchange, "Invalid request");
        }
    }

    private void applySkyLantern(AuctionItem a, net.minecraft.server.MinecraftServer server) {
        if (a.skyLanternUuid == null || a.isExpired()) return;
        ServerPlayerEntity lantern = server.getPlayerManager().getPlayer(a.skyLanternUuid);
        if (lantern == null) return;
        if (lantern.getUuid().equals(a.currentBidderUuid)) return; // 已经是最高出价者
        int newBid = a.currentBid + a.minBidIncrement;
        if (!CurrencyUtils.deductEmeralds(lantern, newBid)) {
            lantern.sendMessage(Text.literal("点天灯失败，绿宝石不足"), false);
            a.skyLanternUuid = null;
            return;
        }
        // 退还前一位
        if (a.currentBidderUuid != null && a.currentBid > 0) {
            ServerPlayerEntity prev = server.getPlayerManager().getPlayer(a.currentBidderUuid);
            if (prev != null) CurrencyUtils.giveEmeralds(prev, a.currentBid);
        }
        a.currentBid = newBid;
        a.currentBidderUuid = lantern.getUuid();
        a.currentBidderName = lantern.getName().getString();
        a.bidHistory.add(new BidRecord(lantern.getName().getString() + "(点天灯)", newBid, System.currentTimeMillis()));
        AuctionManager.save();
        lantern.sendMessage(Text.literal("点天灯自动出价：" + newBid + " 绿宝石"), false);
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
