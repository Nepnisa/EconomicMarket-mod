package com.market.data;

import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;

public class CurrencyUtils {
    public static int getEmeraldCount(ServerPlayerEntity player) {
        int count = 0;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(Items.EMERALD)) count += stack.getCount();
        }
        return count;
    }

    public static boolean deductEmeralds(ServerPlayerEntity player, int amount) {
        if (getEmeraldCount(player) < amount) return false;
        int remaining = amount;
        for (ItemStack stack : player.getInventory().main) {
            if (stack.isOf(Items.EMERALD)) {
                int take = Math.min(stack.getCount(), remaining);
                stack.decrement(take);
                remaining -= take;
                if (remaining <= 0) break;
            }
        }
        player.getInventory().markDirty();
        return true;
    }

    public static void giveEmeralds(ServerPlayerEntity player, int amount) {
        ItemStack stack = new ItemStack(Items.EMERALD, amount);
        if (!player.getInventory().insertStack(stack)) {
            player.dropItem(stack, false);
        }
    }
}