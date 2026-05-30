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
        stocks.add(new StockItem("NEJT","NE浜ら€?,50));
        stocks.add(new StockItem("MSFT","寰蒋鏂瑰潡",120));
        stocks.add(new StockItem("APLE","鑻规灉鍏徃",95));
        stocks.add(new StockItem("GOLD","閲戠熆闆嗗洟",80));
        stocks.add(new StockItem("IRON","閾侀敪鑲′唤",30));
        stocks.add(new StockItem("COAL","鐓ょ熆鑳芥簮",20));
        stocks.add(new StockItem("REDS","绾㈢煶绉戞妧",65));
        stocks.add(new StockItem("DIAM","閽荤煶鎺ц偂",150));
        stocks.add(new StockItem("WHEAT","灏忛害鍐滀笟",15));
        stocks.add(new StockItem("WOOL","缇婃瘺绾虹粐",25));
        stocks.add(new StockItem("FISH","娓斾笟鑱斿悎",18));
        stocks.add(new StockItem("PORK","鍏荤尓鍦?,22));
        stocks.add(new StockItem("CHKN","鍏婚浮鍏徃",12));
        stocks.add(new StockItem("SUGR","鐢樿敆绯栦笟",28));
        stocks.add(new StockItem("PAPR","绾稿紶宸ヤ笟",35));
        stocks.add(new StockItem("GLSS","鐜荤拑鍒堕€?,40));
        stocks.add(new StockItem("BONE","楠ㄧ矇鍖栬偉",10));
        stocks.add(new StockItem("GUNP","鐏嵂闆嗗洟",55));
        stocks.add(new StockItem("OBSI","榛戞洔鐭冲熀寤?,100));
        stocks.add(new StockItem("NETH","涓嬬晫璐告槗",70));
        stocks.add(new StockItem("ENDR","鏈湴鐗╂祦",90));
        stocks.add(new StockItem("POTN","鑽按閰块€?,45));
        stocks.add(new StockItem("BOOK","闄勯瓟涔﹀眬",60));
        stocks.add(new StockItem("TORH","鐏妸鐓ф槑",8));
        stocks.add(new StockItem("LADR","姊瓙鍒堕€?,5));
        stocks.add(new StockItem("CHST","绠卞瓙浠撳偍",13));
        stocks.add(new StockItem("HOPR","婕忔枟宸ヤ笟",38));
        stocks.add(new StockItem("RAIL","閾佽建浜ら€?,33));
        stocks.add(new StockItem("BOAT","鑸硅埗杩愯緭",16));
        stocks.add(new StockItem("SLIM","绮樻恫鐞冪鎶€",42));
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