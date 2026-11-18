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

        if (level().isClientSide) {
            alabeoO = alabeo;
            float giro = Mth.wrapDegrees(getYRot() - yRotO);
            alabeo += (Mth.clamp(-giro * 14.0f, -60.0f, 60.0f) - alabeo) * 0.15f;
        }
        if (level().isClientSide && getSalud() < saludMaxima() * 0.4f && random.nextInt(2) == 0) {
            level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY() + 0.8, getZ(), 0, 0.05, 0);
        }
    }

    /** Alabeo visual (solo cliente): el avión se inclina al girar. */
    public float alabeo, alabeoO;

    private void volar(Player p) {
        // el avión sigue la mirada del piloto con un giro máximo que depende de la velocidad
        float eficacia = Mth.clamp(velocidad / VEL_MAX, 0.0f, 1.0f);
        float maxGiro = onGround() ? 2.5f : 0.6f + 3.2f * eficacia;
        float dy = Mth.clamp(Mth.wrapDegrees(p.getYRot() - getYRot()), -maxGiro, maxGiro);
        float objPitch = Mth.clamp(p.getXRot(), -55.0f, 55.0f);
        if (onGround() && velocidad < VEL_SUSTENTACION) objPitch = Math.min(objPitch, 0.0f) * 0.3f;
        float dp = Mth.clamp(objPitch - getXRot(), -2.2f * Math.max(0.3f, eficacia), 2.2f * Math.max(0.3f, eficacia));
        setYRot(getYRot() + dy);
        setXRot(getXRot() + dp);

        // motor: W acelera, S frena; picar acelera y subir frena (energía)
        if (p.zza > 0) velocidad += 0.022f;
        else if (p.zza < 0) velocidad -= 0.045f;
        else velocidad -= 0.003f;
        velocidad += (float) (Math.sin(Math.toRadians(getXRot())) * 0.012);
        if (onGround()) velocidad -= 0.004f; // rozamiento de las ruedas
        velocidad = Mth.clamp(velocidad, 0.0f, VEL_MAX);

        Vec3 dir = Vec3.directionFromRotation(getXRot(), getYRot());
        Vec3 objetivo = dir.scale(velocidad);
        // inercia: la velocidad real se acerca poco a poco a la dirección del morro
        Vec3 dm = getDeltaMovement().lerp(objetivo, onGround() ? 0.3 : 0.12);
        if (velocidad < VEL_SUSTENTACION) {
            // por debajo de la velocidad de sustentación pierde altura
            double caida = 0.08 * (1.0 - velocidad / VEL_SUSTENTACION);
            dm = dm.add(0, -caida, 0);
        }
        if (onGround() && dm.y < 0) dm = new Vec3(dm.x, 0, dm.z);
        if (horizontalCollision) velocidad *= 0.4f;
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
            BalaEntity b = new BalaEntity(level(), p, 7.0f, 80f);
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
