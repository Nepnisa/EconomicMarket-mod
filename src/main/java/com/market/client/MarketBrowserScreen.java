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
import java.util.concurrent.*;

public class MarketBrowserScreen extends Screen {
    private static final String BASE_URL = "http://localhost:5888";
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private int tab = 0;
    private final String playerUuid;
    private final String playerName;

    // 页面数据
    private List<MarketItem> marketItems = new ArrayList<>();
    private List<AuctionItem> auctions = new ArrayList<>();
    private List<StockItem> stocks = new ArrayList<>();
    private int balance = 0;
    private int bankBalance = 0;
    private List<LeaderboardEntry> leaderboard = new ArrayList<>();
    private List<InventoryEntry> inventory = new ArrayList<>();
    private Set<String> ownedStocks = new HashSet<>();

    // 输入框
    private TextFieldWidget bankAmountInput;
    private TextFieldWidget sellSlotInput, sellPriceInput;
    private TextFieldWidget bidAmountInput;
    private TextFieldWidget stockBuyQtyInput;

    // 股票自动刷新
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> stockRefreshFuture;

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
        clearChildren();
        String[] tabs = {"市场", "拍卖", "股票", "钱包", "银行", "排行榜"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), btn -> switchTab(idx))
                .dimensions(10 + i * 65, 10, 60, 20).build());
        }
        bankAmountInput = new TextFieldWidget(textRenderer, 0, 0, 80, 20, Text.literal(""));
        sellSlotInput = new TextFieldWidget(textRenderer, 0, 0, 40, 20, Text.literal(""));
        sellPriceInput = new TextFieldWidget(textRenderer, 0, 0, 60, 20, Text.literal(""));
        bidAmountInput = new TextFieldWidget(textRenderer, 0, 0, 60, 20, Text.literal(""));
        stockBuyQtyInput = new TextFieldWidget(textRenderer, 0, 0, 40, 20, Text.literal(""));
        switchTab(0);
    }

    private void switchTab(int index) {
        this.tab = index;
        clearChildren(); // 清除所有旧组件
        String[] tabs = {"市场", "拍卖", "股票", "钱包", "银行", "排行榜"};
        for (int i = 0; i < tabs.length; i++) {
            final int idx = i;
            addDrawableChild(ButtonWidget.builder(Text.literal(tabs[i]), btn -> switchTab(idx))
                .dimensions(10 + i * 65, 10, 60, 20).build());
        }
        if (stockRefreshFuture != null) stockRefreshFuture.cancel(false);
        refreshData();
    }

    private void refreshData() {
        switch (tab) {
            case 0 -> loadMarketAndInventory();
            case 1 -> loadAuctions();
            case 2 -> startStockAutoRefresh();
            case 3 -> loadWalletBalance();
            case 4 -> loadBankBalance();
            case 5 -> loadLeaderboard();
        }
    }

    // ---------- 数据加载 ----------
    private void loadMarketAndInventory() {
        new Thread(() -> {
            try {
                marketItems = fetchList("/api/market/list", MarketItem.class);
                var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/player/inventory?uuid=" + playerUuid)).GET().build();
                var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                inventory = gson.fromJson(resp.body(), new TypeToken<List<InventoryEntry>>(){}.getType());
                if (inventory == null) inventory = new ArrayList<>();
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadAuctions() {
        new Thread(() -> {
            try { auctions = fetchList("/api/auction/list", AuctionItem.class); } catch (Exception ignored) {}
        }).start();
    }

    private void loadWalletBalance() {
        new Thread(() -> {
            try {
                var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/wallet/balance?uuid=" + playerUuid)).GET().build();
                var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
                balance = obj.get("balance").getAsInt();
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadBankBalance() {
        new Thread(() -> {
            try {
                var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/bank/balance?uuid=" + playerUuid)).GET().build();
                var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
                bankBalance = obj.get("balance").getAsInt();
            } catch (Exception ignored) {}
        }).start();
    }

    private void loadLeaderboard() {
        new Thread(() -> {
            try {
                var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/leaderboard")).GET().build();
                var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                leaderboard = gson.fromJson(resp.body(), new TypeToken<List<LeaderboardEntry>>(){}.getType());
            } catch (Exception ignored) {}
        }).start();
    }

    private void startStockAutoRefresh() {
        loadStocks();
        if (scheduler == null) scheduler = Executors.newScheduledThreadPool(1);
        stockRefreshFuture = scheduler.scheduleAtFixedRate(this::loadStocks, 10, 10, TimeUnit.SECONDS);
    }

    private void loadStocks() {
        new Thread(() -> {
            try {
                stocks = fetchList("/api/stock/list", StockItem.class);
                // 获取持仓信息（需要后端支持该接口，如果后端没有，可以暂时留空）
                // 这里假设有一个 /api/stock/holdings?uuid=xxx 接口，如果没有，请先用空集合
                // 临时方案：从已实现的 PlayerStocks 获取，但这里为了不阻塞，先用空
                ownedStocks.clear();
                // 如果你已经实现了该接口，取消下面注释：
                // var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + "/api/stock/holdings?uuid=" + playerUuid)).GET().build();
                // var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                // JsonObject obj = gson.fromJson(resp.body(), JsonObject.class);
                // if (obj.has("stocks")) {
                //     JsonArray arr = obj.getAsJsonArray("stocks");
                //     for (JsonElement e : arr) ownedStocks.add(e.getAsJsonObject().get("code").getAsString());
                // }
            } catch (Exception ignored) {}
        }).start();
    }

    private <T> List<T> fetchList(String path, Class<T> clazz) throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create(BASE_URL + path)).GET().build();
        var resp = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        return gson.fromJson(resp.body(), TypeToken.getParameterized(List.class, clazz).getType());
    }

    // ---------- 操作方法 ----------
    private void buyMarketItem(String itemId) {
        new Thread(() -> {
            try {
                JsonObject json = new JsonObject();
                json.addProperty("playerUuid", playerUuid);
                json.addProperty("itemId", itemId);
                sendPost("/api/market/buy", json.toString());
                loadMarketAndInventory();
            } catch (Exception ignored) {}
        }).start();
    }

    private void sellMarketItem() {
        try {
            int slot = Integer.parseInt(sellSlotInput.getText());
            int price = Integer.parseInt(sellPriceInput.getText());
            if (slot < 0 || slot > 35 || price <= 0) return;
            JsonObject json = new JsonObject();
            json.addProperty("playerUuid", playerUuid);
            json.addProperty("slot", slot);
            json.addProperty("price", price);
            sendPost("/api/market/sell", json.toString());
            loadMarketAndInventory();
        } catch (Exception ignored) {}
    }

    private void bidAuction(String auctionId) {
        try {
            int amount = Integer.parseInt(bidAmountInput.getText());
            if (amount <= 0) return;
            JsonObject json = new JsonObject();
            json.addProperty("playerUuid", playerUuid);
            json.addProperty("auctionId", auctionId);
            json.addProperty("amount", amount);
            sendPost("/api/auction/bid", json.toString());
            loadAuctions();
        } catch (Exception ignored) {}
    }

    private void buyStock(String code) {
        try {
            int qty = stockBuyQtyInput.getText().isEmpty() ? 1 : Integer.parseInt(stockBuyQtyInput.getText());
            JsonObject json = new JsonObject();
            json.addProperty("playerUuid", playerUuid);
            json.addProperty("code", code);
            json.addProperty("qty", qty);
            sendPost("/api/stock/buy", json.toString());
            loadStocks();
        } catch (Exception ignored) {}
    }

    private void sellStock(String code) {
        try {
            int qty = stockBuyQtyInput.getText().isEmpty() ? 1 : Integer.parseInt(stockBuyQtyInput.getText());
            JsonObject json = new JsonObject();
            json.addProperty("playerUuid", playerUuid);
            json.addProperty("code", code);
            json.addProperty("qty", qty);
            sendPost("/api/stock/sell", json.toString());
            loadStocks();
        } catch (Exception ignored) {}
    }

    private void bankAction(String action) {
        try {
            int amount = Integer.parseInt(bankAmountInput.getText());
            if (amount <= 0) return;
            JsonObject json = new JsonObject();
            json.addProperty("playerUuid", playerUuid);
            json.addProperty("amount", amount);
            sendPost("/api/bank/" + action, json.toString());
            loadBankBalance();
        } catch (Exception ignored) {}
    }

    private void sendPost(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(BASE_URL + path))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    // ---------- 渲染 ----------
    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        int y = 35;
        int x = 10;

        switch (tab) {
            case 0: // 市场
                context.drawTextWithShadow(textRenderer, "上架物品:", x, y, 0xFFFFFF);
                y += 12;
                sellSlotInput.setX(x); sellSlotInput.setY(y);
                sellSlotInput.render(context, mouseX, mouseY, delta);
                context.drawTextWithShadow(textRenderer, "槽位", x + 45, y + 5, 0xAAAAAA);
                sellPriceInput.setX(x + 80); sellPriceInput.setY(y);
                sellPriceInput.render(context, mouseX, mouseY, delta);
                context.drawTextWithShadow(textRenderer, "价格", x + 145, y + 5, 0xAAAAAA);
                addDrawableChild(ButtonWidget.builder(Text.literal("上架"), btn -> sellMarketItem())
                    .dimensions(x + 200, y, 40, 20).build());
                y += 25;
                for (MarketItem item : marketItems) {
                    context.drawTextWithShadow(textRenderer, item.name + " - " + item.price + " E  (卖家: " + item.seller + ")", x + 5, y, 0xAAAAAA);
                    addDrawableChild(ButtonWidget.builder(Text.literal("购买"), btn -> buyMarketItem(item.id))
                        .dimensions(width - 60, y - 2, 40, 16).build());
                    y += 14;
                }
                break;

            case 1: // 拍卖
                context.drawTextWithShadow(textRenderer, "出价金额:", x, y, 0xAAAAAA);
                bidAmountInput.setX(80); bidAmountInput.setY(y - 2);
                bidAmountInput.render(context, mouseX, mouseY, delta);
                y += 18;
                for (AuctionItem a : auctions) {
                    context.drawTextWithShadow(textRenderer, a.name + "  当前出价: " + a.currentBid + " E", x + 5, y, 0xAAAAAA);
                    addDrawableChild(ButtonWidget.builder(Text.literal("出价"), btn -> bidAuction(a.id))
                        .dimensions(width - 60, y - 2, 40, 16).build());
                    y += 14;
                }
                break;

            case 2: // 股票
                context.drawTextWithShadow(textRenderer, "数量:", x, y, 0xAAAAAA);
                stockBuyQtyInput.setX(50); stockBuyQtyInput.setY(y - 2);
                stockBuyQtyInput.render(context, mouseX, mouseY, delta);
                y += 16;
                for (StockItem s : stocks) {
                    boolean owned = ownedStocks.contains(s.code);
                    String line = s.name + " (" + s.code + ") " + s.price + " E " + String.format("%.2f", s.changePercent) + "%";
                    if (owned) line += "  [已持有]";
                    int color = s.change >= 0 ? 0x55FF55 : 0xFF5555;
                    context.drawTextWithShadow(textRenderer, line, x + 5, y, color);
                    addDrawableChild(ButtonWidget.builder(Text.literal("买入"), btn -> buyStock(s.code))
                        .dimensions(width - 100, y - 2, 40, 16).build());
                    addDrawableChild(ButtonWidget.builder(Text.literal("卖出"), btn -> sellStock(s.code))
                        .dimensions(width - 55, y - 2, 40, 16).build());
                    y += 14;
                }
                break;

            case 3: // 钱包
                context.drawTextWithShadow(textRenderer, "你的绿宝石: " + balance, x, y, 0xFFAA00);
                break;

            case 4: // 银行
                context.drawTextWithShadow(textRenderer, "银行余额: " + bankBalance + " E", x, y, 0xFFAA00);
                y += 15;
                bankAmountInput.setX(x); bankAmountInput.setY(y);
                bankAmountInput.render(context, mouseX, mouseY, delta);
                addDrawableChild(ButtonWidget.builder(Text.literal("存款"), btn -> bankAction("deposit"))
                    .dimensions(x + 90, y, 50, 20).build());
                addDrawableChild(ButtonWidget.builder(Text.literal("取款"), btn -> bankAction("withdraw"))
                    .dimensions(x + 145, y, 50, 20).build());
                break;

            case 5: // 排行榜
                context.drawTextWithShadow(textRenderer, "绿宝石排行:", x, y, 0xFFFFFF);
                y += 12;
                for (LeaderboardEntry e : leaderboard) {
                    context.drawTextWithShadow(textRenderer, e.name + " - " + e.balance + " E", x + 5, y, 0xFFFF00);
                    y += 12;
                }
                break;
        }
    }

    // ---------- 内部数据类 ----------
    static class MarketItem { String id, name, seller; int price; }
    static class AuctionItem { String id, name; int currentBid; }
    static class StockItem { String code, name; int price, change; double changePercent; }
    static class LeaderboardEntry { String name; int balance; }
    static class InventoryEntry { int slot; String name; int count; }
}
