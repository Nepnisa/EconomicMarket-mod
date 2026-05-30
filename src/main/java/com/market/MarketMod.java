package com.market;

import com.market.api.*;
import com.market.data.*;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import java.io.IOException;
import java.net.InetSocketAddress;

public class MarketMod implements ModInitializer {
    public static MinecraftServer server;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            try {
                HttpServer httpServer = HttpServer.create(new InetSocketAddress(5888), 0);
                httpServer.createContext("/", exchange -> {
                    exchange.getResponseHeaders().set("Location", "/web/index.html");
                    exchange.sendResponseHeaders(302, -1);
                    exchange.close();
                });
                httpServer.createContext("/web/", new StaticFileHandler());
                httpServer.createContext("/api/auth", new AuthHandler());
                httpServer.createContext("/api/market/list", new MarketListHandler());
                httpServer.createContext("/api/market/buy", new MarketBuyHandler());
                httpServer.createContext("/api/market/sell", new MarketSellHandler());
                httpServer.createContext("/api/auction/list", new AuctionListHandler());
                httpServer.createContext("/api/auction/bid", new AuctionBidHandler());
                httpServer.createContext("/api/auction/sell", new AuctionSellHandler());
                httpServer.createContext("/api/stock/list", new StockListHandler());
                httpServer.createContext("/api/stock/buy", new StockBuyHandler());
                httpServer.createContext("/api/stock/sell", new StockSellHandler());
                httpServer.createContext("/api/wallet/balance", new WalletBalanceHandler());
                httpServer.createContext("/api/leaderboard", new LeaderboardHandler());
                httpServer.createContext("/api/player/inventory", new PlayerInventoryHandler());
                httpServer.start();
            } catch (IOException e) { e.printStackTrace(); }
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        if (server.getTicks() % 1200 == 0) { // 1 min
            for (var player : server.getPlayerManager().getPlayerList()) {
                CurrencyUtils.giveEmeralds(player, 1);
            }
        }
        if (server.getTicks() % 100 == 0) { // 5 sec
            AuctionManager.checkExpiredAuctions(server);
        }
        if (server.getTicks() % 1200 == 0) {
            StockMarket.tickPrices();
        }
    }
}