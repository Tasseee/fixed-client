package nightfall.gui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.ChunkPos;
import nightfall.analyzer.*;
import org.lwjgl.glfw.GLFW;
/** Chunk Explorer: 2D-Raster mit Zoom, Verschieben, Regionslinien und Detailansicht. Nicht analysierte Chunks bleiben grau. */
public class ChunkMap extends Screen {
  double ox, oz; int cell = 10; boolean only;
  public ChunkMap() { super(Component.literal("Chunk Explorer")); var p = Minecraft.getInstance().player; ox = p.getBlockX() / 16.0; oz = p.getBlockZ() / 16.0; }
  @Override public void render(GuiGraphics g, int mx, int my, float d) {
    var mc = Minecraft.getInstance(); int x0 = 30, y0 = 28, x1 = width - 30, y1 = height - 40, mxC = (x0 + x1) / 2, myC = (y0 + y1) / 2;
    g.fill(0, 0, width, height, 0xF0100D16); g.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, Hud.A);  g.fill(x0, y0, x1, y1, 0xFF0B0A10);
    int shownN = 0; for (Result r : OGBaseFinder.results.values()) if (OGBaseFinder.shown(r)) shownN++;
    Hud.grad(g, font, "CHUNK EXPLORER", x0, 10, Hud.A, Hud.B);
    g.drawString(font, "Treffer: " + shownN + "  |  Analysiert: " + OGBaseFinder.results.size() + "  |  Zoom " + cell + "px", x0 + 120, 10, 0xFFB8B2CC, false);
    g.enableScissor(x0, y0, x1, y1);
    int ca = (int) Math.floor(ox - (mxC - x0) / (double) cell) - 1, cb = (int) Math.ceil(ox + (x1 - mxC) / (double) cell) + 1;
    int za = (int) Math.floor(oz - (myC - y0) / (double) cell) - 1, zb = (int) Math.ceil(oz + (y1 - myC) / (double) cell) + 1;
    int gap = cell > 6 ? 1 : 0; Result hov = null; int hx = 0, hz = 0;
    int hovA = (int) Math.floor(ox + (mx - mxC) / (double) cell), hovB = (int) Math.floor(oz + (my - myC) / (double) cell);
    for (int a = ca; a <= cb; a++) for (int b = za; b <= zb; b++) {
      int sx = (int) (mxC + (a - ox) * cell), sz = (int) (myC + (b - oz) * cell);
      Result r = OGBaseFinder.results.get(ChunkPos.asLong(a, b)); int c;
      if (r != null && (!only || OGBaseFinder.shown(r))) c = OGBaseFinder.color(r);
      else if (mc.level != null && mc.level.getChunkSource().getChunkNow(a, b) != null) c = 0xFF2A2735; else c = 0xFF14121A;
      g.fill(sx, sz, sx + cell - gap, sz + cell - gap, c);
      if (Math.floorMod(a, 32) == 0) g.fill(sx - 1, sz, sx, sz + cell, 0x66FFFFFF);
      if (Math.floorMod(b, 32) == 0) g.fill(sx, sz - 1, sx + cell, sz, 0x66FFFFFF);
      if (a == hovA && b == hovB) { hov = r; hx = a; hz = b; g.fill(sx, sz, sx + cell, sz + 1, 0xFFFFFFFF); }
    }
    int px = (int) (mxC + (mc.player.getBlockX() / 16.0 - ox) * cell), pz = (int) (myC + (mc.player.getBlockZ() / 16.0 - oz) * cell);
    g.fill(px - 2, pz - 2, px + 3, pz + 3, 0xFFFFFFFF); g.fill(px - 1, pz - 1, px + 2, pz + 2, Hud.A);
    g.disableScissor();
    String[] lg = { "Unbekannt", "Niedrig", "Mittel", "Hoch", "Sehr hoch", "Decoy" }; int[] lc = { 0xFF2A2735, 0xFF22C55E, 0xFFEAB308, 0xFFF97316, 0xFFEF4444, 0xFF6B6B7B }; int lx = x0;
    for (int i = 0; i < lg.length; i++) { g.fill(lx, y1 + 10, lx + 8, y1 + 18, lc[i]); g.drawString(font, lg[i], lx + 12, y1 + 10, 0xFFB8B2CC, false); lx += font.width(lg[i]) + 28; }
    g.drawString(font, "Ziehen: Karte | Mausrad: Zoom | Rechtsklick: Koordinaten kopieren | F: nur Treffer | R: zentrieren", x0, y1 + 24, 0xFF6F6A80, false);
    if (mx >= x0 && mx < x1 && my >= y0 && my < y1) {
      java.util.List<String> t = new java.util.ArrayList<>(); t.add("Chunk " + hx + ", " + hz + "  (Block " + hx * 16 + ", " + hz * 16 + ")");
      if (hov == null) t.add("Nicht analysiert (keine Daten)");
      else { t.add("Score " + hov.score() + " | Konfidenz " + hov.conf() + "% | Beweise " + hov.evidence() + " | Abdeckung " + hov.coverage() + "%"); if (hov.decoy()) t.add("Als DECOY eingestuft"); t.addAll(hov.why()); }
      int w = 0; for (String s : t) w = Math.max(w, font.width(s)); int tx = Math.min(mx + 12, width - w - 12), ty = Math.min(my + 12, height - t.size() * 11 - 8);
      g.fill(tx - 4, ty - 4, tx + w + 4, ty + t.size() * 11 + 2, 0xEE000000);
      for (int i = 0; i < t.size(); i++) g.drawString(font, t.get(i), tx, ty + i * 11, i == 0 ? Hud.A : 0xFFE9E4F5, false);
    }
  }
  @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy) { ox -= dx / cell; oz -= dy / cell; return true; }
  @Override public boolean mouseScrolled(double mx, double my, double sx, double sy) { cell = Math.max(3, Math.min(40, cell + (int) Math.signum(sy) * 2)); return true; }
  @Override public boolean mouseClicked(MouseButtonEvent e, boolean dbl) {
    if (e.button() == 1) { int a = (int) Math.floor(ox + (e.x() - width / 2.0) / cell), b = (int) Math.floor(oz + (e.y() - (28 + height - 40) / 2.0) / cell);
      Minecraft.getInstance().keyboardHandler.setClipboard((a * 16) + " " + (b * 16)); return true; }
    return super.mouseClicked(e, dbl);
  }
  @Override public boolean keyPressed(KeyEvent e) {
    if (e.key() == GLFW.GLFW_KEY_F) { only = !only; return true; }
    if (e.key() == GLFW.GLFW_KEY_R) { var p = Minecraft.getInstance().player; ox = p.getBlockX() / 16.0; oz = p.getBlockZ() / 16.0; return true; }
    return super.keyPressed(e);
  }
  @Override public boolean isPauseScreen() { return false; }
}
