package es.guerramod.pueblo;

/** Oficios que se pueden dar a un aldeano con la tablet municipal. */
public enum Rol {
    NINGUNO("Sin oficio", "espera a que el representante o tú le deis trabajo", 0xAAAAAA),
    REPRESENTANTE("Representante", "organiza el pueblo: reparte los oficios que hagan falta", 0xFFD24A),
    CONSTRUCTOR("Constructor", "levanta casas nuevas para que el pueblo crezca", 0xE0A060),
    AGRICULTOR("Agricultor", "hace huertos y cultiva", 0x8BD35A),
    CAMINERO("Peón caminero", "hace caminos y calles entre las casas y el centro", 0xC8B48C),
    FAROLERO("Electricista", "pone farolas por las calles y, en Navidad, luces", 0xFFF08A),
    BOMBERO("Bombero", "apaga los incendios cerca del pueblo", 0xFF5A4A),
    DEFENSA("Defensa", "protege el pueblo de monstruos y va a la guerra", 0x6A8AFF),
    VENDEDOR("Comerciante", "vende y compra (comercio de aldeano)", 0x5AD0C0),
    POLICIA("Policía Nacional", "patrulla las calles, detiene a los monstruos y protege a los vecinos", 0x3A7BFF);

    public final String nombre, descripcion;
    public final int color;

    Rol(String nombre, String descripcion, int color) {
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.color = color;
    }

    public static Rol de(int i) {
        Rol[] v = values();
        return i >= 0 && i < v.length ? v[i] : NINGUNO;
    }
}
