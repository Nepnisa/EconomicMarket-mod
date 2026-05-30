package com.market.api;

import com.google.gson.JsonObject;
import com.market.MarketMod;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class AuthHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        String query = exchange.getRequestURI().getQuery();
        if (query == null || !query.contains("name=")) {
            String resp = "{\"online\":false,\"error\":\"missing name\"}";
            byte[] respBytes = resp.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, respBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(respBytes); os.close();
            return;
        }
        String name = query.substring(query.indexOf("name=")+5);
        ServerPlayerEntity player = MarketMod.server.getPlayerManager().getPlayer(name);
        JsonObject json = new JsonObject();
        if (player != null) {
            json.addProperty("online", true);
            json.addProperty("uuid", player.getUuidAsString());
            json.addProperty("name", player.getName().getString());
        } else {
            json.addProperty("online", false);
        }
        byte[] resp = json.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp); os.close();
    }
}