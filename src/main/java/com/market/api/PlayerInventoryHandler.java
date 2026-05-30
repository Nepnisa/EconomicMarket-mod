package com.market.api;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.market.MarketMod;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class PlayerInventoryHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("uuid=")) {
            exchange.sendResponseHeaders(400, -1);
            return;
        }
        String uuidStr = query.substring(query.indexOf("uuid=")+5);
        UUID uuid = UUID.fromString(uuidStr);
        ServerPlayerEntity player = MarketMod.server.getPlayerManager().getPlayer(uuid);
        if (player == null) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        JsonArray arr = new JsonArray();
        for (int i = 0; i < 36; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("slot", i);
                obj.addProperty("name", stack.getItem().getName().getString());
                obj.addProperty("count", stack.getCount());
                arr.add(obj);
            }
        }

        byte[] resp = arr.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp); os.close();
    }
}