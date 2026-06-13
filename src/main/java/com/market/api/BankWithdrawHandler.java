package com.market.api;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.market.MarketMod;
import com.market.data.BankAccount;
import com.market.data.BankStorage;
import com.market.data.CurrencyUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BankWithdrawHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        JsonObject json = JsonParser.parseString(body).getAsJsonObject();
        UUID uuid = UUID.fromString(json.get("playerUuid").getAsString());
        int amount = json.get("amount").getAsInt();

        if (amount <= 0) { sendError(exchange, "Amount must be positive"); return; }

        MarketMod.server.execute(() -> {
            ServerPlayerEntity player = MarketMod.server.getPlayerManager().getPlayer(uuid);
            if (player == null) {
                try { sendError(exchange, "Player offline"); } catch (IOException ignored) {}
                return;
            }
            BankAccount acc = BankStorage.getAccount(uuid);
            if (acc.balance < amount) {
                try { sendError(exchange, "Bank balance insufficient"); } catch (IOException ignored) {}
                return;
            }
            acc.balance -= amount;
            BankStorage.updateAccount(uuid, acc);
            CurrencyUtils.giveEmeralds(player, amount);
            try { sendJson(exchange, "{\"success\":true}"); } catch (IOException ignored) {}
        });
    }

    private void sendJson(HttpExchange e, String s) throws IOException {
        byte[] d = s.getBytes(StandardCharsets.UTF_8);
        e.getResponseHeaders().set("Content-Type", "application/json");
        e.sendResponseHeaders(200, d.length);
        e.getResponseBody().write(d); e.close();
    }
    private void sendError(HttpExchange e, String m) throws IOException {
        sendJson(e, "{\"error\":\"" + m + "\"}");
    }
}
