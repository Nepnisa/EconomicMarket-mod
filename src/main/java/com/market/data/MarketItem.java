package com.market.data;

import net.minecraft.item.ItemStack;
import java.util.UUID;

public class MarketItem {
    public UUID id;
    public UUID sellerUuid;
    public String sellerName;
    public ItemStack item;
    public int price;
    public long createdTime;

    public MarketItem() {}
    public MarketItem(UUID id, UUID sellerUuid, String sellerName, ItemStack item, int price) {
        this.id = id; this.sellerUuid = sellerUuid; this.sellerName = sellerName;
        this.item = item.copy(); this.price = price; this.createdTime = System.currentTimeMillis();
    }
    public String toJson() {
        return String.format("{\"id\":\"%s\",\"seller\":\"%s\",\"name\":\"%s\",\"price\":%d}",
                id, sellerName, item.getName().getString(), price);
    }
}