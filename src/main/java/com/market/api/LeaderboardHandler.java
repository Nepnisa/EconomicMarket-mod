package com.market.api;

import com.market.MarketMod;
import com.market.data.CurrencyUtils;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class LeaderboardHandler implements HttpHandler {
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { exchange.sendResponseHeaders(405, -1); return; }
        List<Map.Entry<String, Integer>> list = new ArrayList<>();
        for (ServerPlayerEntity player : MarketMod.server.getPlayerManager().getPlayerList()) {
            list.add(new AbstractMap.SimpleEntry<>(player.getName().getString(), CurrencyUtils.getEmeraldCount(player)));
        }
        list.sort((a,b) -> b.getValue() - a.getValue());
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < Math.min(10, list.size()); i++) {
            var e = list.get(i);
            sb.append("{\"name\":\"").append(e.getKey()).append("\",\"balance\":").append(e.getValue()).append("}");
            if (i < Math.min(10, list.size()) - 1) sb.append(",");
        }
        sb.append("]");
        byte[] resp = sb.toString().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, resp.length);
        OutputStream os = exchange.getResponseBody();
        os.write(resp); os.close();
    }
}