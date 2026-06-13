package com.market.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class BankStorage {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("economicmarket/bank_data.json");
    private static final Gson GSON = new Gson();

    public static Map<UUID, BankAccount> loadAll() {
        if (!Files.exists(FILE)) return new HashMap<>();
        try {
            String json = Files.readString(FILE);
            return GSON.fromJson(json, new TypeToken<Map<UUID, BankAccount>>(){}.getType());
        } catch (IOException e) { return new HashMap<>(); }
    }

    public static void saveAll(Map<UUID, BankAccount> accounts) {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(accounts));
        } catch (IOException e) { e.printStackTrace(); }
    }

    public static BankAccount getAccount(UUID uuid) {
        Map<UUID, BankAccount> all = loadAll();
        return all.computeIfAbsent(uuid, k -> new BankAccount(0, System.currentTimeMillis()));
    }

    public static void updateAccount(UUID uuid, BankAccount account) {
        Map<UUID, BankAccount> all = loadAll();
        all.put(uuid, account);
        saveAll(all);
    }
}
