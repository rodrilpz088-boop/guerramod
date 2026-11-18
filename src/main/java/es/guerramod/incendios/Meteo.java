package es.guerramod.incendios;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraftforge.common.Tags;

/**
 * Condiciones meteorológicas en un punto y comportamiento del fuego en hierba, con los modelos que usan los
 * servicios forestales:
 * <ul>
 *   <li>Humedad del combustible fino muerto (McArthur Mk5, a partir de temperatura, humedad relativa y curado).</li>
 *   <li>Velocidad de avance en pasto (modelo CSIRO de Cheney 1998: viento, humedad del combustible y curado).</li>
 *   <li>Forma elíptica del incendio según el viento (relación longitud/anchura de Alexander 1985).</li>
 *   <li>Índice de peligro de incendio en pasto (GFDI Mk5) para el riesgo BAJO..EXTREMO.</li>
 * </ul>
 *
 * @param temperatura        °C
 * @param humedad            humedad relativa del aire (%)
 * @param viento             km/h
 * @param curado             % de hierba seca (curada); la hierba verde casi no arde
 * @param humedadCombustible % de agua en la hierba muerta
 * @param factorHumedad      coeficiente de humedad del modelo CSIRO (0..1)
 * @param factorCurado       coeficiente de curado del modelo CSIRO (0..1)
 * @param rosCabeza          velocidad del frente a favor del viento (m/s = bloques/s)
 * @param excentricidad      excentricidad de la elipse del incendio (0 = círculo sin viento)
 * @param indice             índice de peligro GFDI
 * @param riesgo             0 bajo, 1 moderado, 2 alto, 3 muy alto, 4 extremo
 */
public record Meteo(double temperatura, double humedad, double viento, double curado, double humedadCombustible,
                    double factorHumedad, double factorCurado, double rosCabeza, double excentricidad, double indice, int riesgo) {

    private static final Long2ObjectOpenHashMap<Meteo> CACHE = new Long2ObjectOpenHashMap<>();
    private static long cacheSegundo = -1;

    /** Condiciones en una posición (se recalculan una vez por segundo y por chunk). */
    public static Meteo en(ServerLevel level, BlockPos pos) {
        long seg = level.getGameTime() / 20;
        if (seg != cacheSegundo) {
            CACHE.clear();
            cacheSegundo = seg;
        }
        long clave = (((long) (pos.getX() >> 4)) << 40) ^ (((long) (pos.getZ() >> 4)) << 16) ^ ((pos.getY() >> 3) & 0xFFFF)
                ^ ((long) level.dimension().location().hashCode() << 52);
        Meteo m = CACHE.get(clave);
        if (m == null) {
            m = calcular(level, pos);
            CACHE.put(clave, m);
        }
        return m;
    }

    public static Meteo calcular(ServerLevel level, BlockPos pos) {
        Clima c = Clima.get(level.getServer());
        Holder<Biome> b = level.getBiome(pos);
        float bt = b.value().getBaseTemperature();
        boolean lluvia = Fuego.llueve(level, pos);
        double dias = c.diasSinLluvia(level);

        // ---- temperatura: clima del bioma + ciclo del día (máxima hacia las 14:00) + altitud + lluvia
        long hora = level.getDayTime() % 24000; // 0 = 6:00, 6000 = 12:00
        double dia = Math.cos(2 * Math.PI * (hora - 8000) / 24000.0);
        double t = 5 + 13 * bt + 7 * dia - 0.0065 * Math.max(0, pos.getY() - 63);
        if (lluvia) t -= 4;
        if (b.is(Tags.Biomes.IS_HOT)) t += 3;

        // ---- humedad relativa: el bioma, la hora (de noche sube) y la lluvia reciente
        double h = 45;
        if (b.is(BiomeTags.IS_JUNGLE) || b.is(Tags.Biomes.IS_SWAMP)) h = 80;
        else if (b.is(Tags.Biomes.IS_WET)) h = 70;
        else if (b.is(Tags.Biomes.IS_DRY) || !b.value().hasPrecipitation()) h = 20;
        h -= 18 * dia;
        h += 30 * Math.exp(-dias / 0.8);
        if (lluvia) h = 97;
        h = clamp(h, 5, 100);

        // ---- curado de la hierba (% seca): crece con los días sin llover y el calor
        double cur = 55 + 18 * dias + 1.2 * (t - 15);
        if (!b.value().hasPrecipitation()) cur += 20;
        if (lluvia) cur = 40;
        cur = clamp(cur, 30, 100);

        // ---- viento (sube con la altitud)
        double u = c.velocidad * (1.0 + Math.max(0, pos.getY() - 70) * 0.004);

        // ---- humedad del combustible fino (McArthur)
        double m;
        if (t + 6 <= 1) m = 45;
        else m = (97.7 + 4.06 * h) / (t + 6) - 0.00854 * h + 3000.0 / cur - 30;
        if (lluvia) m = Math.max(m, 35);
        m = clamp(m, 2, 60);

        // ---- coeficientes CSIRO
        double fm;
        if (m < 12) fm = Math.exp(-0.108 * m);
        else if (u <= 10) fm = 0.684 - 0.0342 * m;
        else fm = 0.547 - 0.0228 * m;
        fm = clamp(fm, 0, 1);
        double fc = 1.12 / (1 + 59.2 * Math.exp(-0.124 * (cur - 50)));
        fc = clamp(fc, 0, 1);

        // ---- velocidad del frente de cabeza en pasto natural (km/h -> m/s)
        double rosKmh = u < 5 ? 0.054 + 0.269 * u : 1.4 + 0.838 * Math.pow(u - 5, 0.844);
        double ros = rosKmh * fm * fc / 3.6;

        // ---- forma elíptica (Alexander): con viento el incendio se alarga
        double lb = 1 + 0.0012 * Math.pow(u, 2.154);
        double e = Math.sqrt(Math.max(0, 1 - 1 / (lb * lb)));

        // ---- índice de peligro en pasto (GFDI Mk5, carga 4,5 t/ha)
        double gfdi = Math.exp(-1.523 + 1.027 * Math.log(4.5) - 0.009432 * Math.pow(100 - cur, 1.536)
                + 0.0276 * t - 0.2205 * Math.sqrt(h) + 0.6422 * Math.sqrt(u));
        int riesgo = lluvia ? 0 : gfdi < 2.5 ? 0 : gfdi < 7.5 ? 1 : gfdi < 20 ? 2 : gfdi < 50 ? 3 : 4;
        return new Meteo(t, h, u, cur, m, fm, fc, ros, e, gfdi, riesgo);
    }

    /** Velocidad de avance (m/s) en una dirección; coseno = coseno del ángulo con el viento. */
    public double rosDireccion(double coseno) {
        return rosCabeza * (1 - excentricidad) / Math.max(0.05, 1 - excentricidad * coseno);
    }

    /** Velocidad del fuego de copas (m/s) a favor del viento: necesita sequía y viento. */
    public double rosCopa(double coseno) {
        double fmVivo = Math.exp(-0.108 * Math.max(humedadCombustible, 6)) * Math.min(1, factorCurado + 0.15);
        double cabeza = fmVivo * (0.04 + 0.025 * viento);
        return cabeza * (1 - excentricidad) / Math.max(0.05, 1 - excentricidad * coseno);
    }

    /** Intensidad del frente (Byram, kW/m) con 0,45 kg/m² de pasto. */
    public double intensidad(double ros) {
        return 18600 * 0.45 * ros;
    }

    /** Altura de llama (m) según la intensidad. */
    public double alturaLlama(double ros) {
        return 0.0775 * Math.pow(Math.max(1, intensidad(ros)), 0.46);
    }

    private static double clamp(double v, double a, double b) {
        return Math.max(a, Math.min(b, v));
    }
}
