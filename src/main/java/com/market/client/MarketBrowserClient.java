package com.market.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.client.MinecraftClient;

public class MarketBrowserClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("EMet")
                .then(ClientCommandManager.literal("browser")
                .executes(ctx -> {
                    MinecraftClient.getInstance().setScreen(new MarketBrowserScreen());
                    return 1;
                }))
            );
        });
    }
}
