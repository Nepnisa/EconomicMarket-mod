package com.market.data;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.nbt.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class PlayerStocks {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("economicmarket/player_stocks.nbt");

    public static Map<UUID, Map<String, Holding>> loadAll() {
        if (!Files.exists(FILE)) return new HashMap<>();
        try {
            NbtCompound root = NbtIo.readCompressed(FILE.toFile());
            Map<UUID, Map<String, Holding>> all = new HashMap<>();
            for (String uuidStr : root.getKeys()) {
                UUID uuid = UUID.fromString(uuidStr);
                NbtCompound pnbt = root.getCompound(uuidStr);
                Map<String, Holding> map = new HashMap<>();
                for (String code : pnbt.getKeys()) {
                    NbtCompound h = pnbt.getCompound(code);
                    map.put(code, new Holding(h.getInt("qty"), h.getInt("avg")));
                }
                all.put(uuid, map);
            }
            return all;
        } catch (IOException e) { return new HashMap<>(); }
    }

    public static void saveAll(Map<UUID, Map<String, Holding>> all) {
        NbtCompound root = new NbtCompound();
        all.forEach((uuid, map) -> {
            NbtCompound pnbt = new NbtCompound();
            map.forEach((code, h) -> {
                NbtCompound hnbt = new NbtCompound();
                hnbt.putInt("qty", h.qty);
                hnbt.putInt("avg", h.avg);
                pnbt.put(code, hnbt);
            });
            root.put(uuid.toString(), pnbt);
        });
        try {
            Files.createDirectories(FILE.getParent());
            NbtIo.writeCompressed(root, FILE.toFile());
        } catch (IOException ignored) {}
    }

    public static class Holding {
        public int qty;
        public int avg;
        public Holding(int q, int a) { qty = q; avg = a; }
    }
}