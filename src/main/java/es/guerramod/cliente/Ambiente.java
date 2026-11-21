package es.guerramod.cliente;

import es.guerramod.GuerraMod;
import es.guerramod.clima.Estaciones;
import es.guerramod.clima.TiempoCliente;
import es.guerramod.registro.ModParticulas;
import es.guerramod.registro.SonidosAmbiente;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.function.Supplier;

/**
 * Mundo vivo: pájaros de día (según el bioma y la estación), grillos, búhos y ranas de noche, chicharras en verano, olas y
 * gaviotas en la costa, viento entre los árboles, arroyos; hojas que caen de los árboles (muchas en otoño) y estelas en
 * el agua al nadar o ir en barca.
 */
@Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class Ambiente {
    private Ambiente() {}

    private static int esperaPajaro = 40, esperaNoche = 40, esperaCosta = 60, esperaViento = 80, esperaArroyo = 60;

    private static void sonar(ClientLevel l, Player p, Supplier<SoundEvent> s, double distMin, double distMax, double altura, float vol) {
        RandomSource r = l.random;
        double a = r.nextDouble() * Math.PI * 2, d = distMin + r.nextDouble() * (distMax - distMin);
        double x = p.getX() + Math.cos(a) * d, z = p.getZ() + Math.sin(a) * d;
        int suelo = l.getHeight(Heightmap.Types.MOTION_BLOCKING, (int) Math.floor(x), (int) Math.floor(z));
        l.playLocalSound(x, suelo + altura * r.nextDouble(), z, s.get(), SoundSource.AMBIENT, vol, 0.92f + r.nextFloat() * 0.16f, false);
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        ClientLevel l = mc.level;
        Player p = mc.player;
        if (l == null || p == null || mc.isPaused() || l.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;
        RandomSource r = l.random;
        BlockPos pos = p.blockPosition();
        int cielo = l.getBrightness(LightLayer.SKY, pos);
        boolean fuera = cielo >= 10 && !p.isUnderWater();
        float sol = (float) Math.cos(l.getTimeOfDay(1f) * Math.PI * 2);
        boolean dia = sol > 0.05f, amanecer = sol > -0.05f && sol < 0.35f;
        boolean lluvia = l.isRaining();
        int d = TiempoCliente.dia, est = Estaciones.estacion(d);
        float temp = TiempoCliente.temperatura;
        Holder<Biome> bioma = l.getBiome(pos);
        boolean bosque = bioma.is(BiomeTags.IS_FOREST) || bioma.is(BiomeTags.IS_TAIGA) || bioma.is(BiomeTags.IS_JUNGLE);
        boolean costa = bioma.is(BiomeTags.IS_BEACH) || bioma.is(BiomeTags.IS_OCEAN);
        boolean desierto = bioma.value().getBaseTemperature() > 1.5f && !bioma.value().hasPrecipitation();

        if (fuera) {
            // ---- pájaros (de día; más en primavera y al amanecer; casi ninguno en invierno o con lluvia)
            if (dia && !lluvia && !desierto && --esperaPajaro <= 0) {
                float ritmo = (est == 0 ? 1.5f : est == 1 ? 1.0f : est == 2 ? 0.7f : 0.3f) * (amanecer ? 1.8f : 1f) * (bosque ? 1.5f : 1f);
                esperaPajaro = (int) ((60 + r.nextInt(140)) / ritmo);
                float x = r.nextFloat();
                Supplier<SoundEvent> s;
                if (est == 0 && x < 0.12f) s = SonidosAmbiente.CUCO;
                else if (bosque && x < 0.22f) s = SonidosAmbiente.CARPINTERO;
                else if (x < 0.45f) s = SonidosAmbiente.JILGUERO;
                else if (x < 0.65f) s = SonidosAmbiente.MIRLO;
                else if (x < 0.85f) s = SonidosAmbiente.GORRION;
                else s = SonidosAmbiente.PALOMA;
                if (!costa || r.nextBoolean()) sonar(l, p, s, 6, 26, 6, 0.55f);
            }
            // ---- noche: grillos con calor, búho en el bosque, ruiseñor en primavera, ranas cerca del agua
            if (!dia && --esperaNoche <= 0) {
                esperaNoche = 50 + r.nextInt(90);
                if (temp > 9 && est != 3 && !lluvia) sonar(l, p, SonidosAmbiente.GRILLOS, 4, 18, 1, 0.35f);
                if (bosque && r.nextFloat() < 0.25f) sonar(l, p, SonidosAmbiente.BUHO, 14, 40, 10, 0.7f);
                if (est == 0 && r.nextFloat() < 0.25f) sonar(l, p, SonidosAmbiente.RUISENOR, 8, 30, 6, 0.6f);
                if (temp > 8 && est != 3 && aguaCerca(l, pos, r) && r.nextFloat() < 0.6f) sonar(l, p, SonidosAmbiente.RANA, 4, 16, 1, 0.5f);
            }
            // ---- chicharras en verano con calor
            if (dia && est == 1 && temp > 24 && !lluvia && r.nextInt(120) == 0) sonar(l, p, SonidosAmbiente.CHICHARRA, 6, 20, 5, 0.4f);
            // ---- costa: olas y gaviotas
            if ((costa || aguaGrande(l, pos)) && --esperaCosta <= 0) {
                esperaCosta = 90 + r.nextInt(60);
                sonar(l, p, SonidosAmbiente.OLAS, 10, 26, 1, 0.6f);
                if (dia && r.nextFloat() < 0.35f) sonar(l, p, SonidosAmbiente.GAVIOTA, 10, 40, 14, 0.6f);
            }
            // ---- viento en los árboles
            if (es.guerramod.incendios.Clima.clienteVelocidad > 14 && hojasCerca(l, pos, r) && --esperaViento <= 0) {
                esperaViento = 80 + r.nextInt(80);
                sonar(l, p, SonidosAmbiente.VIENTO_HOJAS, 3, 12, 5, (float) Math.min(0.8, es.guerramod.incendios.Clima.clienteVelocidad / 45.0));
            }
            // ---- arroyo
            if (bioma.is(BiomeTags.IS_RIVER) && --esperaArroyo <= 0) {
                esperaArroyo = 100 + r.nextInt(40);
                sonar(l, p, SonidosAmbiente.ARROYO, 3, 12, 0, 0.45f);
            }
        }
        hojas(l, p, r, d);
        estelas(l, p, r);
    }

    private static boolean aguaCerca(ClientLevel l, BlockPos p, RandomSource r) {
        for (int i = 0; i < 12; i++) {
            BlockPos q = l.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, p.offset(r.nextInt(33) - 16, 0, r.nextInt(33) - 16)).below();
            if (l.getBlockState(q).is(Blocks.WATER)) return true;
        }
        return false;
    }

    private static boolean aguaGrande(ClientLevel l, BlockPos p) {
        int n = 0;
        for (int dx = -24; dx <= 24; dx += 12) for (int dz = -24; dz <= 24; dz += 12) {
            BlockPos q = l.getHeightmapPos(Heightmap.Types.WORLD_SURFACE, p.offset(dx, 0, dz)).below();
            if (l.getBlockState(q).is(Blocks.WATER)) n++;
        }
        return n >= 12;
    }

    private static boolean hojasCerca(ClientLevel l, BlockPos p, RandomSource r) {
        for (int i = 0; i < 6; i++) {
            BlockPos q = l.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, p.offset(r.nextInt(17) - 8, 0, r.nextInt(17) - 8)).below();
            if (l.getBlockState(q).is(BlockTags.LEAVES)) return true;
        }
        return false;
    }

    /** Hojas que se desprenden y caen girando, arrastradas por el viento. */
    private static void hojas(ClientLevel l, Player p, RandomSource r, int d) {
        int est = Estaciones.estacion(d);
        int muestras = est == 2 ? 14 : est == 3 ? 3 : 2;
        float prob = est == 2 ? 0.9f : 0.35f;
        for (int i = 0; i < muestras; i++) {
            BlockPos q = p.blockPosition().offset(r.nextInt(33) - 16, r.nextInt(17) - 4, r.nextInt(33) - 16);
            BlockState s = l.getBlockState(q);
            if (!s.is(BlockTags.LEAVES) || !l.getBlockState(q.below()).isAir() || r.nextFloat() > prob) continue;
            int c;
            if (s.is(Blocks.SPRUCE_LEAVES)) c = ColoresEstacion.conifera(net.minecraft.world.level.FoliageColor.getEvergreenColor());
            else if (s.is(Blocks.BIRCH_LEAVES)) c = ColoresEstacion.caduca(net.minecraft.world.level.FoliageColor.getBirchColor(), q);
            else if (s.is(Blocks.OAK_LEAVES) || s.is(Blocks.DARK_OAK_LEAVES)) c = ColoresEstacion.caduca(BiomeColors.getAverageFoliageColor(l, q), q);
            else if (s.is(Blocks.CHERRY_LEAVES)) c = 0xF2B8D0;
            else c = BiomeColors.getAverageFoliageColor(l, q);
            l.addParticle(ModParticulas.HOJA.get(), q.getX() + r.nextDouble(), q.getY() - 0.05, q.getZ() + r.nextDouble(),
                    ((c >> 16) & 255) / 255.0, ((c >> 8) & 255) / 255.0, (c & 255) / 255.0);
        }
    }

    /** Estela blanca detrás de lo que se mueve por el agua (barcas, nadadores, animales). */
    private static void estelas(ClientLevel l, Player p, RandomSource r) {
        for (Entity e : l.getEntities(p, p.getBoundingBox().inflate(24), en -> en.isInWater() || en.getVehicle() != null && en.getVehicle().isInWater())) {
            if (!e.isInWater()) continue;
            double v = e.getDeltaMovement().horizontalDistanceSqr();
            if (v < 0.004 || r.nextFloat() > Math.min(1, v * 25)) continue;
            BlockPos b = e.blockPosition();
            double y = b.getY() + (l.getFluidState(b.above()).isEmpty() ? l.getFluidState(b).getHeight(l, b) : 1.0);
            l.addParticle(ParticleTypes.FISHING, e.getX() - e.getDeltaMovement().x * 4 + (r.nextDouble() - 0.5) * e.getBbWidth(), y,
                    e.getZ() - e.getDeltaMovement().z * 4 + (r.nextDouble() - 0.5) * e.getBbWidth(), 0, 0.02, 0);
        }
        if (p.isInWater() && p.getDeltaMovement().horizontalDistanceSqr() > 0.004 && r.nextInt(3) == 0) {
            l.addParticle(ParticleTypes.SPLASH, p.getX() + (r.nextDouble() - 0.5), p.getY() + p.getBbHeight() * 0.7, p.getZ() + (r.nextDouble() - 0.5), 0, 0.1, 0);
        }
    }

    // ---------------------------------------------------------------- partícula de hoja
    public static class Hoja extends TextureSheetParticle {
        private final float giro, fase;

        Hoja(ClientLevel l, double x, double y, double z, float r, float g, float b, SpriteSet s) {
            super(l, x, y, z);
            setColor(r, g, b);
            pickSprite(s);
            lifetime = 220 + random.nextInt(200);
            gravity = 0.0f;
            quadSize = 0.08f + random.nextFloat() * 0.05f;
            giro = (random.nextFloat() - 0.5f) * 0.25f;
            fase = random.nextFloat() * 6.28f;
            xd = 0;
            yd = -0.02;
            zd = 0;
        }

        @Override
        public void tick() {
            super.tick();
            double ang = Math.toRadians(es.guerramod.incendios.Clima.clienteAngulo);
            double v = es.guerramod.incendios.Clima.clienteVelocidad / 3.6 / 20.0 * 0.6;
            double bal = Math.sin(age * 0.12 + fase) * 0.025;
            if (!onGround) {
                xd = Math.cos(ang) * v + bal;
                zd = Math.sin(ang) * v + Math.cos(age * 0.1 + fase) * 0.02;
                yd = -0.03 - 0.01 * Math.sin(age * 0.2 + fase);
                oRoll = roll;
                roll += giro;
            } else {
                xd = zd = 0;
                if (age > lifetime - 40) alpha = (lifetime - age) / 40f;
            }
        }

        @Override
        public ParticleRenderType getRenderType() {
            return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
        }
    }

    public record ProveedorHoja(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public net.minecraft.client.particle.Particle createParticle(SimpleParticleType t, ClientLevel l, double x, double y, double z,
                                                                    double r, double g, double b) {
            return new Hoja(l, x, y, z, (float) r, (float) g, (float) b, sprites);
        }
    }

    @Mod.EventBusSubscriber(modid = GuerraMod.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static final class Registro {
        @SubscribeEvent
        public static void particulas(RegisterParticleProvidersEvent e) {
            e.registerSpriteSet(ModParticulas.HOJA.get(), ProveedorHoja::new);
        }
    }
}
