package com.example.magicbook;

/** จับคู่ข้อความที่ Whisper ถอดมา กับคำร่ายเวท แบบยอมให้ผิดเล็กน้อย (fuzzy) */
public final class TextMatch {
    private TextMatch() {}

    public static String normalize(String s) {
        return s.toLowerCase().replaceAll("[\\s\\p{Punct}“”‘’…]+", "");
    }

    /** คืนค่า 0 = ตรงเป๊ะ, ยิ่งมากยิ่งไม่ตรง (= ระยะแก้ไข / ความยาวคำ) หาในทุกช่วงของข้อความ */
    public static double score(String text, String key) {
        int k = key.length();
        if (text.isEmpty() || k == 0) return 9;
        int best = Integer.MAX_VALUE;
        if (text.length() < k - 2) {
            best = lev(text, key);
        } else {
            for (int len = Math.max(1, k - 2); len <= k + 2 && len <= text.length(); len++) {
                for (int i = 0; i + len <= text.length(); i++) {
                    best = Math.min(best, lev(text.substring(i, i + len), key));
                    if (best == 0) return 0;
                }
            }
        }
        return (double) best / k;
    }

    private static int lev(String a, String b) {
        int[] prev = new int[b.length() + 1], cur = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;
        for (int i = 1; i <= a.length(); i++) {
            cur[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int c = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                cur[j] = Math.min(Math.min(cur[j - 1] + 1, prev[j] + 1), prev[j - 1] + c);
            }
            int[] t = prev; prev = cur; cur = t;
        }
        return prev[b.length()];
    }
}
