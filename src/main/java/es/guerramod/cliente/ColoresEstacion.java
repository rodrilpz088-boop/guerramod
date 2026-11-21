package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.clima.Estaciones;
import es.guerramod.clima.TiempoCliente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.FoliageColor;
import net.minecraft.world.level.GrassColor;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Colores de la vegetación según la estación: primavera verde fresco, verano más seco y amarillento, otoño con los
 * árboles de hoja caduca en naranjas, rojos, amarillos y marrones (cada árbol a su ritmo), e invierno apagado.
 * Los pinos y abetos siguen verdes; la selva, la sabana y los manglares no cambian.
 */
public final class ColoresEstacion {
    private ColoresEstacion() {}

    private static int tramoPintado = -1;

    private static float suave(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    private static int mezcla(int c, float r, float g, float b, float w) {
        float cr = ((c >> 16) & 255) / 255f, cg = ((c >> 8) & 255) / 255f, cb = (c & 255) / 255f;
        cr += (r - cr) * w;
        cg += (g - cg) * w;
        cb += (b - cb) * w;
        return ((int) (Math.max(0, Math.min(1, cr)) * 255) << 16) | ((int) (Math.max(0, Math.min(1, cg)) * 255) << 8)
                | (int) (Math.max(0, Math.min(1, cb)) * 255);
    }

    private static float hash(BlockPos p, int k) {
        // el mismo árbol cambia a la vez: se agrupa por zonas de 3 bloques
        long h = (p.getX() / 3) * 0x9E3779B97F4A7C15L ^ (p.getZ() / 3) * 0xC2B2AE3D27D4EB4FL ^ (p.getY() / 4) * 0x165667B19E3779F9L ^ k;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1L << 24);
    }

    /** Hierba según la estación. */
    public static int hierba(int base) {
        int d = TiempoCliente.dia;
        float primavera = d < 200 ? (float) Math.sin(Math.PI * d / 200.0) : 0;
        float verano = d >= 200 && d < 400 ? (float) Math.sin(Math.PI * (d - 200) / 200.0) : 0;
        float otono = d >= 400 && d < 600 ? (float) Math.sin(Math.PI * (d - 400) / 200.0) : 0;
        float invierno = d >= 600 ? (float) Math.sin(Math.PI * (d - 600) / 200.0) : 0;
        int c = base;
        c = mezcla(c, 0.42f, 0.78f, 0.25f, 0.22f * primavera);
        c = mezcla(c, 0.72f, 0.70f, 0.36f, 0.28f * verano);
        c = mezcla(c, 0.55f, 0.55f, 0.28f, 0.25f * otono);
        c = mezcla(c, 0.45f, 0.48f, 0.34f, 0.35f * invierno);
        return c;
    }

    /** Hojas de árboles de hoja caduca. */
    public static int caduca(int base, BlockPos pos) {
        int d = TiempoCliente.dia;
        int c = base;
        if (d < 120) c = mezcla(c, 0.50f, 0.80f, 0.28f, 0.35f * (1 - d / 120f)); // brotes de primavera
        if (d >= 400 && d < 600) {
            float ritmo = hash(pos, 7) * 40;
            float otono = suave(410 + ritmo, 480 + ritmo, d);
            float seco = suave(540 + ritmo * 0.5f, 600, d);
            float h = hash(pos, 3);
            float r, g, b;
            if (h < 0.3f) { r = 0.90f; g = 0.47f; b = 0.10f; }
            else if (h < 0.5f) { r = 0.74f; g = 0.18f; b = 0.08f; }
            else if (h < 0.8f) { r = 0.88f; g = 0.72f; b = 0.16f; }
            else { r = 0.58f; g = 0.36f; b = 0.14f; }
            c = mezcla(c, r, g, b, 0.85f * otono);
            c = mezcla(c, 0.45f, 0.30f, 0.15f, 0.7f * seco);
        }
        if (d >= 600) {
            float primavera = suave(760, 799, d);
            c = mezcla(c, 0.42f, 0.32f, 0.20f, 0.75f * (1 - primavera));
        }
        return c;
    }

    /** Coníferas: más oscuras en invierno. */
    public static int conifera(int base) {
        float inv = (float) Math.max(0, -Estaciones.ciclo(TiempoCliente.dia));
        return mezcla(base, 0.20f, 0.33f, 0.22f, 0.3f * inv);
    }

    private static int follaje(BlockAndTintGetter l, BlockPos p) {
        return l != null && p != null ? BiomeColors.getAverageFoliageColor(l, p) : FoliageColor.getDefaultColor();
    }

    @Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registro {
        @SubscribeEvent
        public static void colores(RegisterColorHandlersEvent.Block e) {
            e.register((s, l, p, i) -> hierba(l != null && p != null ? BiomeColors.getAverageGrassColor(l, p) : GrassColor.getDefaultColor()),
                    Blocks.GRASS_BLOCK, Blocks.GRASS, Blocks.FERN, Blocks.TALL_GRASS, Blocks.LARGE_FERN, Blocks.POTTED_FERN);
            e.register((s, l, p, i) -> p == null ? follaje(l, p) : caduca(follaje(l, p), p), Blocks.OAK_LEAVES, Blocks.DARK_OAK_LEAVES, Blocks.VINE);
            e.register((s, l, p, i) -> p == null ? FoliageColor.getBirchColor() : caduca(FoliageColor.getBirchColor(), p), Blocks.BIRCH_LEAVES);
            e.register((s, l, p, i) -> conifera(FoliageColor.getEvergreenColor()), Blocks.SPRUCE_LEAVES);
        }
    }

    /** Cuando cambia el tramo de la estación (cada 10 días) se vuelven a pintar los chunks. */
    @Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class Repintar {
        @SubscribeEvent
        public static void tick(TickEvent.ClientTickEvent e) {
            if (e.phase != TickEvent.Phase.END) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null || !TiempoCliente.recibido) return;
            int tramo = TiempoCliente.dia / 10;
            if (tramo != tramoPintado) {
                boolean primera = tramoPintado < 0;
                tramoPintado = tramo;
                if (!primera || TiempoCliente.dia != 0) mc.levelRenderer.allChanged();
            }
        }
    }
}
