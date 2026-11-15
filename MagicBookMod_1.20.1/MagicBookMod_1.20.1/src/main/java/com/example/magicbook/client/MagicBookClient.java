package com.example.magicbook.client;

import com.example.magicbook.MagicBookItem;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MagicBookClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // กดคลิกขวาค้างที่หนังสือ = เปิดไมค์ / ปล่อย = ปิดไมค์
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            boolean active = mc.player != null && mc.player.isUsingItem()
                    && mc.player.getUseItem().getItem() instanceof MagicBookItem;
            if (active) VoiceListener.start(); else VoiceListener.stop();
        });
    }
}
