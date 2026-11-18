package es.guerramod.radiacion;

import es.guerramod.registro.ModBloques;
import es.guerramod.registro.ModEfectos;
import es.guerramod.registro.ModEtiquetas;
import es.guerramod.registro.ModItems;
import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** Zonas radiactivas que dejan las bombas nucleares. */
public class DatosRadiacion extends SavedData {
    private static final String NOMBRE = "guerramod_radiacion";

    public record Zona(String dimension, double x, double y, double z, double radio, long fin) {}

    public final List<Zona> zonas = new ArrayList<>();

    public static DatosRadiacion get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(DatosRadiacion::cargar, DatosRadiacion::new, NOMBRE);
    }

    public static DatosRadiacion cargar(CompoundTag t) {
        DatosRadiacion d = new DatosRadiacion();
        for (Tag x : t.getList("zonas", Tag.TAG_COMPOUND)) {
            CompoundTag z = (CompoundTag) x;
            d.zonas.add(new Zona(z.getString("dim"), z.getDouble("x"), z.getDouble("y"), z.getDouble("z"),
                    z.getDouble("r"), z.getLong("fin")));
        }
        return d;
    }

    @Override
    public CompoundTag save(CompoundTag t) {
        ListTag l = new ListTag();
        for (Zona z : zonas) {
            CompoundTag c = new CompoundTag();
            c.putString("dim", z.dimension());
            c.putDouble("x", z.x());
            c.putDouble("y", z.y());
            c.putDouble("z", z.z());
            c.putDouble("r", z.radio());
            c.putLong("fin", z.fin());
            l.add(c);
        }
        t.put("zonas", l);
        return t;
    }

    public void anadir(ServerLevel level, Vec3 c, double radio, int duracionTicks) {
        zonas.add(new Zona(level.dimension().location().toString(), c.x, c.y, c.z, radio,
                level.getServer().overworld().getGameTime() + duracionTicks));
        setDirty();
    }

    /** Intensidad de 0 a 1 en una posición (0 = sin radiación). */
    public float intensidad(Level level, Vec3 pos) {
        String dim = level.dimension().location().toString();
        float max = 0;
        for (Zona z : zonas) {
            if (!z.dimension().equals(dim)) continue;
            double dx = pos.x - z.x(), dz = pos.z - z.z();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d < z.radio()) max = Math.max(max, (float) (1.0 - d / z.radio()));
        }
        return max;
    }

    /** Se llama cada segundo desde el servidor. */
    public void tick(MinecraftServer server) {
        long ahora = server.overworld().getGameTime();
        if (zonas.removeIf(z -> z.fin() < ahora)) setDirty();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isCreative() || p.isSpectator()) continue;
            float i = zonas.isEmpty() ? 0 : intensidad(p.level(), p.position());
            boolean geiger = p.getMainHandItem().is(ModItems.GEIGER.get()) || p.getOffhandItem().is(ModItems.GEIGER.get());
            if (i <= 0) continue;
            ServerLevel sl = p.serverLevel();
            sl.sendParticles(p, ParticleTypes.WHITE_ASH, false, p.getX(), p.getY() + 1, p.getZ(), 30, 6, 3, 6, 0.01);
            if (geiger) {
                sl.playSound(null, p.getX(), p.getY(), p.getZ(), ModSonidos.GEIGER.get(), SoundSource.PLAYERS, 0.6f, 0.8f + i);
                p.displayClientMessage(Component.literal("☢ Radiación: " + Math.round(i * 100) + "%")
                        .withStyle(i < 0.4f ? ChatFormatting.YELLOW : ChatFormatting.RED), true);
            }
            if (protegido(p)) continue;
            int nivel = i > 0.7f ? 2 : i > 0.35f ? 1 : 0;
            p.addEffect(new MobEffectInstance(ModEfectos.RADIACION.get(), 200, nivel));
        }
    }

    public static boolean trajeNBQ(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.MASCARA_NBQ.get())
                && e.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.TRAJE_NBQ.get())
                && e.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.PANTALON_NBQ.get())
                && e.getItemBySlot(EquipmentSlot.FEET).is(ModItems.BOTAS_NBQ.get());
    }

    /** Protegido: traje NBQ, pastillas de yodo, purificador cerca o un techo de búnker encima. */
    public static boolean protegido(LivingEntity e) {
        if (trajeNBQ(e)) return true;
        if (e.hasEffect(ModEfectos.PROTECCION_RADIACION.get())) return true;
        return enBunker(e.level(), e.blockPosition());
    }

    public static boolean enBunker(Level level, BlockPos pos) {
        // purificador de aire a menos de 6 bloques
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int x = -6; x <= 6; x++)
            for (int y = -3; y <= 3; y++)
                for (int z = -6; z <= 6; z++) {
                    m.set(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
                    if (level.getBlockState(m).is(ModBloques.PURIFICADOR_AIRE.get())) return true;
                }
        // techo de hormigón reforzado o plomo encima (hasta 12 bloques) y sin cielo
        if (level.canSeeSky(pos.above())) return false;
        for (int y = 1; y <= 12; y++) {
            if (level.getBlockState(pos.above(y)).is(ModEtiquetas.ANTI_RADIACION)) return true;
        }
        return false;
    }
}
