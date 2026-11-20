package es.guerramod.cliente.cielo;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import es.guerramod.GuerraMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.Random;

/**
 * Nubes: dos capas (cúmulos a 200 m y cirros altos) hechas con ruido fractal que se mueven con el viento. La cantidad de
 * nubes viene del servidor (nubosidad de la zona), se iluminan con el color real del sol (blancas a mediodía,
 * naranjas y rosas al atardecer, grises con lluvia) y a lo lejos se funden con el cielo.
 */
public final class Nubes {
    private Nubes() {}

    private static final int T = 256;
    private static final ResourceLocation LOC_CUM = new ResourceLocation(GuerraMod.MODID, "dinamica/cumulos");
    private static final ResourceLocation LOC_CIR = new ResourceLocation(GuerraMod.MODID, "dinamica/cirros");
    private static DynamicTexture texCum, texCir;
    private static float[] ruidoCum, ruidoCir;
    private static float hechaCob = -1, hechaLluvia = -1;
    private static double offX, offZ, offCx, offCz;
    private static long ultimoNs;
    private static VertexBuffer vb;

    // ---------------------------------------------------------------- ruido fractal periódico (se repite sin costuras)
    private static float[] ruido(long semilla, int[][] periodos, float[] amps) {
        float[] r = new float[T * T];
        Random rnd = new Random(semilla);
        for (int o = 0; o < periodos.length; o++) {
            int px = periodos[o][0], py = periodos[o][1];
            float[] red = new float[px * py];
            for (int i = 0; i < red.length; i++) red[i] = rnd.nextFloat();
            for (int y = 0; y < T; y++) {
                double fy = (double) y * py / T;
                int iy = (int) Math.floor(fy);
                double ty = fy - iy;
                ty = ty * ty * (3 - 2 * ty);
                int y0 = Math.floorMod(iy, py), y1 = Math.floorMod(iy + 1, py);
                for (int x = 0; x < T; x++) {
                    double fx = (double) x * px / T;
                    int ix = (int) Math.floor(fx);
                    double tx = fx - ix;
                    tx = tx * tx * (3 - 2 * tx);
                    int x0 = Math.floorMod(ix, px), x1 = Math.floorMod(ix + 1, px);
                    double a = red[y0 * px + x0] * (1 - tx) + red[y0 * px + x1] * tx;
                    double b = red[y1 * px + x0] * (1 - tx) + red[y1 * px + x1] * tx;
                    r[y * T + x] += (float) ((a * (1 - ty) + b * ty) * amps[o]);
                }
            }
        }
        // ecualizar: así "cobertura 40 %" son exactamente el 40 % de los píxeles
        Integer[] idx = new Integer[T * T];
        for (int i = 0; i < idx.length; i++) idx[i] = i;
        final float[] rr = r;
        Arrays.sort(idx, (p, q) -> Float.compare(rr[p], rr[q]));
        float[] eq = new float[T * T];
        for (int k = 0; k < idx.length; k++) eq[idx[k]] = (float) k / (idx.length - 1);
        return eq;
    }

    private static void iniciar() {
        if (texCum != null) return;
        ruidoCum = ruido(7781L, new int[][]{{4, 4}, {8, 8}, {16, 16}, {32, 32}, {64, 64}}, new float[]{1f, 0.55f, 0.3f, 0.16f, 0.08f});
        ruidoCir = ruido(1193L, new int[][]{{2, 10}, {4, 22}, {8, 44}, {16, 90}}, new float[]{1f, 0.6f, 0.35f, 0.2f});
        texCum = new DynamicTexture(T, T, false);
        texCir = new DynamicTexture(T, T, false);
        Minecraft.getInstance().getTextureManager().register(LOC_CUM, texCum);
        Minecraft.getInstance().getTextureManager().register(LOC_CIR, texCir);
    }

    private static float suave(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    private static void rehacer(float cob, float lluvia) {
        NativeImage img = texCum.getPixels();
        NativeImage ci = texCir.getPixels();
        if (img == null || ci == null) return;
        float um = 1 - cob;
        for (int y = 0; y < T; y++) {
            for (int x = 0; x < T; x++) {
                float n = ruidoCum[y * T + x];
                float a = suave(um - 0.02f, um + 0.13f, n);
                float grosor = Math.max(0, Math.min(1, (n - um) / Math.max(0.2f, 1 - um)));
                float gris = (float) (1 - 0.52 * Math.pow(grosor, 0.75) * (0.55 + 0.45 * cob)) * (1 - 0.3f * lluvia);
                int g = (int) (Math.max(0, Math.min(1, gris)) * 255);
                int al = (int) (a * 0.96f * 255);
                img.setPixelRGBA(x, y, (al << 24) | (g << 16) | (g << 8) | g);
                float m = ruidoCir[y * T + x];
                float ac = suave(0.55f, 0.95f, m) * 0.7f;
                int alc = (int) (ac * 255);
                ci.setPixelRGBA(x, y, (alc << 24) | 0xF2F2F2);
            }
        }
        texCum.bind();
        img.upload(0, 0, 0, 0, 0, T, T, true, false, false, false);
        texCir.bind();
        ci.upload(0, 0, 0, 0, 0, T, T, true, false, false, false);
        hechaCob = cob;
        hechaLluvia = lluvia;
    }

    public static void dibujar(ClientLevel level, float parcial, PoseStack pose, Matrix4f proy, double camX, double camY, double camZ) {
        iniciar();
        float cob = Atmosfera.nubes;
        float lluvia = Atmosfera.lluvia;
        if (Math.abs(cob - hechaCob) > 0.012f || Math.abs(lluvia - hechaLluvia) > 0.05f) rehacer(cob, lluvia);

        // el viento mueve las nubes (en altura sopla más fuerte)
        long nt = System.nanoTime();
        double dt = ultimoNs == 0 ? 0 : Math.min(0.25, (nt - ultimoNs) / 1e9);
        ultimoNs = nt;
        double ang = Math.toRadians(es.guerramod.incendios.Clima.clienteAngulo);
        double v = (es.guerramod.incendios.Clima.clienteVelocidad / 3.6) * 1.8 + 1.0;
        offX -= Math.cos(ang) * v * dt;
        offZ -= Math.sin(ang) * v * dt;
        offCx -= Math.cos(ang) * v * 2.5 * dt;
        offCz -= Math.sin(ang) * v * 2.5 * dt;

        if (vb == null) vb = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1, 1, 1, 1);
        float niebla = Atmosfera.niebla;
        // cirros: velo alto de buen tiempo (desaparecen con el cielo cubierto)
        float aCir = (1 - Math.min(1, cob * 1.3f)) * 0.85f * (1 - niebla);
        if (aCir > 0.02f) capa(LOC_CIR, 420, 5200, offCx, offCz, aCir, pose, proy, camX, camY, camZ);
        if (cob > 0.01f) capa(LOC_CUM, 205, 2300, offX, offZ, 1 - niebla * 0.9f, pose, proy, camX, camY, camZ);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    }

    private static void capa(ResourceLocation tex, double altura, double escala, double ox, double oz, float alfa, PoseStack pose,
                             Matrix4f proy, double camX, double camY, double camZ) {
        float lejos = Minecraft.getInstance().gameRenderer.getDepthFar();
        double R = Math.min(3000, lejos * 0.92);
        double hy = altura - camY;
        if (Math.abs(hy) < 1) hy = hy < 0 ? -1 : 1;
        double bu = (camX + ox) / escala, bv = (camZ + oz) / escala;
        bu -= Math.floor(bu);
        bv -= Math.floor(bv);
        final int N = 24;
        BufferBuilder bb = Tesselator.getInstance().getBuilder();
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        float[] col = new float[3], cielo = new float[3];
        float[][] fila = new float[N + 1][];
        float[][] filaAnt = null;
        for (int j = 0; j <= N; j++) {
            fila = new float[N + 1][];
            double z = -R + 2 * R * j / N;
            for (int i = 0; i <= N; i++) {
                double x = -R + 2 * R * i / N;
                double d = Math.sqrt(x * x + z * z);
                double dl = Math.sqrt(d * d + hy * hy);
                double dx = x / dl, dy = hy / dl, dz = z / dl;
                Atmosfera.colorNube(dx, dy, dz, col, 0);
                CieloMundo.colorBoveda(dx, Math.max(dy, 0.02), dz, cielo);
                // perspectiva aérea: a lo lejos las nubes se funden con el cielo
                float fa = (float) (1 - Math.exp(-dl / 2600.0)) * 0.85f;
                float r = col[0] + (cielo[0] - col[0]) * fa, g = col[1] + (cielo[1] - col[1]) * fa, b = col[2] + (cielo[2] - col[2]) * fa;
                float borde = 1 - suave(0.55f * (float) R, (float) R, (float) d);
                fila[i] = new float[]{(float) x, (float) z, (float) (bu + x / escala), (float) (bv + z / escala), r, g, b, alfa * borde};
            }
            if (filaAnt != null) {
                for (int i = 0; i < N; i++) {
                    v(bb, filaAnt[i], hy);
                    v(bb, filaAnt[i + 1], hy);
                    v(bb, fila[i + 1], hy);
                    v(bb, fila[i], hy);
                }
            }
            filaAnt = fila;
        }
        RenderSystem.setShaderTexture(0, tex);
        vb.bind();
        vb.upload(bb.end());
        vb.drawWithShader(pose.last().pose(), proy, GameRenderer.getPositionTexColorShader());
        VertexBuffer.unbind();
    }

    private static void v(BufferBuilder bb, float[] p, double hy) {
        bb.vertex(p[0], (float) hy, p[1]).uv(p[2], p[3]).color(p[4], p[5], p[6], p[7]).endVertex();
    }
}
