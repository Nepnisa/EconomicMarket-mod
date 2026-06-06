package com.market.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.MarketMod;
import com.market.data.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import java.io.*;
import java.net.InetAddress;
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
            boolean enableSkyLantern = json.has("enableSkyLantern") && json.get("enableSkyLantern").getAsBoolean();
            int timeIncrement = json.has("timeIncrement") ? json.get("timeIncrement").getAsInt() : 0;

            // 限制参数范围，并保存为 final 变量供 lambda 使用
            if (durationMin > 10) durationMin = 10;
            if (durationMin < 1) durationMin = 1;
            if (timeIncrement > 20) timeIncrement = 20;
            if (timeIncrement < 0) timeIncrement = 0;

            final int finalDuration = durationMin;
            final int finalTimeIncrement = timeIncrement;

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
                a.endTime = System.currentTimeMillis() + finalDuration * 60000L;
                a.currentBid = 0;
                a.enableSkyLantern = enableSkyLantern;
                a.timeIncrement = finalTimeIncrement;
                AuctionManager.auctions.add(a);
                AuctionManager.save();

                // 获取本机IP
                String ip = "localhost";
                try {
                    ip = InetAddress.getLocalHost().getHostAddress();
                } catch (Exception e) {}

                // 广播消息
                String msg = String.format(
                    "玩家“%s”发起了一场拍卖！物品：%s | 起拍价：%d 绿宝石 | 时长：%d 分钟 | 每次加价延时：%d 秒 | %s点天灯 | 参与地址：http://%s:5888",
                    player.getName().getString(),
                    toSell.getName().getString(),
                    startPrice,
                    finalDuration,
                    finalTimeIncrement,
                    enableSkyLantern ? "已开启" : "未开启",
                    ip
                );
                MarketMod.server.getPlayerManager().broadcast(Text.literal(msg), false);

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
