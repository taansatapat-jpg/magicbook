package com.example.magicbook;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

public enum Spell {
    FIRE("บอลไฟ", "บอลไฟ", Items.BLAZE_POWDER, 4, 15, 0.2),
    WATER("บอลน้ำ", "บอลน้ำ", Items.PRISMARINE_CRYSTALS, 4, 15, 0.2),
    EARTH("บอลดิน", "บอลดิน", Items.IRON_INGOT, 3, 15, 0.2),
    LIGHT("บอลแสง", "บอลแสง", Items.GLOWSTONE_DUST, 8, 15, 0.2),
    DARK("บอลมืด", "บอลมืด", Items.ENDER_PEARL, 2, 15, 0.2),
    HEAL("จงรักษา", "จงรักษา", Items.GOLDEN_APPLE, 1, 60, 0.2),
    BLACK_HOLE("หลุมดำ",
            "มวลสารจงทะลายกฎเกณฑ์จงสูญสิ้นดูดกลืนทุกสิ่งสู่ความว่างเปล่าชั่วนิรันดร์ ดูม",
            Items.NETHER_STAR, 1, 1200, 0.3);

    public final String title;
    public final String phrase;      // ที่ normalize แล้ว
    public final Item costItem;      // ของที่ใช้ "ปลดล็อก" ครั้งแรก (ใช้ครั้งเดียว)
    public final int costCount;
    public final int cooldownTicks;
    public final double tolerance;   // ยอมให้ถอดเสียงผิดได้กี่ % ของความยาวคำ

    Spell(String title, String phrase, Item costItem, int costCount, int cooldownTicks, double tolerance) {
        this.title = title;
        this.phrase = TextMatch.normalize(phrase);
        this.costItem = costItem;
        this.costCount = costCount;
        this.cooldownTicks = cooldownTicks;
        this.tolerance = tolerance;
    }
}
