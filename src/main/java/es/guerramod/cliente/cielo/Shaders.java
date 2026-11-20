package es.guerramod.cliente.cielo;

/**
 * Detecta si hay un paquete de sombreadores activo en Oculus/Iris. Si lo hay, el mod deja que el shader dibuje el cielo,
 * las nubes, la lluvia, la niebla y el suelo mojado (si no, se pisarían). Se consulta por reflexión para no depender de Oculus.
 */
public final class Shaders {
    private Shaders() {}

    private static long ultimo = 0;
    private static boolean activo = false;
    private static Object api;
    private static java.lang.reflect.Method metodo;
    private static boolean sinIris = false;

    public static boolean activos() {
        if (sinIris) return false;
        long t = System.currentTimeMillis();
        if (t - ultimo < 1000) return activo;
        ultimo = t;
        try {
            if (metodo == null) {
                Class<?> c = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                api = c.getMethod("getInstance").invoke(null);
                metodo = c.getMethod("isShaderPackInUse");
            }
            activo = (Boolean) metodo.invoke(api);
        } catch (Throwable e) {
            sinIris = true;
            activo = false;
        }
        return activo;
    }
}
