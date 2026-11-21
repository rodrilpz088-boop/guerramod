package es.guerramod.cliente.cielo;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Axis;
import es.guerramod.GuerraMod;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.DimensionSpecialEffects;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.material.FogType;
import org.joml.Matrix4f;

/**
 * Cielo realista del mundo normal: bóveda con dispersión atmosférica, sol con su color real (blanco a mediodía, naranja
 * y rojo al ponerse), halo, luna con sus fases, estrellas con la Vía Láctea y nubes que se mueven con el viento.
 */
public class CieloMundo extends DimensionSpecialEffects.OverworldEffects {
    private static final ResourceLocation LUNA = new ResourceLocation("textures/environment/moon_phases.png");
    /** Si algo falla (otro mod, sombreadores...) se vuelve al cielo normal sin romper el juego. */
    public static boolean desactivado = false;

    // malla de la bóveda: anillos de elevación (más densos cerca del horizonte) y acimut
    private static final double[] ELEV = {-90, -30, -10, -4, -1.5, 0, 1, 2.5, 4.5, 7, 10, 14, 19, 25, 32, 40, 50, 62, 76, 90};
    private static final int NA = 48;
    private static final float[] COLORES = new float[ELEV.length * (NA + 1) * 3];
    private static long ultimoCalculo = Long.MIN_VALUE;

    // estrellas: posición en la esfera (marco del cielo que gira), tamaño, brillo y color
    private static float[] estrellas;
    private static final int N_ESTRELLAS = 2600;

    public static boolean activo(ClientLevel level) {
        return !desactivado && !Shaders.activos() && level != null && level.effects() instanceof CieloMundo;
    }

    private static boolean cieloNormal(Camera cam) {
        if (cam.getFluidInCamera() != FogType.NONE) return true;
        Entity e = cam.getEntity();
        return e instanceof LivingEntity le && (le.hasEffect(MobEffects.BLINDNESS) || le.hasEffect(MobEffects.DARKNESS));
    }

    @Override
    public boolean renderSky(ClientLevel level, int ticks, float parcial, PoseStack pose, Camera cam, Matrix4f proy, boolean niebla,
                             Runnable prepararNiebla) {
        if (desactivado || Shaders.activos() || cieloNormal(cam)) return false;
        try {
            prepararNiebla.run();
            if (niebla) return true;
            Atmosfera.actualizar(level, parcial, cam.getPosition().y);
            calcularBoveda(level);
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            boveda(pose);
            RenderSystem.enableBlend();
            RenderSystem.blendFuncSeparate(com.mojang.blaze3d.platform.GlStateManager.SourceFactor.SRC_ALPHA,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.SourceFactor.ONE,
                    com.mojang.blaze3d.platform.GlStateManager.DestFactor.ZERO);
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees((float) es.guerramod.clima.Estaciones.inclinacionSol(es.guerramod.clima.TiempoCliente.dia)));
            pose.mulPose(Axis.YP.rotationDegrees(-90.0F));
            pose.mulPose(Axis.XP.rotationDegrees(level.getTimeOfDay(parcial) * 360.0F));
            estrellas(pose.last().pose(), level);
            sol(pose.last().pose());
            luna(pose.last().pose(), level);
            pose.popPose();
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            return true;
        } catch (Throwable t) {
            desactivado = true;
            GuerraMod.LOGGER.error("Cielo realista desactivado por un error; se usa el cielo normal", t);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.disableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            return false;
        }
    }

    @Override
    public boolean renderClouds(ClientLevel level, int ticks, float parcial, PoseStack pose, double camX, double camY, double camZ,
                                Matrix4f proy) {
        if (desactivado || Shaders.activos()) return false;
        try {
            Atmosfera.actualizar(level, parcial, camY);
            Nubes.dibujar(level, parcial, pose, proy, camX, camY, camZ);
            return true;
        } catch (Throwable t) {
            desactivado = true;
            GuerraMod.LOGGER.error("Nubes realistas desactivadas por un error; se usan las normales", t);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            RenderSystem.enableCull();
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();
            return false;
        }
    }

    /** Si nieva, sin sonido ni salpicaduras de lluvia. */
    @Override
    public boolean tickRain(ClientLevel level, int ticks, Camera cam) {
        return !desactivado && Lluvia.nevando();
    }

    @Override
    public boolean renderSnowAndRain(ClientLevel level, int ticks, float parcial, net.minecraft.client.renderer.LightTexture luz,
                                     double camX, double camY, double camZ) {
        if (desactivado || Shaders.activos()) return false;
        try {
            return Lluvia.dibujar(level, parcial, camX, camY, camZ);
        } catch (Throwable t) {
            desactivado = true;
            GuerraMod.LOGGER.error("Lluvia realista desactivada por un error; se usa la normal", t);
            RenderSystem.enableCull();
            RenderSystem.disableBlend();
            return false;
        }
    }

    // ---------------------------------------------------------------- bóveda
    private static double dirX(int a, int e) {
        return Math.cos(Math.toRadians(ELEV[e])) * Math.cos(2 * Math.PI * a / NA);
    }

    private static double dirY(int e) {
        return Math.sin(Math.toRadians(ELEV[e]));
    }

    private static double dirZ(int a, int e) {
        return Math.cos(Math.toRadians(ELEV[e])) * Math.sin(2 * Math.PI * a / NA);
    }

    private static void calcularBoveda(ClientLevel level) {
        long t = level.getGameTime();
        if (t == ultimoCalculo) return;
        ultimoCalculo = t;
        int iHor = 5; // anillo de 0°
        for (int e = ELEV.length - 1; e >= iHor; e--) {
            for (int a = 0; a <= NA; a++) {
                Atmosfera.colorCielo(dirX(a % NA, e), dirY(e), dirZ(a % NA, e), COLORES, (e * (NA + 1) + a) * 3);
            }
        }
        // bajo el horizonte: el color del horizonte cada vez más oscuro (suelo lejano y bruma)
        for (int e = 0; e < iHor; e++) {
            double f = Math.max(0.25, 1 + Math.sin(Math.toRadians(ELEV[e])) * 3.5);
            for (int a = 0; a <= NA; a++) {
                int d = (e * (NA + 1) + a) * 3, h = (iHor * (NA + 1) + a) * 3;
                COLORES[d] = (float) (COLORES[h] * f);
                COLORES[d + 1] = (float) (COLORES[h + 1] * f);
                COLORES[d + 2] = (float) (COLORES[h + 2] * f);
            }
        }
        // niebla: el cielo se funde con ella (más cerca del horizonte)
        float n = Atmosfera.niebla;
        if (n > 0.01f) {
            float[][] porAcimut = new float[NA + 1][];
            for (int a = 0; a <= NA; a++) porAcimut[a] = Atmosfera.colorNiebla(Math.cos(2 * Math.PI * a / NA), Math.sin(2 * Math.PI * a / NA));
            for (int e = 0; e < ELEV.length; e++) {
                double w = Math.min(1, n * 1.1) * (1 - 0.6 * Math.max(0, dirY(e)));
                for (int a = 0; a <= NA; a++) {
                    float[] f = porAcimut[a];
                    int d = (e * (NA + 1) + a) * 3;
                    for (int c = 0; c < 3; c++) COLORES[d + c] += (float) ((f[c] - COLORES[d + c]) * w);
                }
            }
        }
    }

    /** Color de la bóveda en una dirección (para mezclar las nubes lejanas con el cielo). */
    static void colorBoveda(double dx, double dy, double dz, float[] out) {
        double el = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, dy))));
        int e = 0;
        while (e < ELEV.length - 2 && ELEV[e + 1] < el) e++;
        double fe = Math.max(0, Math.min(1, (el - ELEV[e]) / (ELEV[e + 1] - ELEV[e])));
        double az = Math.atan2(dz, dx);
        if (az < 0) az += 2 * Math.PI;
        double u = az / (2 * Math.PI) * NA;
        int a = Math.min(NA - 1, (int) u);
        double fa = u - a;
        for (int c = 0; c < 3; c++) {
            double v00 = COLORES[(e * (NA + 1) + a) * 3 + c], v01 = COLORES[(e * (NA + 1) + a + 1) * 3 + c];
            double v10 = COLORES[((e + 1) * (NA + 1) + a) * 3 + c], v11 = COLORES[((e + 1) * (NA + 1) + a + 1) * 3 + c];
            out[c] = (float) ((v00 * (1 - fa) + v01 * fa) * (1 - fe) + (v10 * (1 - fa) + v11 * fa) * fe);
        }
    }

    private static void boveda(PoseStack pose) {
        Matrix4f m = pose.last().pose();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        final float R = 100;
        for (int e = 0; e < ELEV.length - 1; e++) {
            for (int a = 0; a < NA; a++) {
                vertice(bb, m, R, a, e);
                vertice(bb, m, R, a + 1, e + 1);
                vertice(bb, m, R, a + 1, e);
                vertice(bb, m, R, a, e);
                vertice(bb, m, R, a, e + 1);
                vertice(bb, m, R, a + 1, e + 1);
            }
        }
        BufferUploader.drawWithShader(bb.end());
    }

    private static void vertice(BufferBuilder bb, Matrix4f m, float r, int a, int e) {
        int i = (e * (NA + 1) + a) * 3;
        int aa = a % NA;
        bb.vertex(m, (float) (dirX(aa, e) * r), (float) (dirY(e) * r), (float) (dirZ(aa, e) * r))
                .color(COLORES[i], COLORES[i + 1], COLORES[i + 2], 1.0f).endVertex();
    }

    // ---------------------------------------------------------------- estrellas
    private static void generarEstrellas() {
        RandomSource r = RandomSource.create(10842L);
        estrellas = new float[N_ESTRELLAS * 8];
        // plano galáctico inclinado (Vía Láctea)
        double gx = 0.3, gy = 0.55, gz = 0.78;
        double gn = Math.sqrt(gx * gx + gy * gy + gz * gz);
        gx /= gn; gy /= gn; gz /= gn;
        for (int i = 0; i < N_ESTRELLAS; i++) {
            double x, y, z;
            do {
                x = r.nextDouble() * 2 - 1;
                y = r.nextDouble() * 2 - 1;
                z = r.nextDouble() * 2 - 1;
            } while (x * x + y * y + z * z > 1 || x * x + y * y + z * z < 0.01);
            double n = Math.sqrt(x * x + y * y + z * z);
            x /= n; y /= n; z /= n;
            if (i % 3 == 0) {
                // aplastar hacia el plano galáctico
                double d = x * gx + y * gy + z * gz;
                double k = 0.88;
                x -= gx * d * k; y -= gy * d * k; z -= gz * d * k;
                n = Math.sqrt(x * x + y * y + z * z);
                x /= n; y /= n; z /= n;
            }
            double mag = Math.pow(r.nextDouble(), 5.0); // pocas brillantes, muchas débiles
            float tam = (float) (0.12 + 0.32 * mag + (i % 3 == 0 ? 0 : 0.03));
            float bri = (float) (0.22 + 0.78 * Math.pow(r.nextDouble(), 2.2) * (0.4 + 0.6 * mag) + 0.5 * mag);
            // color según la temperatura de la estrella
            double tc = r.nextDouble();
            float cr, cg, cb;
            if (tc < 0.12) { cr = 0.72f; cg = 0.82f; cb = 1.0f; }
            else if (tc < 0.72) { cr = 0.95f; cg = 0.96f; cb = 1.0f; }
            else if (tc < 0.9) { cr = 1.0f; cg = 0.94f; cb = 0.8f; }
            else { cr = 1.0f; cg = 0.78f; cb = 0.58f; }
            int o = i * 8;
            estrellas[o] = (float) x; estrellas[o + 1] = (float) y; estrellas[o + 2] = (float) z;
            estrellas[o + 3] = tam; estrellas[o + 4] = Math.min(1, bri);
            estrellas[o + 5] = cr; estrellas[o + 6] = cg; estrellas[o + 7] = cb;
        }
    }

    private static void estrellas(Matrix4f m, ClientLevel level) {
        double sy = Atmosfera.sy;
        // visibles cuando el cielo oscurece; las tapan las nubes, la lluvia y la niebla
        float vis = (float) Math.max(0, Math.min(1, (-sy - 0.04) / 0.22));
        vis *= (1 - Math.min(1, Atmosfera.nubes * 1.15f)) * (1 - Atmosfera.lluvia) * (1 - Atmosfera.niebla);
        if (vis <= 0.01f) return;
        if (estrellas == null) generarEstrellas();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        final float R = 95;
        float tiempo = (level.getGameTime() % 100000) * 0.05f;
        for (int i = 0; i < N_ESTRELLAS; i++) {
            int o = i * 8;
            float x = estrellas[o], y = estrellas[o + 1], z = estrellas[o + 2];
            float tam = estrellas[o + 3];
            // parpadeo suave
            float tw = 0.8f + 0.2f * (float) Math.sin(tiempo * (1.3f + (i % 7) * 0.37f) + i);
            float a = estrellas[o + 4] * vis * tw;
            // dos vectores perpendiculares para el cuadradito
            float ux, uy, uz;
            if (Math.abs(y) < 0.9f) { ux = -z; uy = 0; uz = x; } else { ux = 0; uy = z; uz = -y; }
            float un = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
            ux /= un; uy /= un; uz /= un;
            float vx = y * uz - z * uy, vy = z * ux - x * uz, vz = x * uy - y * ux;
            float cx = x * R, cy = y * R, cz = z * R;
            float r = estrellas[o + 5], g = estrellas[o + 6], b = estrellas[o + 7];
            bb.vertex(m, cx - (ux + vx) * tam, cy - (uy + vy) * tam, cz - (uz + vz) * tam).color(r, g, b, a).endVertex();
            bb.vertex(m, cx + (ux - vx) * tam, cy + (uy - vy) * tam, cz + (uz - vz) * tam).color(r, g, b, a).endVertex();
            bb.vertex(m, cx + (ux + vx) * tam, cy + (uy + vy) * tam, cz + (uz + vz) * tam).color(r, g, b, a).endVertex();
            bb.vertex(m, cx - (ux - vx) * tam, cy - (uy - vy) * tam, cz - (uz - vz) * tam).color(r, g, b, a).endVertex();
        }
        BufferUploader.drawWithShader(bb.end());
    }

    // ---------------------------------------------------------------- sol
    private static void disco(BufferBuilder bb, Matrix4f m, float y, float radio, float r, float g, float b, float aCentro, float aBorde,
                              float radioInt) {
        final int SEG = 40;
        for (int i = 0; i < SEG; i++) {
            double a0 = 2 * Math.PI * i / SEG, a1 = 2 * Math.PI * (i + 1) / SEG;
            float x0 = (float) Math.cos(a0), z0 = (float) Math.sin(a0), x1 = (float) Math.cos(a1), z1 = (float) Math.sin(a1);
            if (radioInt <= 0) {
                bb.vertex(m, 0, y, 0).color(r, g, b, aCentro).endVertex();
                bb.vertex(m, x1 * radio, y, z1 * radio).color(r, g, b, aBorde).endVertex();
                bb.vertex(m, x0 * radio, y, z0 * radio).color(r, g, b, aBorde).endVertex();
            } else {
                // anillo
                bb.vertex(m, x0 * radioInt, y, z0 * radioInt).color(r, g, b, aCentro).endVertex();
                bb.vertex(m, x1 * radioInt, y, z1 * radioInt).color(r, g, b, aCentro).endVertex();
                bb.vertex(m, x1 * radio, y, z1 * radio).color(r, g, b, aBorde).endVertex();
                bb.vertex(m, x0 * radioInt, y, z0 * radioInt).color(r, g, b, aCentro).endVertex();
                bb.vertex(m, x1 * radio, y, z1 * radio).color(r, g, b, aBorde).endVertex();
                bb.vertex(m, x0 * radio, y, z0 * radio).color(r, g, b, aBorde).endVertex();
            }
        }
    }

    private static void sol(Matrix4f m) {
        float vis = (float) ((1 - Math.pow(Atmosfera.nubes, 1.3) * 0.93) * (1 - Atmosfera.lluvia) * (1 - Atmosfera.niebla * 0.85));
        // se esconde tras el horizonte (con la refracción se ve un poco más)
        vis *= (float) Math.max(0, Math.min(1, (Atmosfera.sy + 0.03) / 0.025));
        if (vis <= 0.01f) return;
        float[] c = Atmosfera.COLOR_SOL;
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        // halo amplio, halo cercano y disco con borde suave
        disco(bb, m, 100, 34, c[0], c[1], c[2], 0.22f * vis, 0, 0);
        disco(bb, m, 100, 11, c[0], c[1], c[2], 0.45f * vis, 0, 0);
        disco(bb, m, 100, 4.2f, c[0], c[1], c[2], 1.0f * vis, 1.0f * vis, 0);
        disco(bb, m, 100, 5.0f, c[0], c[1], c[2], 1.0f * vis, 0f, 4.2f);
        BufferUploader.drawWithShader(bb.end());
    }

    // ---------------------------------------------------------------- luna
    private static void luna(Matrix4f m, ClientLevel level) {
        float vis = (float) ((1 - Math.pow(Atmosfera.nubes, 1.3) * 0.9) * (1 - Atmosfera.lluvia) * (1 - Atmosfera.niebla * 0.8));
        vis *= (float) Math.max(0, Math.min(1, (Atmosfera.my + 0.03) / 0.03));
        // de día la luna se ve pálida
        vis *= (float) (0.35 + 0.65 * Math.max(0, Math.min(1, (-Atmosfera.sy + 0.05) / 0.2)));
        if (vis <= 0.01f) return;
        float[] c = Atmosfera.COLOR_LUNA;
        int fase = level.getMoonPhase();
        int l = fase % 4, i1 = fase / 4 % 2;
        float u0 = l / 4.0f, v0 = i1 / 2.0f, u1 = (l + 1) / 4.0f, v1 = (i1 + 1) / 2.0f;
        float t = 11.0f;
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, LUNA);
        RenderSystem.setShaderColor(c[0], c[1], c[2], vis);
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        bb.vertex(m, -t, -100.0F, t).uv(u1, v1).endVertex();
        bb.vertex(m, t, -100.0F, t).uv(u0, v1).endVertex();
        bb.vertex(m, t, -100.0F, -t).uv(u0, v0).endVertex();
        bb.vertex(m, -t, -100.0F, -t).uv(u1, v0).endVertex();
        BufferUploader.drawWithShader(bb.end());
        RenderSystem.setShaderColor(1, 1, 1, 1);
        // halo lunar tenue
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        bb.begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION_COLOR);
        disco(bb, m, -100, 26, c[0] * 0.7f, c[1] * 0.75f, c[2] * 0.85f, 0.12f * vis * Atmosfera.luna, 0, 0);
        BufferUploader.drawWithShader(bb.end());
    }
}
