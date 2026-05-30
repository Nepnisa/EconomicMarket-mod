package com.market.data;

import net.minecraft.item.Item;
import net.minecraft.item.Items;
import java.util.*;

public class PawnShopData {
    public static final Map<Item, Integer> PRICES = new LinkedHashMap<>();
    static {
        PRICES.put(Items.COBBLESTONE, 1);
        PRICES.put(Items.OAK_LOG, 2);
        PRICES.put(Items.IRON_INGOT, 5);
        PRICES.put(Items.GOLD_INGOT, 10);
        PRICES.put(Items.DIAMOND, 20);
    }
}