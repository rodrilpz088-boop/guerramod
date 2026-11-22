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
                    double factorHumedad, double factorCurado, double rosCabeza, double excentricidad, double indice, int riesgo,
                    double nubes, double niebla, double turbidez, double puntoRocio, String zona) {

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
        es.guerramod.clima.Zonas.Zona z = es.guerramod.clima.Zonas.de(b);
        boolean lluvia = Fuego.llueve(level, pos);
        boolean lloviendo = level.isRaining();
        double dias = c.diasSinLluvia(level);
        double calor = c.calentamiento();

        // ---- nubosidad de la zona (los desiertos casi siempre despejados, la selva nublada)
        double nub = Float.isNaN(c.nubesFija) ? c.nubes : c.nubesFija / 100.0;
        nub = clamp(nub * z.nubes() + (lloviendo ? 0.4 : 0), 0, 1);
        if (lloviendo && z.nubes() > 0.2) nub = Math.max(nub, 0.9);

        // ---- temperatura: clima de la zona + ciclo del día (máxima hacia las 14:00, menos con nubes) + altitud
        long hora = level.getDayTime() % 24000; // 0 = 6:00, 6000 = 12:00
        double dia = Math.cos(2 * Math.PI * (hora - 8000) / 24000.0);
        // altitud: unos 0,6 °C menos cada 10 bloques por encima de 70 (gradiente del aire, algo exagerado como el relieve)
        double alt = pos.getY() > 70 ? (pos.getY() - 70) * 0.055 : 0;
        // estación del año (el máximo a finales de julio, el mínimo a finales de enero) y Navidad fría
        int diaAnio = es.guerramod.clima.Estaciones.dia(level);
        double estac = es.guerramod.clima.Estaciones.ciclo(diaAnio);
        double luzEst = es.guerramod.clima.Estaciones.cicloLuz(diaAnio);
        boolean nieve = z.fria() && z.tMedia() < 2; // zonas de nieve perpetua (tundra, taiga nevada, glaciares, cumbres)
        // la nieve refleja el sol: el día calienta mucho menos donde todo está blanco
        double oscil = z.amplitud() * (nieve ? 0.6 : 1.0);
        double t = z.tMedia() + oscil * dia * (1 - 0.55 * nub) * (0.85 + 0.15 * luzEst) - alt;
        t += es.guerramod.clima.Zonas.amplitudAnual(z) * (estac - 1) / 2;
        if (es.guerramod.clima.Estaciones.navidad(diaAnio)) t -= 4;
        if (lluvia) t -= nieve ? 1.5 : 4;
        // calentamiento global (algo más fuerte en las zonas frías: amplificación polar), con límite realista
        double cal = Math.max(-3, Math.min(4.5, calor));
        t += cal * (z.fria() ? 1.4 : 1.0);
        // ola de calor: en la nieve casi no se nota
        if (c.extraCalorTicks > 0) t += c.extraCalor * (nieve ? 0.15 : z.fria() ? 0.4 : 1.0);
        if (!Float.isNaN(c.tempFija)) {
            // /incendios temperatura: fija la de las zonas templadas; el resto cambia en proporción (la nieve muy poco)
            double delta = c.tempFija - 20.0;
            t += delta * (nieve ? 0.25 : z.fria() ? 0.55 : 1.0);
        }
        // ventisca: el aire polar desploma la temperatura (en la selva y el desierto llega más templado)
        double vent = c.ventisca();
        double bajada = 0;
        if (vent > 0) {
            double f = z.tMedia() > 25 ? 0.45 : z.tMedia() > 22 ? 0.75 : 1.0;
            bajada = c.ventiscaFrio * f * vent;
            t -= bajada;
        }
        // límites de cada zona: ni en la nieve hace 26 °C ni en el desierto -30 °C
        double anual = es.guerramod.clima.Zonas.amplitudAnual(z);
        double max = z.tMedia() + z.amplitud() + (nieve ? 3 : 7) + Math.max(0, cal);
        double min = z.tMedia() - anual - z.amplitud() - 14 - alt - bajada;
        t = clamp(t, min, Math.min(max - alt, 50));
        if (pos.getY() < 40) t += (12 - t) * Math.min(1, (40 - pos.getY()) / 30.0); // bajo tierra, temperatura estable

        // ---- humedad relativa: la zona, la hora (de noche sube), la lluvia reciente, las nubes y el calentamiento
        double h = z.humedad();
        h -= (z.humedad() < 30 ? 20 : 16) * dia;
        h += 30 * Math.exp(-dias / 0.8);
        h += 10 * nub;
        h -= 8 * estac; // veranos secos, inviernos húmedos
        h -= 2.5 * Math.max(0, calor);
        if (c.extraCalorTicks > 0) h -= c.extraCalor * 1.5;
        if (!Float.isNaN(c.humFija)) h = c.humFija; // fijada con /incendios humedad
        if (lluvia) h = 97;
        h = clamp(h, 1, 100);

        // ---- curado de la hierba (% seca): crece con los días sin llover y el calor
        double cur = 55 + 18 * dias + 1.2 * (t - 15) + 6 * Math.max(0, calor) + 25 * estac - 5; // en invierno la hierba está verde
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

        // ---- punto de rocío (Magnus) y niebla: aire casi saturado, poco viento; sobre todo de noche y al amanecer,
        //      en valles, ríos y pantanos; en la montaña, dentro de las nubes
        double gam = Math.log(h / 100.0) + 17.62 * t / (243.12 + t);
        double rocio = 243.12 * gam / (17.62 - gam);
        double dif = t - rocio;
        double nie = clamp((3.2 - dif) / 3.2, 0, 1) * clamp((14 - u) / 9, 0, 1);
        if (pos.getY() > 95) nie *= clamp(1 - (pos.getY() - 95) / 30.0, 0, 1);
        if (pos.getY() > 170 && nub > 0.6) nie = Math.max(nie, clamp((nub - 0.6) * 2.5, 0, 1) * clamp((pos.getY() - 170) / 25.0, 0, 1));
        if (lluvia) nie = Math.max(nie, 0.3);
        if (pos.getY() < 40) nie = 0;

        // ---- turbidez del aire (partículas): humedad, polvo del desierto y humo de incendios y fábricas
        double turb = 1.0 + 1.6 * Math.pow(h / 100.0, 3) + c.aerosol * 6;
        if (z.humedad() < 25) turb += 1.2;
        return new Meteo(t, h, u, cur, m, fm, fc, ros, e, gfdi, riesgo, nub, nie, turb, rocio, z.nombre());
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

    /**
     * Sensación térmica por el viento (fórmula del índice de enfriamiento, °C y km/h): con -20 °C y 80 km/h el cuerpo
     * pierde calor como a -38 °C.
     */
    public static double sensacion(double t, double vientoKmh) {
        if (t > 10 || vientoKmh < 4.8) return t;
        double v = Math.pow(vientoKmh, 0.16);
        return 13.12 + 0.6215 * t - 11.37 * v + 0.3965 * t * v;
    }
}
