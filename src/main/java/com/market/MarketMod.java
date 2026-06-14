package com.market;

import com.market.api.*;
import com.market.data.*;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.sun.net.httpserver.HttpServer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.server.command.CommandManager;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;

public class MarketMod implements ModInitializer {
    public static MinecraftServer server;
    private static HttpServer httpServer;
    private static int currentPort = 5888;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s;
            startHttpServer(currentPort);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("EMet")
                .then(CommandManager.literal("port")
                    .then(CommandManager.argument("port", IntegerArgumentType.integer(1, 65535))
                    .executes(context -> {
                        int newPort = IntegerArgumentType.getInteger(context, "port");
                        if (restartHttpServer(newPort)) {
                            context.getSource().sendFeedback(
                                () -> Text.literal("Market port changed to " + newPort), true);
                            return 1;
                        } else {
                            context.getSource().sendError(
                                Text.literal("Failed to bind port " + newPort + ". Is it already in use?"));
                            return 0;
                        }
                    }))
                )
                .then(CommandManager.literal("emerald")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player != null) {
                            int balance = CurrencyUtils.getEmeraldCount(player);
                            player.sendMessage(Text.literal("Backpack emeralds: " + balance), false);
                            return 1;
                        }
                        return 0;
                    }))
                .then(CommandManager.literal("bankin")
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        if (player != null) {
                            if (!CurrencyUtils.deductEmeralds(player, amount)) {
                                player.sendMessage(Text.literal("Not enough emeralds"), false);
                                return 0;
                            }
                            BankAccount acc = BankStorage.getAccount(player.getUuid());
                            acc.balance += amount;
                            BankStorage.updateAccount(player.getUuid(), acc);
                            player.sendMessage(Text.literal("Deposited " + amount + " emeralds"), false);
                            return 1;
                        }
                        return 0;
                    })))
                .then(CommandManager.literal("bankout")
                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        int amount = IntegerArgumentType.getInteger(ctx, "amount");
                        if (player != null) {
                            BankAccount acc = BankStorage.getAccount(player.getUuid());
                            if (acc.balance < amount) {
                                player.sendMessage(Text.literal("Bank balance insufficient"), false);
                                return 0;
                            }
                            acc.balance -= amount;
                            BankStorage.updateAccount(player.getUuid(), acc);
                            CurrencyUtils.giveEmeralds(player, amount);
                            player.sendMessage(Text.literal("Withdrew " + amount + " emeralds"), false);
                            return 1;
                        }
                        return 0;
                    })))
                .then(CommandManager.literal("bankmoney")
                    .executes(ctx -> {
                        ServerPlayerEntity player = ctx.getSource().getPlayer();
                        if (player != null) {
                            BankAccount acc = BankStorage.getAccount(player.getUuid());
                            player.sendMessage(Text.literal("Bank balance: " + acc.balance + " emeralds"), false);
                            return 1;
                        }
                        return 0;
                    }))
                .then(CommandManager.literal("help")
                    .executes(ctx -> {
                        ctx.getSource().sendFeedback(() -> Text.literal(
                            "=== EconomicMarket Commands ===\n" +
                            "/EMet port <port> - Change web panel port\n" +
                            "/EMet emerald - Check backpack emeralds\n" +
                            "/EMet bankin <amount> - Deposit emeralds\n" +
                            "/EMet bankout <amount> - Withdraw emeralds\n" +
                            "/EMet bankmoney - Check bank balance\n" +
                            "/EMet browser - Open in-game GUI\n" +
                            "/EMet help - Show this help"
                        ), false);
                        return 1;
                    }))
            );
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String ip = "localhost";
            try { ip = java.net.InetAddress.getLocalHost().getHostAddress(); } catch (Exception ignored) {}
            player.sendMessage(Text.literal(
                "Welcome! Market at http://" + ip + ":" + currentPort +
                " or /EMet browser. /EMet help for commands."), false);
        });

        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void startHttpServer(int port) {
        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
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
            httpServer.createContext("/api/bank/balance", new BankBalanceHandler());
            httpServer.createContext("/api/bank/deposit", new BankDepositHandler());
            httpServer.createContext("/api/bank/withdraw", new BankWithdrawHandler());
            httpServer.start();
            currentPort = port;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean restartHttpServer(int port) {
        if (httpServer != null) httpServer.stop(0);
        try {
            startHttpServer(port);
            return true;
        } catch (Exception e) {
            startHttpServer(currentPort);
            return false;
        }
    }

    private void tick(MinecraftServer server) {
        if (server.getTicks() % 1200 == 0) {
            for (var player : server.getPlayerManager().getPlayerList()) {
                CurrencyUtils.giveEmeralds(player, 1);
            }
            Map<UUID, BankAccount> accounts = BankStorage.loadAll();
            boolean changed = false;
            for (BankAccount acc : accounts.values()) {
                if (acc.balance > 0) {
                    int interest = (int)(acc.balance * 0.01);
                    if (interest > 0) {
                        acc.balance += interest;
                        acc.lastInterestTime = System.currentTimeMillis();
                        changed = true;
                    }
                }
            }
            if (changed) BankStorage.saveAll(accounts);
        }
        if (server.getTicks() % 100 == 0) AuctionManager.checkExpiredAuctions(server);
        if (server.getTicks() % 1200 == 0) StockMarket.tickPrices();
    }
}
