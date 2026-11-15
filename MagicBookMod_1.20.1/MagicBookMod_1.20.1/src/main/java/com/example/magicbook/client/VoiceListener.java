package com.example.magicbook.client;

import com.example.magicbook.MagicBookMod;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import io.github.givimad.whisperjni.WhisperContext;
import io.github.givimad.whisperjni.WhisperFullParams;
import io.github.givimad.whisperjni.WhisperJNI;
import io.github.givimad.whisperjni.WhisperSamplingStrategy;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.TargetDataLine;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Arrays;

/**
 * ฟังไมค์ตอนกดคลิกขวาค้างที่หนังสือ -> ตัดประโยคด้วยการเงียบ -> Whisper ถอดเสียงภาษาไทย -> ส่งข้อความไปเซิร์ฟเวอร์
 * ต้องมีไฟล์โมเดล:  .minecraft/config/magicbook/ggml-small.bin
 */
public final class VoiceListener {
    private static final int RATE = 16000, CHUNK = 1600;   // 0.1 วิ ต่อก้อน
    private static final double THRESHOLD = 0.02;          // ความดังที่ถือว่า "กำลังพูด" (ปรับได้)
    private static final int SILENCE_CHUNKS = 7;           // เงียบ 0.7 วิ = จบประโยค

    private static volatile boolean running;
    private static boolean failed;
    private static Thread thread;
    private static WhisperJNI whisper;
    private static WhisperContext ctx;

    private VoiceListener() {}

    public static synchronized void start() {
        if (failed) return;
        running = true;
        if (thread != null && thread.isAlive()) return;
        thread = new Thread(VoiceListener::loop, "MagicBook-Voice");
        thread.setDaemon(true);
        thread.start();
    }

    public static void stop() { running = false; }

    private static void loop() {
        try {
            if (!init()) return;
            AudioFormat fmt = new AudioFormat(RATE, 16, 1, true, false);
            try (TargetDataLine line = AudioSystem.getTargetDataLine(fmt)) {
                line.open(fmt, RATE * 2);
                line.start();
                say("§d🎤 กำลังฟัง...");
                byte[] buf = new byte[CHUNK * 2];
                float[] utt = new float[RATE * 14];
                int n = 0, silent = 0;
                boolean speaking = false;
                ArrayDeque<float[]> pre = new ArrayDeque<>();

                while (running) {
                    int r = line.read(buf, 0, buf.length);
                    if (r <= 0) continue;
                    int cnt = r / 2;
                    float[] ch = new float[cnt];
                    double sum = 0;
                    for (int i = 0; i < cnt; i++) {
                        short s = (short) ((buf[2 * i + 1] << 8) | (buf[2 * i] & 0xff));
                        ch[i] = s / 32768f;
                        sum += ch[i] * ch[i];
                    }
                    double rms = Math.sqrt(sum / cnt);

                    if (rms > THRESHOLD) {
                        if (!speaking) {
                            speaking = true;
                            for (float[] p : pre) n = append(utt, n, p);
                            pre.clear();
                        }
                        silent = 0;
                        n = append(utt, n, ch);
                    } else if (speaking) {
                        silent++;
                        n = append(utt, n, ch);
                    } else {
                        pre.addLast(ch);
                        if (pre.size() > 3) pre.removeFirst();
                    }

                    if (speaking && (silent >= SILENCE_CHUNKS || n + CHUNK > utt.length)) {
                        transcribe(utt, n);
                        line.flush();
                        n = 0; silent = 0; speaking = false;
                    }
                }
            }
        } catch (Throwable t) {
            failed = true;
            t.printStackTrace();
            say("§cระบบเสียงมีปัญหา: " + t.getMessage());
        } finally {
            running = false;
        }
    }

    private static int append(float[] dst, int n, float[] src) {
        int len = Math.min(src.length, dst.length - n);
        System.arraycopy(src, 0, dst, n, len);
        return n + len;
    }

    private static boolean init() throws Exception {
        if (ctx != null) return true;
        Path model = FabricLoader.getInstance().getConfigDir().resolve("magicbook").resolve("ggml-small.bin");
        if (!Files.exists(model)) {
            failed = true;
            say("§cไม่พบโมเดลเสียง: " + model);
            return false;
        }
        say("§7กำลังโหลดโมเดลเสียง...");
        WhisperJNI.loadLibrary();
        whisper = new WhisperJNI();
        ctx = whisper.init(model);
        return true;
    }

    private static void transcribe(float[] utt, int n) {
        float[] samples = Arrays.copyOf(utt, Math.max(n, RATE)); // Whisper ต้องการอย่างน้อย ~1 วิ
        WhisperFullParams p = new WhisperFullParams(WhisperSamplingStrategy.GREEDY);
        p.language = "th";
        p.printProgress = false;
        p.printTimestamps = false;
        p.nThreads = Math.max(2, Runtime.getRuntime().availableProcessors() / 2);
        if (whisper.full(ctx, p, samples, samples.length) != 0) return;

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < whisper.fullNSegments(ctx); i++) sb.append(whisper.fullGetSegmentText(ctx, i));
        String text = sb.toString().trim();
        if (text.length() < 2) return;

        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> {
            if (mc.player == null) return;
            mc.player.displayClientMessage(Component.literal("§7ได้ยิน: §f" + text), true);
            if (ClientPlayNetworking.canSend(MagicBookMod.SPEECH_PACKET)) {
                FriendlyByteBuf buf = PacketByteBufs.create();
                buf.writeUtf(text, 300);
                ClientPlayNetworking.send(MagicBookMod.SPEECH_PACKET, buf);
            }
        });
    }

    private static void say(String s) {
        Minecraft mc = Minecraft.getInstance();
        mc.execute(() -> { if (mc.player != null) mc.player.displayClientMessage(Component.literal(s), true); });
    }
}
