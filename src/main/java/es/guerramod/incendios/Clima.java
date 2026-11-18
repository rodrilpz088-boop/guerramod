package es.guerramod.incendios;

import es.guerramod.red.Red;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Viento y sequía del mundo. El viento cambia poco a poco (dirección y fuerza) y la sequía crece con los días sin
 * llover. Con todo eso se calcula el riesgo de incendio (bajo, moderado, alto, muy alto, extremo).
 */
public class Clima extends SavedData {
    private static final String NOMBRE = "guerramod_clima";
    /** Duración de un día de Minecraft en ticks. */
    public static final long DIA = 24000;

    /** Hacia dónde sopla el viento, en grados (0 = hacia el este, 90 = hacia el sur). */
    public float angulo = 45;
    /** Velocidad del viento en km/h. */
    public float velocidad = 12;
    public long ultimaLluvia = 0;
    /** Incendios naturales por rayos secos con riesgo muy alto o extremo. */
    public boolean naturales = true;
    /** Temperatura (°C) y humedad (%) fijadas con /incendios; NaN = automáticas (bioma, hora, lluvia). */
    public float tempFija = Float.NaN, humFija = Float.NaN;

    /** Copia en el cliente (para el minimapa). */
    public static float clienteAngulo = 45, clienteVelocidad = 12;
    public static int clienteRiesgo = 1;

    public static Clima get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(Clima::cargar, Clima::new, NOMBRE);
    }

    public static Clima cargar(CompoundTag t) {
        Clima c = new Clima();
        c.angulo = t.getFloat("angulo");
        c.velocidad = t.getFloat("velocidad");
        c.ultimaLluvia = t.getLong("lluvia");
        c.naturales = !t.contains("naturales") || t.getBoolean("naturales");
        c.tempFija = t.contains("tempFija") ? t.getFloat("tempFija") : Float.NaN;
        c.humFija = t.contains("humFija") ? t.getFloat("humFija") : Float.NaN;
        return c;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        t.putFloat("angulo", angulo);
        t.putFloat("velocidad", velocidad);
        t.putLong("lluvia", ultimaLluvia);
        t.putBoolean("naturales", naturales);
        if (!Float.isNaN(tempFija)) t.putFloat("tempFija", tempFija);
        if (!Float.isNaN(humFija)) t.putFloat("humFija", humFija);
        return t;
    }

    /** Cada 30 segundos: el viento cambia un poco. */
    public void tick(MinecraftServer server) {
        Level ow = server.overworld();
        long ahora = ow.getGameTime();
        if (ow.isRaining()) ultimaLluvia = ahora;
        if (ahora % 600 != 0) return;
        RandomSource r = ow.random;
        angulo = (angulo + (float) r.nextGaussian() * 14.0f + 360.0f) % 360.0f;
        float objetivo = ow.isThundering() ? 38 : ow.isRaining() ? 22 : 14;
        velocidad = Mth.clamp(velocidad + (float) r.nextGaussian() * 4.0f + (objetivo - velocidad) * 0.08f, 0.0f, 55.0f);
        setDirty();
        Red.sincronizarClima(server, angulo, velocidad, riesgo(ow, ow.getSharedSpawnPos()));
        if (naturales && !ow.isRaining() && ow instanceof net.minecraft.server.level.ServerLevel sl) {
            for (net.minecraft.server.level.ServerPlayer p : sl.players()) {
                int rg = riesgo(sl, p.blockPosition());
                if (rg >= 3 && r.nextFloat() < (rg == 4 ? 0.10f : 0.04f)) rayoSeco(sl, p.blockPosition(), r);
            }
        }
    }

    /** Guarda los cambios hechos con comandos y avisa a los jugadores (minimapa). */
    public void cambiado(MinecraftServer server) {
        setDirty();
        net.minecraft.server.level.ServerLevel ow = server.overworld();
        Red.sincronizarClima(server, angulo, velocidad, Meteo.calcular(ow, ow.getSharedSpawnPos()).riesgo());
    }

    /** Pone los días que lleva sin llover. */
    public void ponerSequia(Level level, double dias) {
        ultimaLluvia = level.getGameTime() - (long) (dias * DIA);
    }

    /** Vector unitario hacia donde sopla (x, z). */
    public double vx() {
        return Math.cos(Math.toRadians(angulo));
    }

    public double vz() {
        return Math.sin(Math.toRadians(angulo));
    }

    /** 0..1 */
    public double fuerza() {
        return Math.min(1.0, velocidad / 40.0);
    }

    /** Días desde la última lluvia. */
    public double diasSinLluvia(Level level) {
        return Math.max(0, (level.getGameTime() - ultimaLluvia) / (double) DIA);
    }

    /** Lo seco que está el sitio: 0,3 (húmedo) a 2 (desierto en sequía). */
    public double sequedad(Level level, BlockPos pos) {
        double s = 0.55 + Math.min(1.0, diasSinLluvia(level) / 3.0) * 0.6;
        Holder<Biome> b = level.getBiome(pos);
        float temp = b.value().getBaseTemperature();
        if (temp >= 1.5f) s *= 1.55;
        else if (temp >= 0.95f) s *= 1.3;
        else if (temp < 0.3f) s *= 0.5;
        if (!b.value().hasPrecipitation()) s *= 1.2;
        return s;
    }

    /** 0 bajo, 1 moderado, 2 alto, 3 muy alto, 4 extremo. */
    public int riesgo(Level level, BlockPos pos) {
        if (level instanceof net.minecraft.server.level.ServerLevel sl) return Meteo.en(sl, pos).riesgo();
        if (level.isRaining()) return 0;
        double v = sequedad(level, pos) * (0.7 + fuerza() * 0.9);
        if (v < 0.6) return 0;
        if (v < 0.85) return 1;
        if (v < 1.15) return 2;
        if (v < 1.5) return 3;
        return 4;
    }

    public static String nombreRiesgo(int r) {
        return switch (r) {
            case 0 -> "BAJO";
            case 1 -> "MODERADO";
            case 2 -> "ALTO";
            case 3 -> "MUY ALTO";
            default -> "EXTREMO";
        };
    }

    public static int colorRiesgo(int r) {
        return switch (r) {
            case 0 -> 0x40C040;
            case 1 -> 0xE0E040;
            case 2 -> 0xF0A020;
            case 3 -> 0xF05020;
            default -> 0xC01010;
        };
    }

    /** De dónde viene el viento (como en el parte meteorológico). */
    public static String procedencia(float angulo) {
        // el viento viene del lado contrario al que sopla
        double desde = (angulo + 180.0) % 360.0;
        // 0 = este, 90 = sur, 180 = oeste, 270 = norte
        String[] nombres = {"este", "sureste", "sur", "suroeste", "oeste", "noroeste", "norte", "noreste"};
        return nombres[(int) Math.round(desde / 45.0) % 8];
    }

    /** Tormenta seca: cae un rayo lejos del jugador y, si hay vegetación, empieza un incendio. */
    public static boolean rayoSeco(net.minecraft.server.level.ServerLevel level, BlockPos cerca, RandomSource r) {
        double ang = r.nextDouble() * Math.PI * 2, dist = 50 + r.nextDouble() * 60;
        int x = cerca.getX() + (int) (Math.cos(ang) * dist), z = cerca.getZ() + (int) (Math.sin(ang) * dist);
        if (!level.isLoaded(new BlockPos(x, cerca.getY(), z))) return false;
        BlockPos copa = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, new BlockPos(x, 0, z));
        BlockPos suelo = level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, new BlockPos(x, 0, z));
        net.minecraft.world.entity.LightningBolt rayo = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(level);
        if (rayo == null) return false;
        rayo.moveTo(net.minecraft.world.phys.Vec3.atBottomCenterOf(copa));
        rayo.setVisualOnly(true);
        level.addFreshEntity(rayo);
        boolean prendio = false;
        BlockPos hoja = copa.below();
        if (level.getBlockState(hoja).is(net.minecraft.tags.BlockTags.LEAVES)) {
            level.setBlock(hoja, BloquesIncendio.LLAMA.get().defaultBlockState().setValue(LlamaBlock.COPA, true)
                    .setValue(LlamaBlock.GRANDE, true), 3);
            prendio = true;
        } else if (Fuego.cabeLlama(level.getBlockState(suelo)) && Fuego.combustible(level, suelo) > 0.05) {
            level.setBlock(suelo, BloquesIncendio.LLAMA.get().defaultBlockState(), 3);
            prendio = true;
        }
        if (prendio) {
            net.minecraft.network.chat.Component aviso = net.minecraft.network.chat.Component.literal(
                    "⚡ Un rayo ha provocado un incendio forestal en " + x + ", " + z).withStyle(net.minecraft.ChatFormatting.GOLD);
            for (net.minecraft.server.level.ServerPlayer p : level.players()) {
                if (p.blockPosition().distSqr(copa) < 300 * 300) p.sendSystemMessage(aviso);
            }
        }
        return prendio;
    }
}
