package com.example.magicbook;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpellHandler {
    private static final String BIND_PHRASE = TextMatch.normalize("หนังสือของฉัน");
    private static final Map<String, Long> COOLDOWNS = new HashMap<>();

    private SpellHandler() {}

    public static void handle(ServerPlayer player, String raw, boolean requireUsing) {
        ItemStack book = findBook(player, requireUsing);
        if (book == null) return;
        String text = TextMatch.normalize(raw);
        if (text.length() < 2) return;

        BookData data = BookData.read(book);
        ServerLevel level = (ServerLevel) player.level();

        // 1) ยังเป็นหนังสือสีขาว -> ต้องพูด "หนังสือของฉัน" ก่อน เพื่อสุ่มสี
        if (!data.isBound()) {
            if (TextMatch.score(text, BIND_PHRASE) <= 0.25) {
                int color = player.getRandom().nextInt(4); // 0 แดง 1 ฟ้า 2 เหลือง 3 เขียว
                new BookData(color, 0).write(book);
                String[] names = {"§cสีแดง", "§9สีฟ้า", "§eสีเหลือง", "§aสีเขียว"};
                msg(player, "§6✦ หนังสือเลือกคุณแล้ว! หนังสือกลายเป็น" + names[color]);
                level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getEyeY(), player.getZ(),
                        40, 0.5, 0.5, 0.5, 0.3);
                level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1f, 1f);
            }
            return;
        }

        // 2) หาคำร่ายเวทที่ใกล้เคียงที่สุด
        Spell best = null;
        double bestScore = 9;
        for (Spell s : Spell.values()) {
            double sc = TextMatch.score(text, s.phrase);
            if (sc <= s.tolerance && sc < bestScore) { best = s; bestScore = sc; }
        }
        if (best == null) return;

        // 3) ยังไม่ปลดล็อก -> ใช้ของปลดล็อก (ครั้งเดียว)
        if (!data.has(best)) {
            if (!consume(player, best)) {
                msg(player, "§cยังไม่ได้ปลดล็อก §f" + best.title + " §c(ต้องมี §e"
                        + best.costItem.getDescription().getString() + " x" + best.costCount + "§c ในกระเป๋า)");
                return;
            }
            data.with(best).write(book);
            msg(player, "§b✦ ปลดล็อกคำร่ายเวท: §f" + best.title + " §b— พูดอีกครั้งเพื่อร่าย");
            level.sendParticles(ParticleTypes.ENCHANT, player.getX(), player.getEyeY(), player.getZ(), 60, 0.6, 0.6, 0.6, 0.6);
            return;
        }

        // 4) cooldown
        String key = player.getUUID() + ":" + best.ordinal();
        long now = level.getGameTime();
        Long until = COOLDOWNS.get(key);
        if (until != null && now < until) {
            msg(player, "§7" + best.title + " ยังไม่พร้อม (" + ((until - now) / 20 + 1) + " วิ)");
            return;
        }
        COOLDOWNS.put(key, now + best.cooldownTicks);

        // 5) ร่ายเวท
        switch (best) {
            case HEAL -> heal(player, level);
            case BLACK_HOLE -> {
                msg(player, "§5✦ ดูม!!");
                BlackHole.spawn(player);
            }
            default -> {
                Orbs.spawn(player, best);
                msg(player, "§6✦ " + best.title);
            }
        }
    }

    private static void heal(ServerPlayer caster, ServerLevel level) {
        for (Player p : level.getEntitiesOfClass(Player.class, caster.getBoundingBox().inflate(5))) {
            p.heal(8f);
            p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 1));
            level.sendParticles(ParticleTypes.HEART, p.getX(), p.getY() + 1.2, p.getZ(), 10, 0.4, 0.5, 0.4, 0.1);
        }
        level.playSound(null, caster.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1f, 1.5f);
        msg(caster, "§a✦ จงรักษา");
    }

    private static ItemStack findBook(ServerPlayer p, boolean requireUsing) {
        if (p.isUsingItem() && p.getUseItem().getItem() instanceof MagicBookItem) return p.getUseItem();
        if (!requireUsing && p.getMainHandItem().getItem() instanceof MagicBookItem) return p.getMainHandItem();
        return null;
    }

    private static boolean consume(ServerPlayer p, Spell s) {
        int have = 0;
        for (ItemStack st : p.getInventory().items) if (st.is(s.costItem)) have += st.getCount();
        if (have < s.costCount) return false;
        int left = s.costCount;
        for (ItemStack st : p.getInventory().items) {
            if (left <= 0) break;
            if (st.is(s.costItem)) { int t = Math.min(left, st.getCount()); st.shrink(t); left -= t; }
        }
        return true;
    }

    private static void msg(ServerPlayer p, String s) { p.displayClientMessage(Component.literal(s), true); }
}
