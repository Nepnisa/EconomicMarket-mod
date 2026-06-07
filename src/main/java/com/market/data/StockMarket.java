package com.market.data;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class StockMarket {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("economicmarket/stock_market.nbt");
    public static final List<StockItem> stocks = new ArrayList<>();
    private static final Random random = new Random();

    static {
        // A股（原30只）
        stocks.add(new StockItem("NEJT","NE交通",50));
        stocks.add(new StockItem("MSFT","微软方块",120));
        stocks.add(new StockItem("APLE","苹果公司",95));
        stocks.add(new StockItem("GOLD","金矿集团",80));
        stocks.add(new StockItem("IRON","铁锭股份",30));
        stocks.add(new StockItem("COAL","煤矿能源",20));
        stocks.add(new StockItem("REDS","红石科技",65));
        stocks.add(new StockItem("DIAM","钻石控股",150));
        stocks.add(new StockItem("WHEAT","小麦农业",15));
        stocks.add(new StockItem("WOOL","羊毛纺织",25));
        stocks.add(new StockItem("FISH","渔业联合",18));
        stocks.add(new StockItem("PORK","养猪场",22));
        stocks.add(new StockItem("CHKN","养鸡公司",12));
        stocks.add(new StockItem("SUGR","甘蔗糖业",28));
        stocks.add(new StockItem("PAPR","纸张工业",35));
        stocks.add(new StockItem("GLSS","玻璃制造",40));
        stocks.add(new StockItem("BONE","骨粉化肥",10));
        stocks.add(new StockItem("GUNP","火药集团",55));
        stocks.add(new StockItem("OBSI","黑曜石基建",100));
        stocks.add(new StockItem("NETH","下界贸易",70));
        stocks.add(new StockItem("ENDR","末地物流",90));
        stocks.add(new StockItem("POTN","药水酿造",45));
        stocks.add(new StockItem("BOOK","附魔书局",60));
        stocks.add(new StockItem("TORH","火把照明",8));
        stocks.add(new StockItem("LADR","梯子制造",5));
        stocks.add(new StockItem("CHST","箱子仓储",13));
        stocks.add(new StockItem("HOPR","漏斗工业",38));
        stocks.add(new StockItem("RAIL","铁轨交通",33));
        stocks.add(new StockItem("BOAT","船舶运输",16));
        stocks.add(new StockItem("SLIM","粘液球科技",42));

        // B股（新增30只）
        stocks.add(new StockItem("XYGD","星云轨道",60));
        stocks.add(new StockItem("BIQT","碧青轨道",55));
        stocks.add(new StockItem("QYZG","穹渊重工",80));
        stocks.add(new StockItem("BY01","深岩集团",45));
        stocks.add(new StockItem("BY02","熔火能源",70));
        stocks.add(new StockItem("BY03","虚空物流",65));
        stocks.add(new StockItem("BY04","结晶科技",90));
        stocks.add(new StockItem("BY05","龙息化工",50));
        stocks.add(new StockItem("BY06","幻翼航空",75));
        stocks.add(new StockItem("BY07","暗影机械",85));
        stocks.add(new StockItem("BY08","闪耀传媒",40));
        stocks.add(new StockItem("BY09","雷鸣电力",58));
        stocks.add(new StockItem("BY10","极光通信",62));
        stocks.add(new StockItem("BY11","苍穹农业",35));
        stocks.add(new StockItem("BY12","不朽建材",48));
        stocks.add(new StockItem("BY13","幻梦医药",72));
        stocks.add(new StockItem("BY14","星火冶炼",55));
        stocks.add(new StockItem("BY15","静水渔业",30));
        stocks.add(new StockItem("BY16","破晓纺织",38));
        stocks.add(new StockItem("BY17","永夜矿业",95));
        stocks.add(new StockItem("BY18","潮汐食品",42));
        stocks.add(new StockItem("BY19","微风酿造",68));
        stocks.add(new StockItem("BY20","月光书局",52));
        stocks.add(new StockItem("BY21","星辰照明",20));
        stocks.add(new StockItem("BY22","天梯制造",15));
        stocks.add(new StockItem("BY23","次元仓储",33));
        stocks.add(new StockItem("BY24","深渊工业",78));
        stocks.add(new StockItem("BY25","浮空交通",46));
        stocks.add(new StockItem("BY26","远洋运输",39));
        stocks.add(new StockItem("BY27","凝胶科技",66));

        load();
    }

    public static StockItem getByCode(String code) { return stocks.stream().filter(s->s.code.equals(code)).findFirst().orElse(null); }

    public static void tickPrices() {
        for (StockItem s : stocks) {
            int old = s.price;
            double fluc = (random.nextDouble() - 0.5) * 0.1;
            int newPrice = (int) Math.max(1, old * (1 + fluc));
            s.change = newPrice - old;
            s.changePercent = (double)s.change / old * 100.0;
            s.price = newPrice;
        }
        save();
    }

    private static void load() {
        if (!Files.exists(FILE)) return;
        try {
            NbtCompound root = NbtIo.readCompressed(FILE.toFile());
            if (root == null) return;
            NbtList list = root.getList("stocks", 10);
            for (int i=0; i<list.size(); i++) {
                NbtCompound t = list.getCompound(i);
                StockItem s = getByCode(t.getString("code"));
                if (s != null) {
                    s.price = t.getInt("price");
                    s.change = t.getInt("change");
                    s.changePercent = t.getDouble("changePercent");
                }
            }
        } catch (IOException ignored) {}
    }

    private static void save() {
        NbtCompound root = new NbtCompound();
        NbtList list = new NbtList();
        for (StockItem s : stocks) {
            NbtCompound t = new NbtCompound();
            t.putString("code", s.code);
            t.putString("name", s.name);
            t.putInt("price", s.price);
            t.putInt("change", s.change);
            t.putDouble("changePercent", s.changePercent);
            list.add(t);
        }
        root.put("stocks", list);
        try {
            Files.createDirectories(FILE.getParent());
            NbtIo.writeCompressed(root, FILE.toFile());
        } catch (IOException ignored) {}
    }
}
