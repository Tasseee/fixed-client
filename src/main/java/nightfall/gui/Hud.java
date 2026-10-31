package nightfall.gui;
import java.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import nightfall.core.*;
import nightfall.core.Module;
public final class Hud {
  public static final int A = 0xFFA855F7, B = 0xFF6D28D9;
  public static int lerp(int a, int b, float t) {
    int r = 0; for (int s = 24; s >= 0; s -= 8) { int x = (a >> s) & 255, y = (b >> s) & 255; r |= ((int) (x + (y - x) * t) & 255) << s; } return r;
  }
  public static void grad(GuiGraphics g, Font f, String s, int x, int y, int c1, int c2) {
    for (int i = 0; i < s.length(); i++) { String ch = s.substring(i, i + 1);
      g.drawString(f, ch, x, y, lerp(c1, c2, i / (float) Math.max(1, s.length() - 1)), true); x += f.width(ch); }
  }
  public static void render(GuiGraphics g, DeltaTracker dt) {
    var mc = Minecraft.getInstance();
    if (mc.options.hideGui || !Modules.HUD.enabled || mc.player == null) return;
    Font f = mc.font;
    // Streamer/Privacy mode only changes the local HUD; it does not hide mods from a server.
    boolean privacy = Modules.OPSEC != null && Modules.OPSEC.enabled;
    boolean hideFps = privacy && Modules.OPSEC.on("Hide FPS");
    boolean hideCoords = privacy && Modules.OPSEC.on("Hide Coordinates");
    boolean hideModuleList = privacy && Modules.OPSEC.on("Hide Module List");
    // Watermark
    String info = hideFps ? "" : " | " + mc.getFps() + " FPS";
    int w = f.width("NIGHTFALL" + info) + 12;
    g.fill(4, 4, 4 + w, 20, 0xA0100C18);
    for (int i = 0; i < w; i++) g.fill(4 + i, 4, 5 + i, 6, lerp(A, B, i / (float) w));
    grad(g, f, "NIGHTFALL", 10, 9, A, B); g.drawString(f, info, 10 + f.width("NIGHTFALL"), 9, 0xFFB8B2CC, true);
    // Koordinaten
    if (!hideCoords) {
      String c = String.format("XYZ  %.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ());
      g.fill(4, mc.getWindow().getGuiScaledHeight() - 18, 4 + f.width(c) + 10, mc.getWindow().getGuiScaledHeight() - 4, 0xA0100C18);
      g.drawString(f, c, 9, mc.getWindow().getGuiScaledHeight() - 14, 0xFFE9E4F5, true);
    }
    // Modulliste
    if (hideModuleList) return;
    List<Module> on = new ArrayList<>();
    for (Module m : Modules.ALL) if (m.enabled && m != Modules.HUD && m != Modules.GUI) on.add(m);
    on.sort(Comparator.comparingInt((Module m) -> f.width(m.name)).reversed());
    int sw = mc.getWindow().getGuiScaledWidth(), y = 4, i = 0;
    for (Module m : on) { int tw = f.width(m.name) + 8;
      g.fill(sw - tw - 6, y, sw - 2, y + 12, 0xA0100C18); g.fill(sw - 2, y, sw, y + 12, lerp(A, B, i / (float) Math.max(1, on.size())));
      g.drawString(f, m.name, sw - tw - 2, y + 2, lerp(A, 0xFFE9D5FF, 0.4f), true); y += 12; i++; }
  }
}
