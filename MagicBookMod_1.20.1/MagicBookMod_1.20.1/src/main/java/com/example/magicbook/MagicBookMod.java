package com.example.magicbook;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public class MagicBookMod implements ModInitializer {
    public static final String MOD_ID = "magicbook";
    public static final ResourceLocation SPEECH_PACKET = new ResourceLocation(MOD_ID, "speech");
    public static Item MAGIC_BOOK;

    @Override
    public void onInitialize() {
        MAGIC_BOOK = Registry.register(BuiltInRegistries.ITEM, new ResourceLocation(MOD_ID, "magic_book"),
                new MagicBookItem(new Item.Properties().stacksTo(1)));

        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
                .register(e -> e.accept(MAGIC_BOOK));

        // client -> server : ข้อความที่ได้ยินจากไมค์
        ServerPlayNetworking.registerGlobalReceiver(SPEECH_PACKET, (server, player, handler, buf, sender) -> {
            String text = buf.readUtf(300);
            server.execute(() -> SpellHandler.handle(player, text, true));
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Orbs.tick();
            BlackHole.tickAll();
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
            BlackHole.releaseAll();
            Orbs.clear();
        });

        // คำสั่งทดสอบโดยไม่ใช้ไมค์:  /magicbook say บอลไฟ  (ถือหนังสือในมือหลัก)
        CommandRegistrationCallback.EVENT.register((dispatcher, access, env) ->
                dispatcher.register(Commands.literal("magicbook")
                        .requires(s -> s.hasPermission(2))
                        .then(Commands.literal("say")
                                .then(Commands.argument("text", StringArgumentType.greedyString())
                                        .executes(c -> {
                                            SpellHandler.handle(c.getSource().getPlayerOrException(),
                                                    StringArgumentType.getString(c, "text"), false);
                                            return 1;
                                        })))));
    }
}
