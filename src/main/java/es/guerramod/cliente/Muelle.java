package es.guerramod.cliente;

/** Muelle amortiguado (masa-muelle). Da a las armas peso e inercia. */
public class Muelle {
    public float pos, vel;

    /**
     * @param objetivo   posición de reposo
     * @param dt         segundos desde el último fotograma
     * @param rigidez    fuerza del muelle (más = vuelve antes)
     * @param amortiguar rozamiento (más = menos rebote)
     */
    public void paso(float objetivo, float dt, float rigidez, float amortiguar) {
        // subpasos para que sea estable aunque vaya a pocos FPS
        int n = Math.max(1, (int) Math.ceil(dt / 0.008f));
        float h = dt / n;
        for (int i = 0; i < n; i++) {
            float a = -rigidez * (pos - objetivo) - amortiguar * vel;
            vel += a * h;
            pos += vel * h;
        }
    }

    public void impulso(float v) {
        vel += v;
    }

    public void reset() {
        pos = 0;
        vel = 0;
    }
}
