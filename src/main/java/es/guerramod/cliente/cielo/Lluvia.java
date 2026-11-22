package es.guerramod.cliente.cielo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.longs.Long2ByteOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Lluvia realista: miles de gotas finas y casi transparentes que caen a unos 9 m/s (cada una deja un trazo corto, como
 * se ve con los ojos), inclinadas por el viento, con más densidad en las tormentas y una cortina de lluvia más difusa a
 * lo lejos. Las gotas están fijas al mundo (no se mueven contigo), se paran en el suelo, en los tejados y en los árboles,
 * y se iluminan según la hora, la luz del cielo y los relámpagos. En zonas de nieve se usa la nevada normal.
 */
public final class Lluvia {
    private Lluvia() {}

    /** Tipo de precipitación por chunk (0 nada, 1 lluvia, 2 nieve); se renueva cada pocos segundos. */
    private static final Long2ByteOpenHashMap PRECIP = new Long2ByteOpenHashMap();
    private static long limpiado = 0;

    private static double hash(long x, long z, int j) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ (j + 1L) * 0x165667B19E3779F9L;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 29);
        return (h >>> 11) * 0x1.0p-53;
    }

    static byte precip(ClientLevel level, int x, int y, int z) {
        long k = (((long) (x >> 4)) << 32) ^ ((z >> 4) & 0xFFFFFFFFL);
        byte v = PRECIP.getOrDefault(k, (byte) -1);
        if (v < 0) {
            BlockPos p = new BlockPos((x & ~15) + 8, y, (z & ~15) + 8);
            Biome.Precipitation pr = level.getBiome(p).value().getPrecipitationAt(p);
            v = (byte) (pr == Biome.Precipitation.RAIN ? 1 : pr == Biome.Precipitation.SNOW ? 2 : 0);
            PRECIP.put(k, v);
        }
        return v;
    }

    /** true si la ha dibujado; false para dejar la lluvia o nieve normal. */
    public static boolean dibujar(ClientLevel level, float parcial, double cx, double cy, double cz) {
        float lluvia = level.getRainLevel(parcial);
        if (lluvia <= 0.001f) return false;
        if (level.getGameTime() - limpiado > 60 || PRECIP.size() > 4096) {
            PRECIP.clear();
            limpiado = level.getGameTime();
        }
        int bx = (int) Math.floor(cx), bz = (int) Math.floor(cz), by = (int) Math.floor(cy);
        if (precip(level, bx, by, bz) == 2) return false; // nieve: la de siempre
        float tormenta = level.getThunderLevel(parcial);
        double intensidad = lluvia * (0.55 + 0.45 * tormenta);
        double t = (level.getGameTime() % 2400000L + parcial) / 20.0;

        // viento: las gotas caen inclinadas y derivan
        double ang = Math.toRadians(es.guerramod.incendios.Clima.clienteAngulo);
        double vv = es.guerramod.incendios.Clima.clienteVelocidad / 3.6 * 0.7;
        double wx = Math.cos(ang) * vv, wz = Math.sin(ang) * vv;

        // luz: hora del día, luz del cielo donde estás y relámpagos
        double dia = Math.max(0, Math.min(1, (Atmosfera.sy + 0.08) / 0.35));
        int cielo = level.getBrightness(LightLayer.SKY, BlockPos.containing(cx, cy + 1, cz));
        double luz = (0.12 + 0.88 * dia) * (0.35 + 0.65 * cielo / 15.0);
        if (level.getSkyFlashTime() > 0) luz = Math.min(1.6, luz + 1.0);
        float cr = (float) Math.min(1, 0.70 * luz + 0.06), cg = (float) Math.min(1, 0.74 * luz + 0.07), cb = (float) Math.min(1, 0.80 * luz + 0.09);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        if (nevando()) {
            // con frío la lluvia es nieve: copos que caen despacio, se mecen y derivan con el viento
            copos(level, bb, bx, by, bz, cx, cy, cz, intensidad, t, wx, wz, Math.min(1, cr * 1.25f + 0.08f), Math.min(1, cg * 1.25f + 0.08f),
                    Math.min(1, cb * 1.2f + 0.1f));
            BufferUploader.drawWithShader(bb.end());
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            return true;
        }
        // gotas cercanas: columnas de 1 bloque hasta 15 de distancia
        capa(level, bb, bx, by, bz, cx, cy, cz, 15, 1, 0, 2 + (int) Math.round(3 * intensidad), intensidad, t, wx, wz, cr, cg, cb,
                0.58, 0.010, 0.0022, 0.30);
        // cortina lejana: más difusa
        capa(level, bb, bx, by, bz, cx, cy, cz, 44, 2, 15, 1 + (int) Math.round(2 * intensidad), intensidad, t, wx, wz, cr, cg, cb,
                1.25, 0.03, 0.0035, 0.13);

        BufferUploader.drawWithShader(bb.end());
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        return true;
    }

    /** ¿Nieva aquí? (frío suficiente según el tiempo que manda el servidor). */
    public static boolean nevando() {
        return es.guerramod.clima.TiempoCliente.recibido && es.guerramod.clima.TiempoCliente.temperatura < 1.0f;
    }

    private static void copos(ClientLevel level, BufferBuilder bb, int bx, int by, int bz, double cx, double cy, double cz, double intensidad,
                              double t, double wx, double wz, float cr, float cg, float cb) {
        final int radio = 18;
        final double span = 28, lo = cy - 10;
        for (int dx = -radio; dx <= radio; dx++) {
            for (int dz = -radio; dz <= radio; dz++) {
                if (dx * dx + dz * dz > radio * radio) continue;
                int x = bx + dx, z = bz + dz;
                if (precip(level, x, by, z) == 0) continue;
                int suelo = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                float vent = es.guerramod.clima.TiempoCliente.ventisca;
                int n = 1 + (int) Math.round(3 * intensidad) + (int) Math.round(4 * vent);
                for (int j = 0; j < n; j++) {
                    if (hash(x, z, j + 140) > 0.4 + 0.6 * intensidad + vent) continue;
                    double vel = 0.9 + 0.7 * hash(x, z, j + 120);
                    double fase = hash(x, z, j + 130) * span;
                    double yAbs = -(t * vel + fase);
                    double y = lo + Math.floorMod((long) Math.floor((yAbs - lo) * 64), (long) (span * 64)) / 64.0;
                    if (y < suelo) continue;
                    double balanceo = Math.sin(t * (1.1 + hash(x, z, j + 150)) + j * 2.1) * 0.35;
                    // con ventisca la nieve viaja casi en horizontal
                    double arrastre = 0.015 * (1 + 14 * vent);
                    double px = x + hash(x, z, j + 100) + balanceo + wx * arrastre * (y - suelo) / vel;
                    double pz = z + hash(x, z, j + 110) + Math.cos(t * 0.9 + j) * 0.25 + wz * arrastre * (y - suelo) / vel;
                    double rx = px - cx, ry = y - cy, rz = pz - cz;
                    double dist = Math.sqrt(rx * rx + ry * ry + rz * rz);
                    if (dist < 0.4) continue;
                    double tam = 0.035 + 0.03 * hash(x, z, j + 160);
                    // cuadradito mirando a la cámara
                    double hx = -rz / dist, hz = rx / dist;
                    double ux = -hx * ry / dist * 0, uy = 1.0, uz = 0;
                    float a = (float) ((0.55 + 0.35 * hash(x, z, j + 170)) * (1 - Math.min(1, dist / (radio + 1.0))) * Math.sqrt(intensidad));
                    if (a < 0.02f) continue;
                    bb.vertex(rx - hx * tam, ry - uy * tam, rz - hz * tam).color(cr, cg, cb, a).endVertex();
                    bb.vertex(rx + hx * tam, ry - uy * tam, rz + hz * tam).color(cr, cg, cb, a).endVertex();
                    bb.vertex(rx + hx * tam, ry + uy * tam, rz + hz * tam).color(cr, cg, cb, a).endVertex();
                    bb.vertex(rx - hx * tam, ry + uy * tam, rz - hz * tam).color(cr, cg, cb, a).endVertex();
                }
            }
        }
    }

    private static void capa(ClientLevel level, BufferBuilder bb, int bx, int by, int bz, double cx, double cy, double cz, int radio, int paso,
                             int radioMin, int gotasMax, double intensidad, double t, double wx, double wz, float cr, float cg, float cb,
                             double largo, double ancho0, double anchoPorDist, double alfa0) {
        final double span = radio > 20 ? 60 : 34;
        final double lo = cy - (radio > 20 ? 20 : 12);
        int r2 = radio * radio, rm2 = radioMin * radioMin;
        int x0 = Math.floorDiv(bx, paso) * paso, z0 = Math.floorDiv(bz, paso) * paso;
        for (int dx = -radio; dx <= radio; dx += paso) {
            for (int dz = -radio; dz <= radio; dz += paso) {
                int d2 = dx * dx + dz * dz;
                if (d2 > r2 || d2 < rm2) continue;
                int x = x0 + dx, z = z0 + dz;
                if (precip(level, x, by, z) != 1) continue;
                int suelo = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                for (int j = 0; j < gotasMax; j++) {
                    if (hash(x, z, j + 40) > 0.35 + 0.65 * intensidad) continue;
                    double px = x + hash(x, z, j) * paso, pz = z + hash(x, z, j + 10) * paso;
                    double vel = 8.0 + 1.6 * hash(x, z, j + 20);
                    double fase = hash(x, z, j + 30) * span;
                    double yAbs = -(t * vel + fase);
                    double y = lo + Math.floorMod((long) Math.floor((yAbs - lo) * 64), (long) (span * 64)) / 64.0;
                    if (y < suelo) continue;
                    double l = largo * (0.8 + 0.4 * hash(x, z, j + 50));
                    double yb = y, yt = y + l;
                    // vector de caída (con el viento) y punto de abajo
                    double ix = -wx / vel * l, iz = -wz / vel * l;
                    double bxp = px - cx, byp = yb - cy, bzp = pz - cz;
                    double txp = bxp + ix, typ = yt - cy, tzp = bzp + iz;
                    double dist = Math.sqrt(bxp * bxp + byp * byp + bzp * bzp);
                    if (dist < 0.6) continue;
                    // lado del trazo, perpendicular a la gota y a la mirada
                    double ax = txp - bxp, ay = typ - byp, az = tzp - bzp;
                    double sx = ay * bzp - az * byp, sy = az * bxp - ax * bzp, sz = ax * byp - ay * bxp;
                    double sn = Math.sqrt(sx * sx + sy * sy + sz * sz);
                    if (sn < 1e-6) continue;
                    double w = (ancho0 + anchoPorDist * dist) * 0.5 / sn;
                    sx *= w; sy *= w; sz *= w;
                    double desvanecer = 1 - Math.min(1, dist / (radio + 1.0));
                    float a = (float) ((alfa0 + 0.15 * hash(x, z, j + 60)) * Math.sqrt(intensidad) * (0.35 + 0.65 * desvanecer));
                    if (a < 0.01f) continue;
                    float ab = a, at = a * 0.15f;
                    bb.vertex(bxp - sx, byp - sy, bzp - sz).color(cr, cg, cb, ab).endVertex();
                    bb.vertex(bxp + sx, byp + sy, bzp + sz).color(cr, cg, cb, ab).endVertex();
                    bb.vertex(txp + sx, typ + sy, tzp + sz).color(cr, cg, cb, at).endVertex();
                    bb.vertex(txp - sx, typ - sy, tzp - sz).color(cr, cg, cb, at).endVertex();
                }
            }
        }
    }
}
