package com.example.magicbook;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class MagicBookItem extends Item {
    public MagicBookItem(Properties properties) { super(properties); }

    @Override public int getUseDuration(ItemStack stack) { return 72000; }
    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.NONE; }

    /** กดคลิกขวาค้าง = เริ่มใช้ (client จะเปิดไมค์เองตอนที่กำลังใช้หนังสืออยู่) */
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!level.isClientSide) {
            BookData d = BookData.read(stack);
            if (!d.isBound()) {
                player.displayClientMessage(Component.literal("§7พูดว่า §f\"หนังสือของฉัน\" §7เพื่อผูกหนังสือ"), true);
            }
        }
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remaining) {
        if (level instanceof ServerLevel sl && entity.tickCount % 3 == 0) {
            sl.sendParticles(ParticleTypes.ENCHANT, entity.getX(), entity.getEyeY(), entity.getZ(),
                    6, 0.6, 0.5, 0.6, 0.4);
        }
    }
}
