package es.guerramod.pueblo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;

import java.util.UUID;

/** Documento Nacional de Identidad de cada aldeano (nombre, dos apellidos, número con su letra, nacimiento, oficio, pueblo). */
public final class Dni {
    private Dni() {}

    private static final String CLAVE = "guerramod_dni";
    private static final String LETRAS = "TRWAGMYFPDXBNJZSQVHLCKE";
    private static final String[] HOMBRES = {"Hugo", "Daniel", "Pablo", "Alejandro", "Álvaro", "Adrián", "David", "Javier", "Sergio", "Diego",
            "Carlos", "Manuel", "José", "Antonio", "Francisco", "Juan", "Miguel", "Raúl", "Iván", "Jorge", "Luis", "Mario", "Rubén", "Óscar",
            "Marcos", "Andrés", "Fernando", "Alberto", "Víctor", "Ignacio", "Gonzalo", "Rodrigo", "Ángel", "Enrique", "Ramón", "Joaquín"};
    private static final String[] MUJERES = {"Lucía", "Martina", "María", "Sofía", "Julia", "Paula", "Valeria", "Carmen", "Elena", "Laura",
            "Marta", "Ana", "Isabel", "Pilar", "Rosa", "Dolores", "Teresa", "Cristina", "Nuria", "Irene", "Sara", "Alba", "Noelia", "Raquel",
            "Beatriz", "Patricia", "Silvia", "Lorena", "Inés", "Claudia", "Rocío", "Mercedes", "Concepción", "Manuela", "Eva", "Natalia"};
    private static final String[] APELLIDOS = {"García", "Fernández", "González", "Rodríguez", "López", "Martínez", "Sánchez", "Pérez",
            "Gómez", "Martín", "Jiménez", "Ruiz", "Hernández", "Díaz", "Moreno", "Muñoz", "Álvarez", "Romero", "Alonso", "Gutiérrez",
            "Navarro", "Torres", "Domínguez", "Vázquez", "Ramos", "Gil", "Ramírez", "Serrano", "Blanco", "Molina", "Morales", "Suárez",
            "Ortega", "Delgado", "Castro", "Ortiz", "Rubio", "Marín", "Sanz", "Núñez", "Iglesias", "Medina", "Garrido", "Cortés",
            "Castillo", "Santos", "Lozano", "Guerrero", "Cano", "Prieto", "Méndez", "Cruz", "Calvo", "Gallego", "Vidal", "León"};

    public static boolean tiene(Villager v) {
        return v.getPersistentData().contains(CLAVE);
    }

    public static CompoundTag datos(Villager v) {
        return v.getPersistentData().getCompound(CLAVE);
    }

    private static void guardar(Villager v, CompoundTag t) {
        v.getPersistentData().put(CLAVE, t);
    }

    /** Le hace el DNI si no lo tiene y lo apunta en el pueblo más cercano (o funda uno). */
    public static CompoundTag asegurar(ServerLevel l, Villager v) {
        if (!tiene(v)) {
            RandomSource r = l.random;
            boolean mujer = r.nextBoolean();
            CompoundTag t = new CompoundTag();
            t.putString("nombre", mujer ? MUJERES[r.nextInt(MUJERES.length)] : HOMBRES[r.nextInt(HOMBRES.length)]);
            t.putString("ap1", APELLIDOS[r.nextInt(APELLIDOS.length)]);
            t.putString("ap2", APELLIDOS[r.nextInt(APELLIDOS.length)]);
            int num = 10000000 + r.nextInt(89999999);
            t.putInt("numero", num);
            t.putString("sexo", mujer ? "M" : "H");
            // edad: los bebés nacen hoy; los adultos tienen entre 18 y 70 años (un año = 800 días de juego)
            long hoy = l.getDayTime() / 24000L;
            int edadAnios = v.isBaby() ? 0 : 18 + r.nextInt(53);
            t.putLong("nacimiento", hoy - edadAnios * 800L - r.nextInt(800));
            t.putInt("rol", Rol.NINGUNO.ordinal());
            guardar(v, t);
            if (!v.hasCustomName() || v.getCustomName().getString().isEmpty()) {
                v.setCustomName(Component.literal(t.getString("nombre") + " " + t.getString("ap1")));
            }
        }
        CompoundTag t = datos(v);
        if (!t.hasUUID("pueblo") || Pueblos.get(l).pueblo(t.getUUID("pueblo")) == null) {
            Pueblos.Pueblo p = Pueblos.get(l).cercanoOCrear(l, v.blockPosition());
            t.putUUID("pueblo", p.id);
            guardar(v, t);
        }
        return t;
    }

    public static String numero(CompoundTag t) {
        int n = t.getInt("numero");
        return String.format("%08d", n) + LETRAS.charAt(n % 23);
    }

    public static Rol rol(Villager v) {
        return tiene(v) ? Rol.de(datos(v).getInt("rol")) : Rol.NINGUNO;
    }

    public static void ponerRol(Villager v, Rol r) {
        CompoundTag t = datos(v);
        t.putInt("rol", r.ordinal());
        guardar(v, t);
    }

    public static UUID pueblo(Villager v) {
        CompoundTag t = datos(v);
        return t.hasUUID("pueblo") ? t.getUUID("pueblo") : null;
    }

    public static void ponerPueblo(Villager v, UUID p) {
        CompoundTag t = datos(v);
        t.putUUID("pueblo", p);
        guardar(v, t);
    }

    public static String nombreCompleto(CompoundTag t) {
        return t.getString("nombre") + " " + t.getString("ap1") + " " + t.getString("ap2");
    }

    /** Fecha de nacimiento con el calendario del mod (día, estación y año). */
    public static String fecha(long dia) {
        long anio = Math.floorDiv(dia, 800L);
        int d = (int) Math.floorMod(dia, 800L);
        return (d % 200 + 1) + " de " + es.guerramod.clima.Estaciones.NOMBRES[d / 200].toLowerCase() + " del año " + (anio + 2000);
    }
}
