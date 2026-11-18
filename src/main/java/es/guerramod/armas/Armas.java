package es.guerramod.armas;

import es.guerramod.registro.ModItems;
import es.guerramod.registro.ModSonidos;

/** Todas las armas de fuego del mod. Los números de mira/boca coinciden con los modelos 3D generados. */
public final class Armas {
    private Armas() {}

    public static final DefArma PISTOLA = new DefArma("pistola", 7f, 16f, 3, false, 15, 32, false,
            () -> ModItems.MUNICION.get(), 1, 2.2f, 0.45f, 3.2f, 0.9f, 0.82f, false, 40f,
            () -> ModSonidos.DISPARO_PISTOLA.get(), 4f, null, false,
            11.1f, 13.0f, 9.2f, 2.0f, 0.62f, 0.15f);

    public static final DefArma SUBFUSIL = new DefArma("subfusil", 5.5f, 16f, 2, true, 30, 42, false,
            () -> ModItems.MUNICION.get(), 1, 3.0f, 0.8f, 1.5f, 1.0f, 0.78f, false, 35f,
            () -> ModSonidos.DISPARO_SUBFUSIL.get(), 4f, null, false,
            11.4f, 14.0f, 8.6f, -2.0f, 0.5f, 0.35f);

    public static final DefArma FUSIL = new DefArma("fusil", 8f, 24f, 2, true, 30, 50, false,
            () -> ModItems.MUNICION_556.get(), 1, 3.2f, 0.32f, 1.9f, 0.75f, 0.68f, false, 90f,
            () -> ModSonidos.DISPARO_FUSIL.get(), 6f, null, false,
            12.25f, 10.5f, 8.75f, -12.0f, 0.45f, 0.55f);

    public static final DefArma ESCOPETA = new DefArma("escopeta", 4.5f, 13f, 16, false, 6, 11, true,
            () -> ModItems.CARTUCHOS.get(), 8, 8.0f, 6.0f, 8.0f, 2.0f, 0.85f, false, 20f,
            () -> ModSonidos.DISPARO_ESCOPETA.get(), 6f, () -> ModSonidos.CORREDERA.get(), false,
            10.7f, 14.5f, 9.5f, -10.0f, 0.45f, 0.6f);

    public static final DefArma FRANCOTIRADOR = new DefArma("francotirador", 30f, 32f, 28, false, 5, 62, false,
            () -> ModItems.MUNICION_762.get(), 1, 6.0f, 0.0f, 9.0f, 1.5f, 0.12f, true, 250f,
            () -> ModSonidos.DISPARO_FRANCOTIRADOR.get(), 9f, () -> ModSonidos.CERROJO.get(), false,
            12.0f, 18.0f, 8.9f, -16.0f, 0.42f, 0.85f);

    public static final DefArma AMETRALLADORA = new DefArma("ametralladora", 8f, 24f, 2, true, 100, 95, false,
            () -> ModItems.MUNICION_556.get(), 1, 5.0f, 1.2f, 1.6f, 1.3f, 0.75f, false, 90f,
            () -> ModSonidos.DISPARO_AMETRALLADORA.get(), 7f, null, false,
            11.75f, 11.0f, 8.8f, -14.0f, 0.42f, 1.0f);

    public static final DefArma LANZACOHETES = new DefArma("lanzacohetes", 0f, 3.2f, 20, false, 1, 60, false,
            () -> ModItems.COHETE.get(), 1, 1.5f, 0.2f, 6.0f, 1.0f, 0.7f, false, 120f,
            () -> ModSonidos.DISPARO_FUERTE.get(), 6f, null, true,
            12.0f, 12.0f, 8.5f, -12.0f, 0.4f, 0.9f);
}
