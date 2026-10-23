package net.kdt.pojavlaunch.minperf; // змінити під пакет у твоєму форку

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Java-версія PerfProfile для форку Amethyst-Android (Java 8 стиль).
 *
 * УВАГА: значення нижче — стартові. Назви рендерерів ОБОВ'ЯЗКОВО звір зі своїм клоном
 * (grep -rn "POJAV_RENDERER|vulkan_zink|zink" app_pojavlauncher/src), я їх не бачив.
 */
public final class MinPerf {
    private MinPerf() {}

    // ---- Константи під MediaTek Dimensity 8500-Ultra (Mali-G720 MC8) ----
    public static final int HEAP_LEGACY_MODDED_MB = 3072;  // Forge 1.12.2 + ~72 моди
    public static final int HEAP_MODERN_MB = 3072;
    public static final double MAX_SHARE_OF_RAM = 0.40;
    public static final int TARGET_FPS = 120;

    /** TODO: звірити з назвами в твоєму форку. */
    public static final String RENDERER_GL4ES = "opengles2";
    public static final String RENDERER_ZINK = "vulkan_zink";

    /** Ввімкни, щоб змусити Zink для старих версій. Лишай false, поки не перевірено на телефоні. */
    public static boolean FORCE_ZINK_FOR_LEGACY = true;

    private static final String[] G1_LOW_PAUSE = {
        "-XX:+UseG1GC",
        "-XX:+UnlockExperimentalVMOptions",
        "-XX:MaxGCPauseMillis=40",
        "-XX:G1NewSizePercent=30",
        "-XX:G1MaxNewSizePercent=40",
        "-XX:G1HeapRegionSize=16M",
        "-XX:G1ReservePercent=20",
        "-XX:+ParallelRefProcEnabled"
    };

    public static boolean isMediaTek() {
        String hw = Build.HARDWARE.toLowerCase(Locale.ROOT);
        String board = Build.BOARD.toLowerCase(Locale.ROOT);
        String maker = Build.VERSION.SDK_INT >= 31 ? Build.SOC_MANUFACTURER.toLowerCase(Locale.ROOT) : "";
        return hw.startsWith("mt") || board.startsWith("mt") || maker.contains("mediatek") || maker.contains("mtk");
    }

    /** 1.12.2, 1.16.5 і старіші. */
    public static boolean isLegacy(String versionId) {
        if (versionId == null) return false;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^1\\.(\\d+)").matcher(versionId);
        return m.find() && Integer.parseInt(m.group(1)) <= 16;
    }

    /** ХУК 1 — рендерер. Викликати там, де форк вибирає Tools.LOCAL_RENDERER. */
    public static String rendererFor(String versionId, String currentRenderer) {
        if (isMediaTek() && FORCE_ZINK_FOR_LEGACY && isLegacy(versionId)) return RENDERER_ZINK;
        return currentRenderer;
    }

    /** ХУК 2 — JVM-аргументи. Додати до списку аргументів Java перед запуском. */
    public static List<String> jvmArgs(Context ctx, String versionId) {
        int wanted = isLegacy(versionId) ? HEAP_LEGACY_MODDED_MB : HEAP_MODERN_MB;
        ActivityManager am = (ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(info);
        long totalMb = info.totalMem / (1024 * 1024);
        int heap = (int) Math.max(1024, Math.min(wanted, totalMb * MAX_SHARE_OF_RAM));

        List<String> out = new ArrayList<>();
        out.add("-Xms" + heap + "M");
        out.add("-Xmx" + heap + "M");
        for (String f : G1_LOW_PAUSE) out.add(f);
        return out;
    }

    /** ХУК 3 — додаткові змінні середовища (накладаються після стандартних). */
    public static Map<String, String> extraEnv(String renderer) {
        Map<String, String> env = new LinkedHashMap<>();
        if (RENDERER_GL4ES.equals(renderer)) {
            env.put("LIBGL_MIPMAP", "3");
            env.put("LIBGL_NORMALIZE", "1");
        }
        return env;
    }

    /** Рядки для options.txt (записати перед стартом гри). */
    public static Map<String, String> optionsTxt() {
        Map<String, String> o = new LinkedHashMap<>();
        o.put("maxFps", String.valueOf(TARGET_FPS));
        o.put("enableVsync", "true");
        return o;
    }
}
