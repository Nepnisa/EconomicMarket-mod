package com.market.data;

import com.google.gson.*;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import net.minecraft.util.JsonHelper;

import java.lang.reflect.Type;
import java.util.Base64;

public class ItemStackAdapter implements JsonSerializer<ItemStack>, JsonDeserializer<ItemStack> {

    @Override
    public JsonElement serialize(ItemStack src, Type typeOfSrc, JsonSerializationContext context) {
        // 将 ItemStack 转为 NBT，再转为 Base64 字符串
        NbtCompound nbt = src.writeNbt(new NbtCompound());
        byte[] nbtBytes = nbt.toString().getBytes();
        String base64 = Base64.getEncoder().encodeToString(nbtBytes);
        JsonObject obj = new JsonObject();
        obj.addProperty("type", "itemstack");
        obj.addProperty("data", base64);
        return obj;
    }

    @Override
    public ItemStack deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject obj = json.getAsJsonObject();
        String base64 = obj.get("data").getAsString();
        byte[] nbtBytes = Base64.getDecoder().decode(base64);
        try {
            NbtCompound nbt = StringNbtReader.parse(new String(nbtBytes)).readCompoundTag();
            return ItemStack.fromNbt(nbt);
        } catch (CommandSyntaxException e) {
            throw new JsonParseException(e);
        }
    }
}
