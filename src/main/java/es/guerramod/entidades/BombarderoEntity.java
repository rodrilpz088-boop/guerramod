package es.guerramod.entidades;

import es.guerramod.pais.Pais;
import es.guerramod.pais.RegistroPaises;
import es.guerramod.registro.ModEntidades;
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

/** Bombardero automático: vuela hasta el objetivo, suelta una alfombra de bombas y se va. */
public class BombarderoEntity extends AeronaveBase {
    private static final double VEL = 0.9;
    private static final double GRAVEDAD_BOMBA = 0.06;

    @Nullable
    private BlockPos objetivo;
    private double altura;
    @Nullable
    private UUID dueno;
    private int fase, contador;
    private Vec3 rumbo = new Vec3(0, 0, 1);

    public BombarderoEntity(EntityType<? extends BombarderoEntity> tipo, Level level) {
        super(tipo, level);
        setSalud(saludMaxima());
        noPhysics = true;
    }

    public BombarderoEntity(Level level, Vec3 inicio, BlockPos objetivo, double altura, Player quien) {
        this(ModEntidades.BOMBARDERO.get(), level);
        setPos(inicio.x, inicio.y, inicio.z);
        this.objetivo = objetivo;
        this.altura = altura;
        this.dueno = quien.getUUID();
        Pais pais = RegistroPaises.de(level).paisDeJugador(quien.getUUID());
        if (pais != null) setColor(pais.color);
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
            double adelanto = VEL * 0.5 * Math.sqrt(2 * caida / GRAVEDAD_BOMBA) * 0.85;
            if (h.length() < adelanto + 14 || contador > 600) {
                fase = 1;
                contador = 0;
            } else {
                rumbo = h.normalize();
            }
            dm = rumbo.scale(VEL).add(0, Mth.clamp((altura - getY()) * 0.1, -0.3, 0.3), 0);
        } else if (fase == 1) {
            dm = rumbo.scale(VEL);
            if (contador % 3 == 0) {
                BombaAereaEntity b = new BombaAereaEntity(level(), position().add(0, -1.6, 0), dm.scale(0.5), getDueno());
                level().addFreshEntity(b);
            }
            if (contador > 32) {
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
}
