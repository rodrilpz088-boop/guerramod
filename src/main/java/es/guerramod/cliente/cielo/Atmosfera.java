package es.guerramod.cliente.cielo;

import es.guerramod.clima.TiempoCliente;
import net.minecraft.client.multiplayer.ClientLevel;

/**
 * Atmósfera física para el cielo: dispersión de Rayleigh (aire, cielo azul), de Mie (polvo, humedad y humo: el halo
 * blanco del sol y los cielos lechosos) y absorción del ozono (el azul profundo del crepúsculo), integradas a lo largo
 * de la línea de visión con la atmósfera esférica de la Tierra (modelo de Nishita). De ahí salen solos los
 * atardeceres naranjas y rosas, la sombra de la Tierra, la hora azul y el sol rojo en el horizonte.
 * Los colores se calculan en el procesador para una malla de direcciones y se pasan a la tarjeta como colores.
 */
public final class Atmosfera {
    private Atmosfera() {}

    static final double RE = 6360e3, RA = 6420e3, HR = 8000, HM = 1200;
    static final double[] BR = {5.8e-6, 13.5e-6, 33.1e-6};
    static final double[] BO = {0.65e-6, 1.881e-6, 0.085e-6};
    static final double[] ANG = {0.76, 1.0, 1.38};
    static final double E = 20.0;
    static final double G_MIE = 0.8;

    // ---------------------------------------------------------------- tabla de profundidad óptica hacia la luz
    private static final int NH = 48, NMU = 256;
    private static float[] tR, tM, tO, tL;

    private static double ozono(double h) {
        return Math.max(0, 1 - Math.abs(h - 25000) / 15000);
    }

    private static void tabla() {
        if (tR != null) return;
        float[] r1 = new float[NH * NMU], m1 = new float[NH * NMU], o1 = new float[NH * NMU], l1 = new float[NH * NMU];
        for (int hi = 0; hi < NH; hi++) {
            double u = hi / (NH - 1.0);
            double h = u * u * 60000;
            double r = RE + h;
            double muHor = -Math.sqrt(Math.max(0, 1 - (RE / r) * (RE / r)));
            for (int mi = 0; mi < NMU; mi++) {
                double mu = -1 + 2.0 * mi / (NMU - 1);
                boolean sombra = mu < muHor;
                double m = sombra ? muHor + 0.002 : mu;
                double dx = Math.sqrt(Math.max(0, 1 - m * m)), dy = m;
                double b = r * dy, c = r * r - RA * RA;
                double t = -b + Math.sqrt(Math.max(0, b * b - c));
                int pasos = 40;
                double ds = t / pasos, aR = 0, aM = 0, aO = 0;
                for (int k = 0; k < pasos; k++) {
                    double s = (k + 0.5) * ds;
                    double px = dx * s, py = r + dy * s;
                    double hh = Math.max(0, Math.sqrt(px * px + py * py) - RE);
                    aR += Math.exp(-hh / HR) * ds;
                    aM += Math.exp(-hh / HM) * ds;
                    aO += ozono(hh) * ds;
                }
                int i = hi * NMU + mi;
                r1[i] = (float) aR;
                m1[i] = (float) aM;
                o1[i] = (float) aO;
                l1[i] = sombra ? 0f : 1f;
            }
        }
        tR = r1; tM = m1; tO = o1; tL = l1;
    }

    /** Profundidad óptica hacia el sol desde altura h (m) con coseno mu: {R, M, O, iluminado}. */
    private static void luz(double h, double mu, double[] out) {
        double u = Math.sqrt(Math.max(0, Math.min(1, h / 60000))) * (NH - 1);
        double v = (Math.max(-1, Math.min(1, mu)) + 1) * 0.5 * (NMU - 1);
        int h0 = Math.min(NH - 2, (int) u), m0 = Math.min(NMU - 2, (int) v);
        double fh = u - h0, fm = v - m0;
        int a = h0 * NMU + m0, b = a + 1, c = a + NMU, d = c + 1;
        double w00 = (1 - fh) * (1 - fm), w01 = (1 - fh) * fm, w10 = fh * (1 - fm), w11 = fh * fm;
        out[0] = tR[a] * w00 + tR[b] * w01 + tR[c] * w10 + tR[d] * w11;
        out[1] = tM[a] * w00 + tM[b] * w01 + tM[c] * w10 + tM[d] * w11;
        out[2] = tO[a] * w00 + tO[b] * w01 + tO[c] * w10 + tO[d] * w11;
        out[3] = tL[a] * w00 + tL[b] * w01 + tL[c] * w10 + tL[d] * w11;
    }

    // ---------------------------------------------------------------- estado del cielo (se actualiza cada tick)
    /** Dirección al sol y a la luna (mundo). */
    public static double sx, sy, sz, mx, my, mz;
    /** Tiempo suavizado. */
    public static float nubes = 0.3f, niebla = 0, turbidez = 1.3f, lluvia = 0, luna = 1;
    /** Exposición (adaptación del ojo) y altura del observador. */
    static double exposicion = 4, h0 = 300;
    /** Color del sol y de la luna (0..1, para el disco) y su visibilidad. */
    public static final float[] COLOR_SOL = new float[3], COLOR_LUNA = new float[3];
    /** Luz del sol que llega a las nubes (lineal) y luz ambiente del cielo (lineal). */
    static final double[] LUZ_NUBES = new double[3], AMBIENTE = new double[3], LUZ_LUNA_NUBES = new double[3];
    static double[] betaM = new double[3];
    private static long ultimoTick = Long.MIN_VALUE;
    private static long ultimoNs = 0;
    static boolean listo = false;

    /** Actualiza el estado; barato si ya se hizo en este tick. */
    public static void actualizar(ClientLevel level, float parcial, double camY) {
        tabla();
        long nt = System.nanoTime();
        double dt = ultimoNs == 0 ? 1 : Math.min(1.0, (nt - ultimoNs) / 1e9);
        ultimoNs = nt;
        // suavizado del tiempo (unos segundos), para que los cambios no salten
        float k = (float) (1 - Math.exp(-dt / 4.0));
        float kn = (float) (1 - Math.exp(-dt / 8.0));
        float lluviaAhora = level.getRainLevel(parcial);
        float objNubes = Math.max(TiempoCliente.nubes, lluviaAhora * 0.97f);
        nubes += (objNubes - nubes) * kn;
        niebla += (TiempoCliente.niebla - niebla) * kn;
        turbidez += (TiempoCliente.turbidez - turbidez) * k;
        lluvia = lluviaAhora;
        luna = level.getMoonBrightness();

        double a = level.getTimeOfDay(parcial) * Math.PI * 2;
        sx = -Math.sin(a);
        sy = Math.cos(a);
        sz = 0;
        mx = -sx; my = -sy; mz = 0;
        h0 = 200 + Math.max(0, camY - 63) * 8;
        double ax = 0.5 * 21e-6 * turbidez;
        for (int c = 0; c < 3; c++) betaM[c] = ax * ANG[c];

        long tick = level.getGameTime();
        if (tick == ultimoTick && listo) return;
        ultimoTick = tick;

        // exposición: el ojo se adapta a la luminosidad del cénit (cielo despejado)
        double[] z = new double[3];
        radiancia(0, 1, 0, z, true);
        double yz = 0.2126 * z[0] + 0.7152 * z[1] + 0.0722 * z[2];
        exposicion = 0.82 / Math.pow(yz + 0.0034, 0.75);

        // color del sol y la luna vistos desde aquí
        double[] l = new double[4];
        luz(h0, sy, l);
        colorDisco(l, COLOR_SOL);
        luz(h0, my, l);
        colorDisco(l, COLOR_LUNA);
        // luz que llega a las nubes (a unos 2 km)
        luz(2000, sy, l);
        for (int c = 0; c < 3; c++) LUZ_NUBES[c] = E * Math.exp(-(BR[c] * l[0] + betaM[c] * 1.1 * l[1] + BO[c] * l[2])) * l[3];
        luz(2000, my, l);
        for (int c = 0; c < 3; c++) LUZ_LUNA_NUBES[c] = E * 0.0016 * luna * Math.exp(-(BR[c] * l[0] + betaM[c] * 1.1 * l[1] + BO[c] * l[2])) * l[3];
        System.arraycopy(z, 0, AMBIENTE, 0, 3);
        listo = true;
    }

    private static void colorDisco(double[] l, float[] out) {
        double max = 1e-9;
        double[] t = new double[3];
        for (int c = 0; c < 3; c++) {
            t[c] = Math.exp(-(BR[c] * l[0] + betaM[c] * 1.1 * l[1] + BO[c] * l[2]));
            max = Math.max(max, t[c]);
        }
        for (int c = 0; c < 3; c++) out[c] = (float) Math.min(1, t[c] / max * 1.0 + 0.04);
    }

    /**
     * Radiancia (lineal) del cielo despejado en la dirección (dx, dy, dz). Integra la luz del sol (y de la luna de noche)
     * dispersada a lo largo del rayo de visión.
     */
    static void radiancia(double dx, double dy, double dz, double[] out, boolean conLuna) {
        out[0] = out[1] = out[2] = 0;
        dispersion(dx, dy, dz, sx, sy, sz, E, out);
        if (conLuna && sy < -0.05 && my > -0.1) dispersion(dx, dy, dz, mx, my, mz, E * 0.0016 * luna, out);
        // dispersión múltiple aproximada: luz azulada difusa mientras el sol ilumina la alta atmósfera
        double th = Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, sy))));
        double amb = E * 0.0021 / (1 + Math.exp(-(th + 5.5) / 1.9));
        double f = 0.6 + 0.4 * Math.max(0, dy);
        out[0] += amb * 0.32 * f + 0.0003;
        out[1] += amb * 0.52 * f + 0.0004;
        out[2] += amb * 1.0 * f + 0.0009;
    }

    private static void dispersion(double dx, double dy, double dz, double lx, double ly, double lz, double intensidad, double[] out) {
        final int N = 12;
        double oy = RE + h0;
        double b = oy * dy;
        double cA = oy * oy - RA * RA;
        double tmax = -b + Math.sqrt(Math.max(0, b * b - cA));
        if (dy < 0) {
            double cG = oy * oy - RE * RE;
            double disc = b * b - cG;
            if (disc > 0) {
                double tg = -b - Math.sqrt(disc);
                if (tg > 0) tmax = tg;
            }
        }
        double odR = 0, odM = 0, odO = 0;
        double sR0 = 0, sR1 = 0, sR2 = 0, sM0 = 0, sM1 = 0, sM2 = 0;
        double[] l = new double[4];
        for (int i = 0; i < N; i++) {
            double u0 = (double) i / N, u1 = (double) (i + 1) / N;
            u0 *= u0;
            u1 *= u1;
            double t = tmax * (u0 + u1) * 0.5, ds = tmax * (u1 - u0);
            double px = dx * t, py = oy + dy * t, pz = dz * t;
            double r = Math.sqrt(px * px + py * py + pz * pz);
            double h = Math.max(0, r - RE);
            double hr = Math.exp(-h / HR) * ds, hm = Math.exp(-h / HM) * ds, ho = ozono(h) * ds;
            odR += hr;
            odM += hm;
            odO += ho;
            double mus = (px * lx + py * ly + pz * lz) / r;
            luz(h, mus, l);
            if (l[3] <= 0.001) continue;
            double tR = odR + l[0], tM = (odM + l[1]) * 1.1, tO = odO + l[2];
            double a0 = Math.exp(-(BR[0] * tR + betaM[0] * tM + BO[0] * tO)) * l[3];
            double a1 = Math.exp(-(BR[1] * tR + betaM[1] * tM + BO[1] * tO)) * l[3];
            double a2 = Math.exp(-(BR[2] * tR + betaM[2] * tM + BO[2] * tO)) * l[3];
            sR0 += a0 * hr; sR1 += a1 * hr; sR2 += a2 * hr;
            sM0 += a0 * hm; sM1 += a1 * hm; sM2 += a2 * hm;
        }
        double mu = dx * lx + dy * ly + dz * lz;
        double pR = 3.0 / (16 * Math.PI) * (1 + mu * mu);
        double g = G_MIE;
        double pM = 3.0 / (8 * Math.PI) * ((1 - g * g) * (1 + mu * mu)) / ((2 + g * g) * Math.pow(1 + g * g - 2 * g * mu, 1.5));
        out[0] += intensidad * (sR0 * BR[0] * pR + sM0 * betaM[0] * pM);
        out[1] += intensidad * (sR1 * BR[1] * pR + sM1 * betaM[1] * pM);
        out[2] += intensidad * (sR2 * BR[2] * pR + sM2 * betaM[2] * pM);
    }

    /** Nubes, lluvia y polvo: el cielo se vuelve gris y más oscuro (en lineal). */
    static void tiempo(double[] c, double dy) {
        double cubierto = Math.pow(nubes, 1.6) * 0.85;
        cubierto = Math.max(cubierto, lluvia * 0.92);
        if (cubierto <= 0.001) return;
        double y = 0.2126 * c[0] + 0.7152 * c[1] + 0.0722 * c[2];
        double gris = y * (0.95 - 0.45 * lluvia) * (dy > 0 ? 1.0 : 0.9);
        c[0] = c[0] * (1 - cubierto) + gris * 0.95 * cubierto;
        c[1] = c[1] * (1 - cubierto) + gris * 0.97 * cubierto;
        c[2] = c[2] * (1 - cubierto) + gris * 1.02 * cubierto;
    }

    /** Lineal -> pantalla (adaptación del ojo + curva tonal + algo de saturación). */
    static void pantalla(double[] c, float[] out, int off) {
        double r = Math.pow(1 - Math.exp(-c[0] * exposicion), 1 / 1.5);
        double g = Math.pow(1 - Math.exp(-c[1] * exposicion), 1 / 1.5);
        double b = Math.pow(1 - Math.exp(-c[2] * exposicion), 1 / 1.5);
        double y = 0.2126 * r + 0.7152 * g + 0.0722 * b;
        out[off] = (float) Math.max(0, Math.min(1, y + (r - y) * 1.18));
        out[off + 1] = (float) Math.max(0, Math.min(1, y + (g - y) * 1.18));
        out[off + 2] = (float) Math.max(0, Math.min(1, y + (b - y) * 1.18));
    }

    /** Color de pantalla del cielo en una dirección, con nubes y lluvia (sin niebla). */
    public static void colorCielo(double dx, double dy, double dz, float[] out, int off) {
        double[] c = new double[3];
        double ey = Math.max(dy, 0.02); // el rayo justo horizontal exagera el enrojecimiento (falta la dispersión múltiple)
        double n = Math.sqrt(dx * dx + ey * ey + dz * dz);
        radiancia(dx / n, ey / n, dz / n, c, true);
        tiempo(c, dy);
        pantalla(c, out, off);
    }

    /** Color de la niebla mirando en horizontal hacia (dx, dz). */
    public static float[] colorNiebla(double dx, double dz) {
        float[] o = new float[3];
        double n = Math.sqrt(dx * dx + dz * dz);
        if (n < 1e-6) { dx = 1; n = 1; }
        colorCielo(dx / n, 0.035, dz / n, o, 0);
        // la niebla densa es de un gris blanquecino que recoge la luz de todo el cielo
        if (niebla > 0.01f) {
            float y = 0.2126f * o[0] + 0.7152f * o[1] + 0.0722f * o[2];
            float gris = Math.min(1, y * 1.05f + 0.03f);
            float w = Math.min(1, niebla * 0.85f);
            o[0] += (gris * 0.97f - o[0]) * w;
            o[1] += (gris * 0.99f - o[1]) * w;
            o[2] += (gris * 1.02f - o[2]) * w;
        }
        return o;
    }

    /** Color de una nube vista en la dirección dada (pantalla), iluminada por el sol (y la luna) y el cielo. */
    static void colorNube(double dx, double dy, double dz, float[] out, int off) {
        double mu = dx * sx + dy * sy + dz * sz;
        double g = 0.6;
        double hg = (1 - g * g) / (4 * Math.PI * Math.pow(1 + g * g - 2 * g * mu, 1.5));
        double fase = 0.45 + 2.2 * hg;
        double mum = dx * mx + dy * my + dz * mz;
        double hgm = (1 - g * g) / (4 * Math.PI * Math.pow(1 + g * g - 2 * g * mum, 1.5));
        double fasem = 0.45 + 2.2 * hgm;
        double oscuro = 1 - 0.55 * lluvia;
        double[] c = new double[3];
        for (int i = 0; i < 3; i++) {
            c[i] = (LUZ_NUBES[i] * 0.065 * fase + LUZ_LUNA_NUBES[i] * 0.06 * fasem + AMBIENTE[i] * 0.75) * oscuro;
        }
        pantalla(c, out, off);
    }
}
