package com.market.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.*;
import java.util.*;

public class AuctionManager {
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("economicmarket/auction_data.json");
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(ItemStack.class, new ItemStackAdapter())
            .create();
    public static List<AuctionItem> auctions = new ArrayList<>();

    static { auctions = load(); }

    public static List<AuctionItem> load() {
        if (!Files.exists(FILE)) return new ArrayList<>();
        try { String json = Files.readString(FILE); return GSON.fromJson(json, new TypeToken<List<AuctionItem>>(){}.getType()); }
        catch (IOException e) { return new ArrayList<>(); }
    }

    public static void save() {
        try { Files.createDirectories(FILE.getParent()); Files.writeString(FILE, GSON.toJson(auctions)); }
        catch (IOException e) { e.printStackTrace(); }
    }

    public static void checkExpiredAuctions(MinecraftServer server) {
        Iterator<AuctionItem> it = auctions.iterator();
        while (it.hasNext()) {
            AuctionItem a = it.next();
            if (a.isExpired()) {
                if (a.currentBid > 0 && a.currentBidderUuid != null) {
                    ServerPlayerEntity winner = server.getPlayerManager().getPlayer(a.currentBidderUuid);
                    if (winner != null) {
                        if (!winner.getInventory().insertStack(a.item.copy())) winner.dropItem(a.item.copy(), false);
                    }
                    ServerPlayerEntity seller = server.getPlayerManager().getPlayer(a.sellerUuid);
                    if (seller != null) {
                        CurrencyUtils.giveEmeralds(seller, a.currentBid);
                        seller.sendMessage(Text.literal("Auction sold for " + a.currentBid + " emeralds"));
                    }
                } else {
                    ServerPlayerEntity seller = server.getPlayerManager().getPlayer(a.sellerUuid);
                    if (seller != null) {
                        if (!seller.getInventory().insertStack(a.item.copy())) seller.dropItem(a.item.copy(), false);
                        seller.sendMessage(Text.literal("Auction ended with no bids"));
                    }
                }
                it.remove();
            }
        }
        save();
    }
}
