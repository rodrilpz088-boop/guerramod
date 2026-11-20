package es.guerramod.clima;

/**
 * Tiempo que el servidor manda a cada jugador (sin clases del cliente, para que también cargue en servidores).
 * El cielo, las nubes, la niebla y el minimapa lo leen.
 */
public final class TiempoCliente {
    private TiempoCliente() {}

    public static volatile float temperatura = 15, humedad = 50, nubes = 0.3f, niebla = 0, turbidez = 1.3f;
    public static volatile float anomalia = 0, co2 = 420;
    public static volatile boolean cambio = true;
    public static volatile String zona = "";
    /** Tamaño del granizo que cae (cm, 0 = no graniza) y cuánto tiembla el suelo (0..1). */
    public static volatile float granizo = 0, temblor = 0;
    /** El servidor ya ha mandado datos (si no, el cielo usa valores por defecto). */
    public static volatile boolean recibido = false;
}
