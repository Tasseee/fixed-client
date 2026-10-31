package nightfall.visuals;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import nightfall.analyzer.*;
import nightfall.core.Modules;
/** Zeichnet auffaellige Chunks als Marker mit Label (nutzt die Projektion aus Esp). */
public final class SusOverlay {
  public static void draw(GuiGraphics g, Font f, Minecraft mc) {
    var m = Modules.BASE; if (!m.enabled || !m.on("Overlay")) return;
    for (Result r : OGBaseFinder.results.values()) {
      if (!OGBaseFinder.shown(r)) continue;
      double d = Math.sqrt((r.ax() - Esp.cx) * (r.ax() - Esp.cx) + (r.ay() - Esp.cy) * (r.ay() - Esp.cy) + (r.az() - Esp.cz) * (r.az() - Esp.cz));
      double[] s = Esp.proj(r.ax(), r.ay(), r.az()); if (s == null) continue;
      int x = (int) s[0], y = (int) s[1], c = OGBaseFinder.color(r);
      String l = (r.decoy() ? "DECOY " : "SUS " + r.score() + " ") + String.format("%.0fm", d); int w = f.width(l);
      g.fill(x - 4, y - 4, x + 5, y + 5, 0xFF000000); g.fill(x - 3, y - 3, x + 4, y + 4, c);
      g.fill(x - w / 2 - 2, y - 17, x + w / 2 + 3, y - 6, 0xB0000000); g.drawString(f, l, x - w / 2, y - 15, c, false);
    }
  }
}
