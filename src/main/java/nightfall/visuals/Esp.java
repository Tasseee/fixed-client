package nightfall.visuals;
import java.util.*;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.phys.AABB;
import nightfall.core.*;
import nightfall.analyzer.FakeSpawnerFilter;
import nightfall.gui.Hud;
import nightfall.core.Module;
/** 2D-Projektions-ESP: rechnet Weltpunkte selbst auf den Bildschirm um (kein Eingriff in die 3D-Render-Pipeline). */
public final class Esp {
  record Hit(AABB box, int type) {}
  static volatile List<Hit> storage = List.of();
  static volatile List<Hit> spawners = List.of();
  static final EntityType<?>[] ET = { EntityType.VILLAGER, EntityType.WANDERING_TRADER, EntityType.ITEM_FRAME, EntityType.GLOW_ITEM_FRAME, EntityType.ARMOR_STAND, EntityType.MINECART, EntityType.ALLAY };
  static final String[] EN = { "Villager", "Wandering Trader", "Item Frame", "Glow Item Frame", "Armor Stand", "Minecart", "Allay" };
  static final String[] SN = { "Chest", "Trapped Chest", "Barrel", "Shulker Box", "Ender Chest" };
  static final int[] SC = { 0xFFE8A33D, 0xFFE5484D, 0xFF9C6B3C, 0xFFE879F9, 0xFF22D3EE };
  static double cx, cy, cz, fx, fy, fz, rx, rz, ux, uy, uz, th, asp; static int W, H, tracers;

  public static void tick(Minecraft mc) {
    boolean scanStorage = Modules.STORAGE.enabled;
    boolean scanSpawners = Modules.SPAWNER.enabled;
    if (mc.level == null || mc.player == null || (!scanStorage && !scanSpawners)) { storage = List.of(); spawners = List.of(); return; }
    if (mc.player.tickCount % 20 != 0) return;
    int range = Math.max(scanStorage ? Modules.STORAGE.s("Range").i() : 0, scanSpawners ? Modules.SPAWNER.s("Range").i() : 0);
    int r = (int) Math.ceil(range / 16.0), pcx = mc.player.getBlockX() >> 4, pcz = mc.player.getBlockZ() >> 4;
    List<Hit> out = new ArrayList<>(), spawnerOut = new ArrayList<>();
    for (int a = -r; a <= r; a++) for (int b = -r; b <= r; b++) {
      var ch = mc.level.getChunkSource().getChunkNow(pcx + a, pcz + b); if (ch == null) continue;
      for (var be : ch.getBlockEntities().values()) {
        var t = be.getType();
        if (scanStorage) {
          int k = t == BlockEntityType.CHEST ? 0 : t == BlockEntityType.TRAPPED_CHEST ? 1 : t == BlockEntityType.BARREL ? 2 : t == BlockEntityType.SHULKER_BOX ? 3 : t == BlockEntityType.ENDER_CHEST ? 4 : -1;
          if (k >= 0 && Modules.STORAGE.on(SN[k])) out.add(new Hit(new AABB(be.getBlockPos()), k));
        }
        if (scanSpawners && t == BlockEntityType.MOB_SPAWNER && !FakeSpawnerFilter.isKnownFakeSpawner(mc.level, be.getBlockPos()))
          spawnerOut.add(new Hit(new AABB(be.getBlockPos()), 0));
      }
    }
    storage = out;
    spawners = spawnerOut;
  }

  static void setup(Minecraft mc, float pt) {
    var p = mc.player;
    if (Cam.freecam) { cx = Cam.x; cy = Cam.y; cz = Cam.z; } else { var v = p.getEyePosition(pt); cx = v.x; cy = v.y; cz = v.z; }
    float yaw = Cam.active() ? Cam.yaw : p.getViewYRot(pt), pitch = Cam.active() ? Cam.pitch : p.getViewXRot(pt);
    double yr = Math.toRadians(yaw), pr = Math.toRadians(pitch);
    fx = -Math.sin(yr) * Math.cos(pr); fy = -Math.sin(pr); fz = Math.cos(yr) * Math.cos(pr);
    rx = -Math.cos(yr); rz = -Math.sin(yr);
    ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;
    th = Math.tan(Math.toRadians(mc.options.fov().get()) / 2.0);
    W = mc.getWindow().getGuiScaledWidth(); H = mc.getWindow().getGuiScaledHeight(); asp = (double) W / H; tracers = 0;
  }
  static double[] proj(double x, double y, double z) {
    double dx = x - cx, dy = y - cy, dz = z - cz, zc = dx * fx + dy * fy + dz * fz;
    if (zc < 0.05) return null;
    double xc = dx * rx + dz * rz, yc = dx * ux + dy * uy + dz * uz;
    return new double[] { W / 2.0 + xc / (zc * th * asp) * W / 2.0, H / 2.0 - yc / (zc * th) * H / 2.0 };
  }
  static double[] box2d(AABB b) {
    double x1 = 1e9, y1 = 1e9, x2 = -1e9, y2 = -1e9;
    for (int i = 0; i < 8; i++) {
      double[] s = proj((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ);
      if (s == null) return null;
      x1 = Math.min(x1, s[0]); y1 = Math.min(y1, s[1]); x2 = Math.max(x2, s[0]); y2 = Math.max(y2, s[1]);
    }
    return new double[] { x1, y1, x2, y2 };
  }
  static void line(GuiGraphics g, double x1, double y1, double x2, double y2, int c) {
    double dx = x2 - x1, dy = y2 - y1; int n = (int) Math.min(2500, Math.max(Math.abs(dx), Math.abs(dy)));
    for (int i = 0; i <= n; i++) { int x = (int) (x1 + dx * i / Math.max(n, 1)), y = (int) (y1 + dy * i / Math.max(n, 1)); g.fill(x, y, x + 1, y + 1, c); }
  }
  static void draw(GuiGraphics g, Font f, AABB b, int col, boolean box, String label, boolean tracer, double hp) {
    double[] r = box2d(b); if (r == null) return;
    int x1 = (int) r[0], y1 = (int) r[1], x2 = (int) r[2], y2 = (int) r[3];
    if (x2 < 0 || x1 > W || y2 < 0 || y1 > H) return;
    if (box) { g.fill(x1, y1, x2, y1 + 1, col); g.fill(x1, y2, x2, y2 + 1, col); g.fill(x1, y1, x1 + 1, y2, col); g.fill(x2, y1, x2 + 1, y2 + 1, col); }
    if (hp >= 0) { int h = (int) ((y2 - y1) * hp); g.fill(x1 - 4, y1, x1 - 2, y2, 0xAA000000); g.fill(x1 - 4, y2 - h, x1 - 2, y2, Hud.lerp(0xFFEF4444, 0xFF22C55E, (float) hp)); }
    if (label != null && !label.isEmpty()) {
      int w = f.width(label), lx = (x1 + x2) / 2 - w / 2; g.fill(lx - 2, y1 - 12, lx + w + 2, y1 - 1, 0xA0000000); g.drawString(f, label, lx, y1 - 10, 0xFFFFFFFF, false);
    }
    if (tracer && tracers++ < 24) line(g, W / 2.0, H, (x1 + x2) / 2.0, y2, col & 0xB0FFFFFF | 0x40000000);
  }

  public static void render(GuiGraphics g, DeltaTracker dt) {
    var mc = Minecraft.getInstance();
    if (mc.player == null || mc.level == null || mc.options.hideGui) return;
    nightfall.core.Module P = Modules.PESP, E = Modules.EESP, S = Modules.STORAGE;
    if (!P.enabled && !E.enabled && !S.enabled && !Modules.SPAWNER.enabled && !Modules.BASE.enabled) return;
    float pt = dt.getGameTimeDeltaPartialTick(false); setup(mc, pt); Font f = mc.font;
    for (Entity e : mc.level.entitiesForRendering()) {
      if (e == mc.player) continue;
      var off = e.getPosition(pt).subtract(e.position()); AABB bb = e.getBoundingBox().move(off);
      double dist = Math.sqrt(bb.getCenter().distanceToSqr(cx, cy, cz));
      if (P.enabled && e.getType() == EntityType.PLAYER && dist <= P.s("Range").d()) {
        String l = (P.on("Name") ? e.getName().getString() : "") + (P.on("Distance") ? String.format(" %.0fm", dist) : "");
        double hp = P.on("Health") && e instanceof LivingEntity le ? Math.max(0, Math.min(1, le.getHealth() / le.getMaxHealth())) : -1;
        draw(g, f, bb, P.s("Color").rgb(), P.on("Box"), l.trim(), P.on("Tracer"), hp);
      } else if (E.enabled && dist <= E.s("Range").d()) {
        for (int i = 0; i < ET.length; i++) if (e.getType() == ET[i] && E.on(EN[i]))
          draw(g, f, bb, E.s("Color").rgb(), E.on("Box"), E.on("Name") ? EN[i] : "", E.on("Tracer"), -1);
      }
    }
    if (S.enabled) for (Hit h : storage) {
      if (Math.sqrt(h.box.getCenter().distanceToSqr(cx, cy, cz)) > S.s("Range").d()) continue;
      draw(g, f, h.box, SC[h.type], S.on("Box"), "", S.on("Tracer"), -1);
    }
    nightfall.core.Module SP = Modules.SPAWNER;
    if (SP.enabled) for (Hit h : spawners) {
      if (Math.sqrt(h.box.getCenter().distanceToSqr(cx, cy, cz)) > SP.s("Range").d()) continue;
      draw(g, f, h.box, SP.s("Color").rgb(), SP.on("Box"), "Spawner", SP.on("Tracer"), -1);
    }
    SusOverlay.draw(g, f, mc);
  }
}
