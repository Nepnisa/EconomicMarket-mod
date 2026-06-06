package com.market.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
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
    public UUID skyLanternUuid;          // 当前点天灯玩家（自动出价）
    public boolean enableSkyLantern;     // 是否开启点天灯模式
    public int timeIncrement;            // 每次出价延长秒数（0-20）
    public List<BidRecord> bidHistory = new ArrayList<>();
    public boolean finished = false;
    public String winnerName = "";

    public AuctionItem() {}

    public boolean isExpired() { return System.currentTimeMillis() >= endTime; }

    public String toJson() {
        long remain = endTime - System.currentTimeMillis();
        return String.format(
            "{\"id\":\"%s\",\"seller\":\"%s\",\"name\":\"%s\",\"startingPrice\":%d,\"currentBid\":%d," +
            "\"minBidIncrement\":%d,\"endTime\":%d,\"remain\":%d,\"enableSkyLantern\":%b," +
            "\"skyLanternUuid\":\"%s\",\"timeIncrement\":%d,\"bidHistory\":%s,\"finished\":%b,\"winner\":\"%s\"}",
            id, sellerName, item.getName().getString(), startingPrice, currentBid,
            minBidIncrement, endTime, Math.max(0, remain), enableSkyLantern,
            skyLanternUuid != null ? skyLanternUuid.toString() : "",
            timeIncrement,
            bidHistoryToJson(),
            finished, winnerName
        );
    }

    private String bidHistoryToJson() {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < bidHistory.size(); i++) {
            sb.append(bidHistory.get(i).toJson());
            if (i < bidHistory.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
