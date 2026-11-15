package com.example.magicbook;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;

/** หลุมดำ 20 วิ: ดูดทุกอย่างเข้ามาวนอยู่ข้างใน (ไม่ตาย) แล้วพ่นออกพร้อมดาเมจ */
public final class BlackHole {
    public static final int DURATION = 400;         // 20 วิ
    private static final double RADIUS = 14;        // รัศมีดูด
    private static final float EJECT_DAMAGE = 12f;  // ดาเมจตอนถูกพ่นออก (6 หัวใจ)
    private static final boolean SPARE_CASTER = true; // true = ผู้ร่ายไม่โดนดูด

    private static final List<BlackHole> ACTIVE = new ArrayList<>();

    private static final class Cap {
        final Entity e; double angle; final double radius, height;
        Cap(Entity e, RandomSourceLike r) { this.e = e; angle = r.next() * Math.PI * 2; radius = 1 + r.next() * 2.5; height = (r.next() - 0.5) * 3; }
    }
    private interface RandomSourceLike { double next(); }

    private final ServerLevel level;
    private final Vec3 center;
    private final UUID owner;
    private int age;
    private final Map<UUID, Cap> caps = new HashMap<>();

    private BlackHole(ServerLevel l, Vec3 c, UUID o) { level = l; center = c; owner = o; }

    public static void spawn(ServerPlayer p) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        BlockHitResult hit = p.level().clip(new ClipContext(eye, eye.add(look.scale(12)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 c = hit.getType() == HitResult.Type.MISS ? hit.getLocation() : hit.getLocation().subtract(look.scale(1.5));
        ServerLevel lvl = (ServerLevel) p.level();
        ACTIVE.add(new BlackHole(lvl, c, p.getUUID()));
        lvl.playSound(null, c.x, c.y, c.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 4f, 0.5f);
    }

    public static void tickAll() {
        Iterator<BlackHole> it = ACTIVE.iterator();
        while (it.hasNext()) if (!it.next().tick()) it.remove();
    }

    public static void releaseAll() {
        for (BlackHole h : ACTIVE) h.release(false);
        ACTIVE.clear();
    }

    private boolean tick() {
        age++;
        visuals();
        RandomSourceLike rnd = () -> level.random.nextDouble();

        // ดูด + จับ
        List<Entity> list = level.getEntities((Entity) null, new AABB(center, center).inflate(RADIUS),
                e -> e.isAlive() && !e.isSpectator()
                        && !(SPARE_CASTER && e.getUUID().equals(owner))
                        && (e instanceof LivingEntity || e instanceof ItemEntity));
        for (Entity e : list) {
            if (caps.containsKey(e.getUUID())) continue;
            Vec3 to = center.subtract(e.position().add(0, e.getBbHeight() / 2, 0));
            double d = to.length();
            if (d > RADIUS) continue;
            if (d < 2.5) {
                caps.put(e.getUUID(), new Cap(e, rnd));
            } else {
                Vec3 v = e.getDeltaMovement().scale(0.6).add(to.normalize().scale(0.25 + (RADIUS - d) * 0.04));
                e.setDeltaMovement(v);
                e.hurtMarked = true;
                e.fallDistance = 0;
            }
        }

        // สิ่งที่ถูกจับ: วนรอบในหลุมดำ
        Iterator<Cap> it = caps.values().iterator();
        while (it.hasNext()) {
            Cap c = it.next();
            Entity e = c.e;
            if (e.isRemoved() || !e.isAlive()) { it.remove(); continue; }
            c.angle += 0.28;
            Vec3 target = center.add(Math.cos(c.angle) * c.radius,
                    c.height + Math.sin(age * 0.12 + c.height) * 0.7 - e.getBbHeight() / 2,
                    Math.sin(c.angle) * c.radius);
            e.setDeltaMovement(target.subtract(e.position()).scale(0.5));
            e.hurtMarked = true;
            e.fallDistance = 0;
            // กันตายระหว่างอยู่ข้างใน (Resistance 5 = กันดาเมจ 100%)
            if (e instanceof LivingEntity le)
                le.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 5, 4, false, false, false));
        }

        if (age >= DURATION) { release(true); return false; }
        return true;
    }

    private void release(boolean launch) {
        for (Cap c : caps.values()) {
            Entity e = c.e;
            if (e.isRemoved()) continue;
            if (e instanceof LivingEntity le) le.removeEffect(MobEffects.DAMAGE_RESISTANCE);
            if (!launch) continue;
            double a = level.random.nextDouble() * Math.PI * 2;
            e.setDeltaMovement(Math.cos(a) * 2.2, 0.9, Math.sin(a) * 2.2);
            e.hurtMarked = true;
            if (e instanceof LivingEntity le) le.hurt(level.damageSources().magic(), EJECT_DAMAGE);
        }
        caps.clear();
        if (launch) {
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 3, 1, 1, 1, 0);
            level.sendParticles(ParticleTypes.REVERSE_PORTAL, center.x, center.y, center.z, 200, 1, 1, 1, 0.8);
            level.playSound(null, center.x, center.y, center.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 5f, 0.6f);
        }
    }

    private void visuals() {
        level.sendParticles(ParticleTypes.SQUID_INK, center.x, center.y, center.z, 8, 0.35, 0.35, 0.35, 0.02);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y, center.z, 4, 0.6, 0.6, 0.6, 0.01);
        for (int i = 0; i < 16; i++) {
            double a = level.random.nextDouble() * Math.PI * 2, r = 2.5 + level.random.nextDouble() * 5;
            level.sendParticles(ParticleTypes.PORTAL,
                    center.x + Math.cos(a) * r, center.y + (level.random.nextDouble() - 0.5) * 0.6, center.z + Math.sin(a) * r,
                    0, -Math.cos(a), 0, -Math.sin(a), 1.5);
        }
        if (age % 20 == 1)
            level.playSound(null, center.x, center.y, center.z, SoundEvents.BEACON_AMBIENT, SoundSource.HOSTILE, 3f, 0.5f);
    }
}
