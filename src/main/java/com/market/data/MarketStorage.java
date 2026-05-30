package com.market.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class MarketStorage {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("economicmarket/market_data.json");
    private static final Gson GSON = new Gson();

    public static List<MarketItem> load() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try {
            String json = Files.readString(FILE);
            return GSON.fromJson(json, new TypeToken<List<MarketItem>>(){}.getType());
        } catch (IOException e) { return new ArrayList<>(); }
    }

    public static void save(List<MarketItem> items) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(items));
        } catch (IOException e) { e.printStackTrace(); }
    }
}