package com.example.magicbook;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/** ลูกบอลเวท 5 ธาตุ: คำนวณเองบนเซิร์ฟเวอร์ (ไม่ใช้ entity) */
public final class Orbs {
    private static final class Orb {
        final ServerLevel level; Vec3 pos; final Vec3 dir; final Spell type; final UUID owner; int age;
        Orb(ServerLevel l, Vec3 p, Vec3 d, Spell t, UUID o) { level = l; pos = p; dir = d; type = t; owner = o; }
    }

    private static final List<Orb> ORBS = new ArrayList<>();
    private Orbs() {}

    public static void clear() { ORBS.clear(); }

    public static void spawn(ServerPlayer p, Spell type) {
        Vec3 dir = p.getLookAngle();
        ServerLevel lvl = (ServerLevel) p.level();
        ORBS.add(new Orb(lvl, p.getEyePosition().add(dir.scale(1.0)), dir, type, p.getUUID()));
        lvl.playSound(null, p.blockPosition(), type == Spell.FIRE ? SoundEvents.BLAZE_SHOOT : SoundEvents.EVOKER_CAST_SPELL,
                SoundSource.PLAYERS, 1f, 1f);
    }

    public static void tick() {
        Iterator<Orb> it = ORBS.iterator();
        while (it.hasNext()) if (!step(it.next())) it.remove();
    }

    private static boolean step(Orb o) {
        if (o.age++ > 60) return false;
        for (int i = 0; i < 2; i++) {
            o.pos = o.pos.add(o.dir.scale(0.6));
            o.level.sendParticles(particle(o.type), o.pos.x, o.pos.y, o.pos.z, 4, 0.12, 0.12, 0.12, 0.01);

            BlockPos bp = BlockPos.containing(o.pos);
            BlockState bs = o.level.getBlockState(bp);
            if (!bs.getCollisionShape(o.level, bp).isEmpty()) { impact(o); return false; }

            List<LivingEntity> hit = o.level.getEntitiesOfClass(LivingEntity.class,
                    new AABB(o.pos, o.pos).inflate(0.6),
                    e -> e.isAlive() && !e.isSpectator() && !e.getUUID().equals(o.owner));
            if (!hit.isEmpty()) { impact(o); return false; }
        }
        return true;
    }

    private static void impact(Orb o) {
        o.level.sendParticles(particle(o.type), o.pos.x, o.pos.y, o.pos.z, 40, 0.6, 0.6, 0.6, 0.15);
        o.level.sendParticles(ParticleTypes.EXPLOSION, o.pos.x, o.pos.y, o.pos.z, 1, 0, 0, 0, 0);
        ServerPlayer owner = o.level.getServer().getPlayerList().getPlayer(o.owner);
        DamageSource src = owner == null ? o.level.damageSources().magic() : o.level.damageSources().indirectMagic(owner, owner);

        for (LivingEntity e : o.level.getEntitiesOfClass(LivingEntity.class, new AABB(o.pos, o.pos).inflate(2.5),
                x -> x.isAlive() && !x.getUUID().equals(o.owner))) {
            Vec3 away = e.position().subtract(o.pos).multiply(1, 0, 1).normalize();
            switch (o.type) {
                case FIRE -> { e.hurt(src, 5f); e.setRemainingFireTicks(100); }
                case WATER -> {
                    e.hurt(src, 3f); e.clearFire();
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 1));
                    knock(e, away, 1.0);
                }
                case EARTH -> {
                    e.hurt(src, 6f);
                    e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 2));
                    knock(e, away, 1.6);
                }
                case LIGHT -> {
                    e.hurt(src, e.getType().is(EntityTypeTags.UNDEAD) ? 10f : 5f);
                    e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 100, 0));
                }
                case DARK -> {
                    e.hurt(src, 5f);
                    e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
                }
                default -> {}
            }
        }
    }

    private static void knock(LivingEntity e, Vec3 dir, double power) {
        e.push(dir.x * power, 0.4, dir.z * power);
        e.hurtMarked = true;
    }

    private static ParticleOptions particle(Spell s) {
        return switch (s) {
            case FIRE -> ParticleTypes.FLAME;
            case WATER -> ParticleTypes.BUBBLE;
            case EARTH -> new BlockParticleOption(ParticleTypes.BLOCK, Blocks.DIRT.defaultBlockState());
            case LIGHT -> ParticleTypes.END_ROD;
            case DARK -> ParticleTypes.SQUID_INK;
            default -> ParticleTypes.SMOKE;
        };
    }
}
