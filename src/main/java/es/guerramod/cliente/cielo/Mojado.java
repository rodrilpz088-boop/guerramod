package es.guerramod.cliente.cielo;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexBuffer;
import com.mojang.blaze3d.vertex.VertexFormat;
import es.guerramod.GuerraMod;
import es.guerramod.clima.TiempoCliente;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Random;

/**
 * Suelo mojado (como en los paquetes de sombreadores): con la lluvia todo lo que está al aire libre se va oscureciendo
 * poco a poco, a manchas, y las superficies planas brillan reflejando el cielo (mucho más al mirar en rasante, como el
 * asfalto mojado), con zonas más encharcadas. El césped y las hojas brillan poco; la piedra, el asfalto o la madera mucho.
 * Cuando para de llover se va secando, más rápido con calor, sol, viento y aire seco. No pone ningún bloque: es un
 * efecto visual dibujado sobre el mundo, solo donde llega la lluvia (no bajo techo ni en zonas de nieve o desierto).
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Mojado {
    private Mojado() {}

    /** Cuánto se ha mojado el mundo a tu alrededor (0 seco .. 1 empapado). */
    public static float humedad = 0;
    private static boolean desactivado = false;
    private static final int R = 30;

    private static VertexBuffer vbOscuro, vbBrillo;
    private static boolean hayOscuro, hayBrillo;
    private static BlockPos origen;
    private static long ultimoTick = Long.MIN_VALUE;
    private static Vec3 ultimaCam = Vec3.ZERO;
    private static float ultimaHumedad = -1;

    private static final ResourceLocation LOC = new ResourceLocation(GuerraMod.MODID, "dinamica/mojado");
    private static DynamicTexture textura;

    // ---------------------------------------------------------------- mojarse y secarse
    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || mc.player == null || mc.isPaused()) return;
        if (!CieloMundo.activo(l)) {
            humedad = 0;
            return;
        }
        final float dt = 0.05f;
        BlockPos p = mc.player.blockPosition();
        float lluvia = l.getRainLevel(1);
        boolean llueve = lluvia > 0.2f && Lluvia.precip(l, p.getX(), p.getY(), p.getZ()) == 1;
        if (llueve) {
            // empapa en ~40 s con lluvia normal, más deprisa con tormenta
            humedad = Math.min(1, humedad + dt / 40f * lluvia * (0.7f + 0.6f * l.getThunderLevel(1)));
        } else {
            double t = TiempoCliente.temperatura, h = TiempoCliente.humedad;
            double f = (1 + (t - 15) * 0.045) * (1 - (h - 60) * 0.008) * (l.isDay() ? 1.4 : 0.55)
                    * (1 + es.guerramod.incendios.Clima.clienteVelocidad * 0.012);
            f = Math.max(0.15, Math.min(4, f));
            humedad = (float) Math.max(0, humedad - dt / 300.0 * f); // unos 5 minutos en condiciones normales
        }
    }

    // ---------------------------------------------------------------- dibujo
    private static double hash(int x, int z, int k) {
        long h = x * 0x9E3779B97F4A7C15L ^ z * 0xC2B2AE3D27D4EB4FL ^ (k + 7L) * 0x165667B19E3779F9L;
        h ^= (h >>> 31);
        h *= 0xBF58476D1CE4E5B9L;
        h ^= (h >>> 29);
        return (h >>> 11) * 0x1.0p-53;
    }

    /** Cada bloque se moja (y se seca) en su momento: así salen manchas, no todo de golpe. */
    private static float local(int x, int z, float w) {
        double umbral = hash(x, z, 1) * 0.45;
        double v = (w - umbral) / 0.55;
        v = Math.max(0, Math.min(1, v));
        return (float) (v * v * (3 - 2 * v));
    }

    /** Cuánto se oscurece al mojarse (la arena y la tierra mucho, las hojas poco). */
    private static float oscurece(BlockState s) {
        if (s.is(BlockTags.LEAVES)) return 0.55f;
        if (s.is(BlockTags.SAND) || s.is(Blocks.GRAVEL)) return 1.25f;
        if (s.is(Blocks.GRASS_BLOCK)) return 0.8f;
        if (s.is(BlockTags.DIRT)) return 1.1f;
        return 1.0f;
    }

    /** Cuánto brilla mojado (el césped y las hojas poco, la piedra, el asfalto y la madera mucho). */
    private static float brilla(BlockState s) {
        if (s.is(BlockTags.LEAVES) || s.is(BlockTags.WOOL)) return 0.08f;
        if (s.is(Blocks.GRASS_BLOCK) || s.is(Blocks.MOSS_BLOCK)) return 0.3f;
        if (s.is(BlockTags.SAND) || s.is(BlockTags.DIRT) || s.is(Blocks.GRAVEL)) return 0.5f;
        return 1.0f;
    }

    private static void iniciarTextura() {
        if (textura != null) return;
        // manchas encharcadas: ruido suave que se repite; el resto es un brillo general más débil
        final int T = 128;
        textura = new DynamicTexture(T, T, false);
        NativeImage img = textura.getPixels();
        Random r = new Random(4242);
        float[] campo = new float[T * T];
        int[][] per = {{4, 4}, {8, 8}, {16, 16}, {32, 32}};
        float[] amp = {1f, 0.5f, 0.25f, 0.12f};
        for (int o = 0; o < per.length; o++) {
            int p = per[o][0];
            float[] red = new float[p * p];
            for (int i = 0; i < red.length; i++) red[i] = r.nextFloat();
            for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
                double fx = (double) x * p / T, fy = (double) y * p / T;
                int ix = (int) fx, iy = (int) fy;
                double tx = fx - ix, ty = fy - iy;
                tx = tx * tx * (3 - 2 * tx);
                ty = ty * ty * (3 - 2 * ty);
                int x0 = ix % p, x1 = (ix + 1) % p, y0 = iy % p, y1 = (iy + 1) % p;
                double a = red[y0 * p + x0] * (1 - tx) + red[y0 * p + x1] * tx;
                double b = red[y1 * p + x0] * (1 - tx) + red[y1 * p + x1] * tx;
                campo[y * T + x] += (float) ((a * (1 - ty) + b * ty) * amp[o]);
            }
        }
        for (int i = 0; i < campo.length; i++) campo[i] /= 1.87f;
        for (int y = 0; y < T; y++) for (int x = 0; x < T; x++) {
            float n = campo[y * T + x];
            float charco = Math.max(0, Math.min(1, (n - 0.52f) / 0.12f));
            float a = 0.28f + 0.72f * charco;
            int al = (int) (a * 255);
            img.setPixelRGBA(x, y, (al << 24) | 0xFFFFFF);
        }
        textura.bind();
        img.upload(0, 0, 0, 0, 0, T, T, true, false, false, false);
        Minecraft.getInstance().getTextureManager().register(LOC, textura);
    }

    @SubscribeEvent
    public static void dibujar(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS || desactivado || humedad < 0.01f) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        if (l == null || !CieloMundo.activo(l)) return;
        try {
            Camera cam = e.getCamera();
            Vec3 c = cam.getPosition();
            iniciarTextura();
            long t = l.getGameTime();
            if (origen == null || t - ultimoTick >= 10 || t < ultimoTick || c.distanceToSqr(ultimaCam) > 2.25
                    || Math.abs(humedad - ultimaHumedad) > 0.02f) {
                reconstruir(l, c);
                ultimoTick = t;
                ultimaCam = c;
                ultimaHumedad = humedad;
            }
            if (!hayOscuro && !hayBrillo) return;
            PoseStack pose = e.getPoseStack();
            pose.pushPose();
            pose.translate(origen.getX() - c.x, origen.getY() - c.y, origen.getZ() - c.z);
            var m = pose.last().pose();
            RenderSystem.enableBlend();
            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1.0f, -12.0f);
            RenderSystem.setShaderColor(1, 1, 1, 1);
            if (hayOscuro) {
                // multiplicar: la superficie mojada es más oscura
                RenderSystem.blendFunc(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.ZERO);
                vbOscuro.bind();
                vbOscuro.drawWithShader(m, e.getProjectionMatrix(), GameRenderer.getPositionColorShader());
            }
            if (hayBrillo) {
                // sumar: reflejo del cielo
                RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
                RenderSystem.setShaderTexture(0, LOC);
                vbBrillo.bind();
                vbBrillo.drawWithShader(m, e.getProjectionMatrix(), GameRenderer.getPositionTexColorShader());
            }
            VertexBuffer.unbind();
            RenderSystem.polygonOffset(0, 0);
            RenderSystem.disablePolygonOffset();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
            pose.popPose();
        } catch (Throwable th) {
            desactivado = true;
            GuerraMod.LOGGER.error("Efecto de suelo mojado desactivado por un error", th);
            RenderSystem.polygonOffset(0, 0);
            RenderSystem.disablePolygonOffset();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
            RenderSystem.depthMask(true);
            RenderSystem.enableCull();
        }
    }

    private static void reconstruir(ClientLevel l, Vec3 c) {
        if (vbOscuro == null) {
            vbOscuro = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
            vbBrillo = new VertexBuffer(VertexBuffer.Usage.DYNAMIC);
        }
        origen = BlockPos.containing(c);
        int ox = origen.getX(), oy = origen.getY(), oz = origen.getZ();
        float w = humedad;
        float[] cielo = new float[3];
        BufferBuilder bb = Tesselator.getInstance().getBuilder();

        // ---- pasada 1: oscurecer (caras de arriba y laterales expuestas a la lluvia)
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
        int n1 = 0;
        int[] alturas = new int[(2 * R + 3) * (2 * R + 3)];
        int ancho = 2 * R + 3;
        for (int dx = -R - 1; dx <= R + 1; dx++)
            for (int dz = -R - 1; dz <= R + 1; dz++)
                alturas[(dx + R + 1) * ancho + dz + R + 1] = l.getHeight(Heightmap.Types.MOTION_BLOCKING, ox + dx, oz + dz) - 1;
        BlockPos.MutableBlockPos bp = new BlockPos.MutableBlockPos();
        double[] ytops = new double[(2 * R + 1) * (2 * R + 1)];
        float[] wls = new float[ytops.length];
        BlockState[] estados = new BlockState[ytops.length];
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                int i = (dx + R) * (2 * R + 1) + dz + R;
                ytops[i] = Double.NaN;
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > R) continue;
                int x = ox + dx, z = oz + dz;
                if (Lluvia.precip(l, x, oy, z) != 1) continue;
                int top = alturas[(dx + R + 1) * ancho + dz + R + 1];
                bp.set(x, top, z);
                BlockState st = l.getBlockState(bp);
                // solo bloques macizos (las hojas o el cristal tienen huecos); bajo los árboles el suelo queda seco
                if (st.isAir() || !st.getFluidState().isEmpty() || !st.canOcclude()) continue;
                VoxelShape sh = st.getShape(l, bp);
                if (sh.isEmpty()) continue;
                double ytop = top + sh.max(Direction.Axis.Y);
                double borde = 1 - Math.max(0, Math.min(1, (dist - (R - 8)) / 8.0));
                float wl = local(x, z, w) * (float) borde;
                if (wl < 0.01f) continue;
                ytops[i] = ytop;
                wls[i] = wl;
                estados[i] = st;
                float g = Math.max(0.45f, 1 - 0.32f * wl * oscurece(st));
                float fx = dx, fz = dz, fy = (float) (ytop - oy + 0.002);
                bb.vertex(fx, fy, fz).color(g, g, g, 1f).endVertex();
                bb.vertex(fx, fy, fz + 1).color(g, g, g, 1f).endVertex();
                bb.vertex(fx + 1, fy, fz + 1).color(g, g, g, 1f).endVertex();
                bb.vertex(fx + 1, fy, fz).color(g, g, g, 1f).endVertex();
                n1++;
                // paredes del bloque de arriba que dan al aire libre (la lluvia también las moja)
                float gs = Math.max(0.55f, 1 - 0.22f * wl * oscurece(st));
                for (Direction d : Direction.Plane.HORIZONTAL) {
                    int hv = alturas[(dx + d.getStepX() + R + 1) * ancho + dz + d.getStepZ() + R + 1];
                    if (hv >= top) continue;
                    float y0 = (float) (Math.max(hv + 1, top - 3) - oy), y1 = fy;
                    if (y1 - y0 < 0.1f) continue;
                    float px = d.getStepX() > 0 ? fx + 1.002f : d.getStepX() < 0 ? fx - 0.002f : 0;
                    float pz = d.getStepZ() > 0 ? fz + 1.002f : d.getStepZ() < 0 ? fz - 0.002f : 0;
                    if (d.getAxis() == Direction.Axis.X) {
                        bb.vertex(px, y0, fz).color(gs, gs, gs, 1f).endVertex();
                        bb.vertex(px, y1, fz).color(gs, gs, gs, 1f).endVertex();
                        bb.vertex(px, y1, fz + 1).color(gs, gs, gs, 1f).endVertex();
                        bb.vertex(px, y0, fz + 1).color(gs, gs, gs, 1f).endVertex();
                    } else {
                        bb.vertex(fx, y0, pz).color(gs, gs, gs, 1f).endVertex();
                        bb.vertex(fx, y1, pz).color(gs, gs, gs, 1f).endVertex();
                        bb.vertex(fx + 1, y1, pz).color(gs, gs, gs, 1f).endVertex();
                        bb.vertex(fx + 1, y0, pz).color(gs, gs, gs, 1f).endVertex();
                    }
                    n1++;
                }
            }
        }
        BufferBuilder.RenderedBuffer rb = bb.end();
        if (n1 > 0) {
            vbOscuro.bind();
            vbOscuro.upload(rb);
            hayOscuro = true;
        } else {
            rb.release();
            hayOscuro = false;
        }

        // ---- pasada 2: reflejo del cielo en las caras planas (Fresnel: más brillo cuanto más en rasante)
        bb.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        int n2 = 0;
        for (int dx = -R; dx <= R; dx++) {
            for (int dz = -R; dz <= R; dz++) {
                int i = (dx + R) * (2 * R + 1) + dz + R;
                if (Double.isNaN(ytops[i])) continue;
                float wl = wls[i];
                float mat = brilla(estados[i]);
                int x = ox + dx, z = oz + dz;
                double vx = x + 0.5 - c.x, vy = ytops[i] - c.y, vz = z + 0.5 - c.z;
                double vn = Math.sqrt(vx * vx + vy * vy + vz * vz);
                if (vn < 1e-3) continue;
                vx /= vn; vy /= vn; vz /= vn;
                if (vy > 0) continue; // superficie por encima de los ojos: no se ve su cara de arriba
                double cos = Math.abs(vy);
                double fres = 0.02 + 0.98 * Math.pow(1 - cos, 5);
                float a = (float) (wl * wl * mat * Math.min(1, fres * 1.6 + 0.05) * 0.95);
                if (a < 0.012f) continue;
                CieloMundo.colorBoveda(vx, -vy, vz, cielo);
                float fx = dx, fz = dz, fy = (float) (ytops[i] - oy + 0.003);
                float u0 = Math.floorMod(x, 1200) / 12f, v0 = Math.floorMod(z, 1200) / 12f, d = 1 / 12f;
                bb.vertex(fx, fy, fz).uv(u0, v0).color(cielo[0], cielo[1], cielo[2], a).endVertex();
                bb.vertex(fx, fy, fz + 1).uv(u0, v0 + d).color(cielo[0], cielo[1], cielo[2], a).endVertex();
                bb.vertex(fx + 1, fy, fz + 1).uv(u0 + d, v0 + d).color(cielo[0], cielo[1], cielo[2], a).endVertex();
                bb.vertex(fx + 1, fy, fz).uv(u0 + d, v0).color(cielo[0], cielo[1], cielo[2], a).endVertex();
                n2++;
            }
        }
        rb = bb.end();
        if (n2 > 0) {
            vbBrillo.bind();
            vbBrillo.upload(rb);
            hayBrillo = true;
        } else {
            rb.release();
            hayBrillo = false;
        }
        VertexBuffer.unbind();
    }
}
