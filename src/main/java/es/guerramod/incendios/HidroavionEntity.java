package es.guerramod.incendios;

import es.guerramod.entidades.AeronaveBase;
import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.registro.ModEntidades;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

/** Hidroavión de extinción (tipo Canadair): vuela bajo hasta el incendio y suelta 6.000 litros de agua en una franja. */
public class HidroavionEntity extends AeronaveBase {
    private static final double VEL = 0.9;
    private static final double GRAVEDAD_BOMBA = 0.06;

    @Nullable
    private BlockPos objetivo;
    private double altura;
    @Nullable
    private UUID dueno;
    private int fase, contador;
    private Vec3 rumbo = new Vec3(0, 0, 1);

    public HidroavionEntity(EntityType<? extends HidroavionEntity> tipo, Level level) {
        super(tipo, level);
        setSalud(saludMaxima());
        noPhysics = true;
    }

    public HidroavionEntity(Level level, Vec3 inicio, BlockPos objetivo, double altura, Player quien) {
        this(ModEntidades.HIDROAVION.get(), level);
        setPos(inicio.x, inicio.y, inicio.z);
        this.objetivo = objetivo;
        this.altura = altura;
        this.dueno = quien.getUUID();
        setColor(0xF2C21B);
        Vec3 d = Vec3.atCenterOf(objetivo).subtract(inicio);
        if (d.x * d.x + d.z * d.z > 0.01) rumbo = new Vec3(d.x, 0, d.z).normalize();
        orientar(rumbo);
    }

    @Override
    public float saludMaxima() {
        return 80.0f;
    }

    private void orientar(Vec3 v) {
        setYRot((float) (Mth.atan2(v.z, v.x) * (180.0 / Math.PI)) - 90.0f);
        setXRot(0);
    }

    @Nullable
    private Entity getDueno() {
        return dueno == null ? null : level().getPlayerByUUID(dueno);
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        if (level().isClientSide) {
            if (random.nextInt(3) == 0) level().addParticle(ParticleTypes.SMOKE, getX(), getY() + 0.4, getZ(), 0, 0, 0);
            return;
        }
        if (objetivo == null) {
            discard();
            return;
        }
        contador++;
        if (contador % 40 == 0) {
            level().playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 4.0f, 0.5f);
        }
        Vec3 destino = new Vec3(objetivo.getX() + 0.5, altura, objetivo.getZ() + 0.5);
        Vec3 dm;
        if (fase == 0) {
            Vec3 d = destino.subtract(position());
            Vec3 h = new Vec3(d.x, 0, d.z);
            // las bombas avanzan un poco al caer: empezamos a soltarlas antes de llegar
            double caida = Math.max(1, getY() - objetivo.getY());
            double adelanto = 2;
            if (h.length() < adelanto + 14 || contador > 600) {
                fase = 1;
                contador = 0;
            } else {
                rumbo = h.normalize();
            }
            dm = rumbo.scale(VEL).add(0, Mth.clamp((altura - getY()) * 0.1, -0.3, 0.3), 0);
        } else if (fase == 1) {
            dm = rumbo.scale(VEL);
            soltarAgua();
            if (contador > 34) {
                fase = 2;
                contador = 0;
            }
        } else {
            dm = rumbo.scale(VEL).add(0, 0.12, 0);
            if (contador > 220) discard();
        }
        setDeltaMovement(dm);
        setPos(getX() + dm.x, getY() + dm.y, getZ() + dm.z);
        orientar(dm);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        objetivo = t.contains("objetivo") ? BlockPos.of(t.getLong("objetivo")) : null;
        altura = t.getDouble("altura");
        dueno = t.hasUUID("dueno") ? t.getUUID("dueno") : null;
        fase = t.getInt("fase");
        contador = t.getInt("contador");
        rumbo = new Vec3(t.getDouble("rx"), 0, t.getDouble("rz"));
        if (rumbo.lengthSqr() < 1.0E-4) rumbo = new Vec3(0, 0, 1);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        if (objetivo != null) t.putLong("objetivo", objetivo.asLong());
        t.putDouble("altura", altura);
        if (dueno != null) t.putUUID("dueno", dueno);
        t.putInt("fase", fase);
        t.putInt("contador", contador);
        t.putDouble("rx", rumbo.x);
        t.putDouble("rz", rumbo.z);
    }

    private void soltarAgua() {
        if (!(level() instanceof ServerLevel sl)) return;
        int x = (int) Math.floor(getX()), z = (int) Math.floor(getZ());
        int suelo = sl.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        Vec3 abajo = new Vec3(getX(), suelo + 1, getZ());
        Fuego.apagarZona(sl, abajo, 4.5, 3600);
        Fuego.apagarZona(sl, abajo.add(0, 3, 0), 4.0, 0);
        sl.sendParticles(new BlockParticleOption(net.minecraft.core.particles.ParticleTypes.BLOCK, Blocks.WATER.defaultBlockState()),
                getX(), getY() - 1, getZ(), 60, 1.2, (getY() - suelo) / 2.0, 1.2, 0.2);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SPLASH, getX(), suelo + 1, getZ(), 40, 3.0, 0.2, 3.0, 0.3);
        if (contador % 6 == 0) sl.playSound(null, getX(), suelo + 1, getZ(), SoundEvents.GENERIC_SPLASH, SoundSource.HOSTILE, 4.0f, 0.6f);
    }
}
