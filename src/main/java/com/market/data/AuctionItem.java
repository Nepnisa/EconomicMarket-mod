package com.market.data;

import net.minecraft.item.ItemStack;
import java.util.UUID;

public class AuctionItem {
    public UUID id;
    public UUID sellerUuid;
    public String sellerName;
    public ItemStack item;
    public int startingPrice;
    public int minBidIncrement;
    public long endTime;
    public int currentBid;
    public UUID currentBidderUuid;
    public String currentBidderName;
    public UUID skyLanternUuid;

    public AuctionItem() {}
    public boolean isExpired() { return System.currentTimeMillis() >= endTime; }
    public String toJson() {
        long remain = endTime - System.currentTimeMillis();
        return String.format("{\"id\":\"%s\",\"seller\":\"%s\",\"name\":\"%s\",\"currentBid\":%d,\"endTime\":%d,\"remain\":%d,\"skyLantern\":%b}",
                id, sellerName, item.getName().getString(), currentBid, endTime, Math.max(0, remain), skyLanternUuid != null);
    }
}