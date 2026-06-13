package com.market.client;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

public class MarketBrowserScreen extends Screen {
    private static final String BASE_URL = "http://localhost:5888";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private int tab = 0;
    private final String playerUuid;
    private final String playerName;

    private List<MarketItem> marketItems = new ArrayList<>();
    private List<AuctionItem> auctions = new ArrayList<>();
    private List<StockItem> stocks = new ArrayList<>();
    private int balance = 0;
    private int bankBalance = 0;
    private List<LeaderboardEntry> leaderboard = new ArrayList<>();
    private TextFieldWidget bankAmountInput;

    public MarketBrowserScreen() {
        super(Text.literal("EconomicMarket"));
        var player = MinecraftClient.getInstance().player;
        if (player != null) {
            playerName = player.getName().getString();
            playerUuid = player.getUuidAsString();
        } else {
            playerName = "";
            playerUuid = "";
        }
    }

    @Override
    protected void init() {
        String[] tabs = {"市场", "拍卖", "股票", "钱包", "银行", "排行榜"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), btn -> {
                this.tab = idx;
                refreshData();
            }).dimensions(10 + i * 60, 10, 55, 20).build());
        }
        bankAmountInput = new TextFieldWidget(textRenderer, width / 2 - 50, 60, 100, 20, Text.literal(""));
        addSelectableChild(bankAmountInput);
        refreshData();
    }

    private void refreshData() {
        new Thread(() -> {
            try {
                switch (tab) {
                    case 0 -> marketItems = fetchList("/api/market/list", MarketItem.class);
                    case 1 -> auctions = fetchList("/api/auction/list", AuctionItem.class);
                    case 2 -> stocks = fetchList("/api/stock/list", StockItem.class);
                    case 3 -> {
                        var resp = httpClient.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/wallet/balance?uuid=" + playerUuid)).GET().build(), HttpResponse.BodyHandlers.ofString());
                        JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
                        balance = obj.get("balance").getAsInt();
                    }
                    case 4 -> {
                        var resp = httpClient.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/bank/balance?uuid=" + playerUuid)).GET().build(), HttpResponse.BodyHandlers.ofString());
                        JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
                        bankBalance = obj.get("balance").getAsInt();
                    }
                    case 5 -> {
                        var resp = httpClient.send(HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/leaderboard")).GET().build(), HttpResponse.BodyHandlers.ofString());
                        leaderboard = gson.fromJson(resp.body(), new TypeToken<List<LeaderboardEntry>>(){}.getType());
                    }
                }
            } catch (Exception ignored) {}
        }).start();
    }

    private <T> List<T> fetchList(String path, Class<T> clazz) throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + path)).GET().build();
        var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(resp.body(), TypeToken.getParameterized(List.class, clazz).getType());
    }

    private void sendBankAction(String action) {
        try {
            int amount = Integer.parseInt(bankAmountInput.getText());
            if (amount <= 0) return;
            JsonObject json = new JsonObject();
            json.addProperty("playerUuid", playerUuid);
            json.addProperty("amount", amount);
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BASE_URL + "/api/bank/" + action))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                .build();
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            refreshData();
        } catch (Exception ignored) {}
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int y = 40;
        if (tab == 0) {
            for (MarketItem item : marketItems) {
                context.drawTextWithShadow(textRenderer, item.name + " - " + item.price + " E", 15, y, 0xAAAAAA);
                y += 12;
            }
        } else if (tab == 1) {
            for (AuctionItem a : auctions) {
                context.drawTextWithShadow(textRenderer, a.name + " 当前出价:" + a.currentBid, 15, y, 0xAAAAAA);
                y += 12;
            }
        } else if (tab == 2) {
            for (StockItem s : stocks) {
                int color = s.change >= 0 ? 0x55FF55 : 0xFF5555;
                context.drawTextWithShadow(textRenderer, s.name + " " + s.price + " E (" + String.format("%.2f", s.changePercent) + "%)", 15, y, color);
                y += 12;
            }
        } else if (tab == 3) {
            context.drawTextWithShadow(textRenderer, "你的余额: " + balance + " 绿宝石", 10, y, 0xFFAA00);
        } else if (tab == 4) {
            context.drawTextWithShadow(textRenderer, "银行余额: " + bankBalance + " 绿宝石", 10, y, 0xFFAA00);
            y += 20;
            context.drawTextWithShadow(textRenderer, "金额:", 10, y, 0xAAAAAA);
            bankAmountInput.setX(50);
            bankAmountInput.setY(y - 5);
            bankAmountInput.render(context, mouseX, mouseY, delta);
            addDrawableChild(ButtonWidget.builder(Text.literal("存款"), btn -> sendBankAction("deposit")).dimensions(160, y - 5, 40, 20).build());
            addDrawableChild(ButtonWidget.builder(Text.literal("取款"), btn -> sendBankAction("withdraw")).dimensions(205, y - 5, 40, 20).build());
        } else if (tab == 5) {
            for (LeaderboardEntry e : leaderboard) {
                context.drawTextWithShadow(textRenderer, e.name + " - " + e.balance + " E", 15, y, 0xFFFF00);
                y += 12;
            }
        }
    }

    static class MarketItem { String id, name, seller; int price; }
    static class AuctionItem { String id, name; int currentBid; }
    static class StockItem { String code, name; int price, change; double changePercent; }
    static class LeaderboardEntry { String name; int balance; }
}
