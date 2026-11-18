package es.guerramod.entidades;

import es.guerramod.registro.ModEntidades;
import es.guerramod.registro.ModItems;
import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;

/** Avión de combate pilotable con ametralladora y bombas. */
public class AvionEntity extends AeronaveBase {
    private static final float VEL_MAX = 1.6f;
    private static final float VEL_SUSTENTACION = 0.55f;

    private float velocidad;
    private int cdDisparo, cdBomba, cdSonido;

    public AvionEntity(EntityType<? extends AvionEntity> tipo, Level level) {
        super(tipo, level);
        setSalud(saludMaxima());
    }

    public AvionEntity(Level level, double x, double y, double z) {
        this(ModEntidades.AVION.get(), level);
        setPos(x, y, z);
        xo = x;
        yo = y;
        zo = z;
    }

    @Override
    public float saludMaxima() {
        return 60.0f;
    }

    public float getVelocidad() {
        return velocidad;
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand mano) {
        if (p.isSecondaryUseActive() || isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide) return p.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof Player p ? p : null;
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.45;
    }

    @Override
    protected boolean recoger(Player p) {
        spawnAtLocation(new ItemStack(ModItems.AVION_CAZA.get()));
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        if (cdDisparo > 0) cdDisparo--;
        if (cdBomba > 0) cdBomba--;

        LivingEntity piloto = getControllingPassenger();
        if (piloto instanceof Player jugador) {
            if (isControlledByLocalInstance()) {
                volar(jugador);
                move(MoverType.SELF, getDeltaMovement());
            }
            jugador.fallDistance = 0;
            if (!level().isClientSide && --cdSonido <= 0) {
                cdSonido = 30;
                level().playSound(null, getX(), getY(), getZ(), SoundEvents.ELYTRA_FLYING, SoundSource.NEUTRAL, 1.2f, 0.6f);
            }
        } else if (!level().isClientSide) {
            // sin piloto: planea hasta el suelo
            velocidad = 0;
            Vec3 dm = getDeltaMovement().multiply(0.9, 1.0, 0.9).add(0, -0.06, 0);
            if (onGround()) dm = new Vec3(dm.x * 0.5, Math.max(0, dm.y), dm.z * 0.5);
            setDeltaMovement(dm);
            move(MoverType.SELF, dm);
        }
        fallDistance = 0;

        if (level().isClientSide && getSalud() < saludMaxima() * 0.4f && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 0, 0.05, 0);
        }
    }

    private void volar(Player p) {
        setYRot(p.getYRot());
        setXRot(Mth.clamp(p.getXRot(), -60.0f, 60.0f));
        if (p.zza > 0) velocidad = Math.min(VEL_MAX, velocidad + 0.025f);
        else if (p.zza < 0) velocidad = Math.max(0, velocidad - 0.05f);
        else velocidad = Math.max(0, velocidad - 0.002f);

        Vec3 dir = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 dm = dir.scale(velocidad);
        if (velocidad < VEL_SUSTENTACION) {
            // pérdida: sin velocidad no vuela
            double caida = 0.4 * (1.0 - velocidad / VEL_SUSTENTACION);
            dm = new Vec3(dm.x, Math.min(dm.y, 0) - caida, dm.z);
        }
        if (onGround() && dm.y < 0) dm = new Vec3(dm.x, 0, dm.z);
        if (horizontalCollision) velocidad *= 0.5f;
        setDeltaMovement(dm);
    }

    /** Ametralladora (lado servidor). */
    public void disparar(ServerPlayer p) {
        if (cdDisparo > 0) return;
        cdDisparo = 3;
        Vec3 dir = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 lado = new Vec3(-dir.z, 0, dir.x);
        if (lado.lengthSqr() < 1.0E-4) lado = new Vec3(1, 0, 0);
        lado = lado.normalize();
        for (int s = -1; s <= 1; s += 2) {
            Vec3 o = position().add(0, 0.5, 0).add(dir.scale(2.4)).add(lado.scale(1.4 * s));
            BalaEntity b = new BalaEntity(level(), p, 7.0f, 1.2f);
            b.setPos(o.x, o.y, o.z);
            b.shoot(dir.x, dir.y, dir.z, 7.0f, 1.0f);
            level().addFreshEntity(b);
        }
        level().playSound(null, getX(), getY(), getZ(), ModSonidos.DISPARO.get(), SoundSource.PLAYERS, 2.0f, 1.3f);
    }

    /** Suelta una bomba aérea (lado servidor). */
    public void soltarBomba(ServerPlayer p) {
        if (cdBomba > 0) return;
        if (!p.getAbilities().instabuild) {
            ItemStack bomba = null;
            for (ItemStack st : p.getInventory().items) {
                if (st.is(ModItems.BOMBA_AEREA.get())) {
                    bomba = st;
                    break;
                }
            }
            if (bomba == null) {
                p.displayClientMessage(Component.literal("No llevas Bombas aéreas").withStyle(ChatFormatting.RED), true);
                cdBomba = 10;
                return;
            }
            bomba.shrink(1);
        }
        cdBomba = 12;
        Vec3 vel = new Vec3(getX() - xo, getY() - yo, getZ() - zo).scale(0.8);
        BombaAereaEntity b = new BombaAereaEntity(level(), position().add(0, -0.8, 0), vel, p);
        level().addFreshEntity(b);
        level().playSound(null, getX(), getY(), getZ(), SoundEvents.DISPENSER_LAUNCH, SoundSource.PLAYERS, 1.5f, 0.6f);
    }
}
