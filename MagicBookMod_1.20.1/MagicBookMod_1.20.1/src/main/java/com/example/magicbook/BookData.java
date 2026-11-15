package com.example.magicbook;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** color: -1 = หนังสือสีขาว (ยังไม่ผูก), 0..3 = แดง/ฟ้า/เหลือง/เขียว. unlocked = bitmask ของ Spell.ordinal() */
public record BookData(int color, int unlocked) {
    public static final BookData EMPTY = new BookData(-1, 0);

    public static BookData read(ItemStack stack) {
        CompoundTag t = stack.getTag();
        if (t == null || !t.contains("MagicColor")) return EMPTY;
        return new BookData(t.getInt("MagicColor"), t.getInt("MagicUnlocked"));
    }

    public void write(ItemStack stack) {
        CompoundTag t = stack.getOrCreateTag();
        t.putInt("MagicColor", color);
        t.putInt("MagicUnlocked", unlocked);
        if (color >= 0) t.putInt("CustomModelData", color + 1); // ให้โมเดลเปลี่ยนสี
    }

    public boolean isBound() { return color >= 0; }
    public boolean has(Spell s) { return ((unlocked >> s.ordinal()) & 1) != 0; }
    public BookData with(Spell s) { return new BookData(color, unlocked | (1 << s.ordinal())); }
}
