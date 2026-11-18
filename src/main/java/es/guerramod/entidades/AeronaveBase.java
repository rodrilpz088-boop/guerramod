package es.guerramod.entidades;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkHooks;

/** Base de los aviones: vida, color del país e interpolación suave del movimiento. */
public abstract class AeronaveBase extends Entity {
    private static final EntityDataAccessor<Float> SALUD = SynchedEntityData.defineId(AeronaveBase.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> COLOR = SynchedEntityData.defineId(AeronaveBase.class, EntityDataSerializers.INT);

    private int lerpPasos;
    private double lerpX, lerpY, lerpZ, lerpYRot, lerpXRot;

    protected AeronaveBase(EntityType<?> tipo, Level level) {
        super(tipo, level);
        this.blocksBuilding = true;
        setNoGravity(true);
    }

    public abstract float saludMaxima();

    @Override
    protected void defineSynchedData() {
        entityData.define(SALUD, 40.0f);
        entityData.define(COLOR, 0xFFFFFF);
    }

    public float getSalud() {
        return entityData.get(SALUD);
    }

    public void setSalud(float s) {
        entityData.set(SALUD, s);
    }

    public int getColor() {
        return entityData.get(COLOR);
    }

    public void setColor(int c) {
        entityData.set(COLOR, c);
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean causeFallDamage(float distancia, float multiplicador, DamageSource fuente) {
        return false;
    }

    /** Si un jugador agachado lo golpea vacío, lo recoge como item. */
    protected boolean recoger(Player p) {
        return false;
    }

    @Override
    public boolean hurt(DamageSource fuente, float cantidad) {
        if (isInvulnerableTo(fuente) || isRemoved()) return false;
        if (level().isClientSide) return true;
        Entity atacante = fuente.getEntity();
        if (atacante != null && hasPassenger(atacante)) return false;
        if (atacante instanceof Player p && fuente.getDirectEntity() == p) {
            if (p.getAbilities().instabuild) {
                discard();
                return true;
            }
            if (p.isShiftKeyDown() && !isVehicle() && recoger(p)) {
                discard();
                return true;
            }
        }
        setSalud(getSalud() - cantidad);
        if (getSalud() <= 0) destruir();
        return true;
    }

    protected void destruir() {
        level().explode(this, getX(), getY() + 0.5, getZ(), 3.0f, Level.ExplosionInteraction.TNT);
        ejectPassengers();
        discard();
    }

    // ---- interpolación (igual que las barcas) ----
    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int pasos, boolean teletransporte) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpPasos = 3;
    }

    protected void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpPasos = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpPasos > 0) {
            double x = getX() + (lerpX - getX()) / lerpPasos;
            double y = getY() + (lerpY - getY()) / lerpPasos;
            double z = getZ() + (lerpZ - getZ()) / lerpPasos;
            double dy = Mth.wrapDegrees(lerpYRot - getYRot());
            setYRot(getYRot() + (float) dy / lerpPasos);
            setXRot(getXRot() + (float) (lerpXRot - getXRot()) / lerpPasos);
            lerpPasos--;
            setPos(x, y, z);
            setRot(getYRot(), getXRot());
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        if (t.contains("salud")) setSalud(t.getFloat("salud"));
        if (t.contains("color")) setColor(t.getInt("color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        t.putFloat("salud", getSalud());
        t.putInt("color", getColor());
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
