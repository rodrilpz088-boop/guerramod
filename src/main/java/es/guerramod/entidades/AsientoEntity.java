package es.guerramod.entidades;

import es.guerramod.registro.ModEntidades;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

/** Entidad invisible sobre la que se sienta el jugador en sillas, sofás, inodoros... */
public class AsientoEntity extends Entity {
    private BlockPos bloque = BlockPos.ZERO;

    public AsientoEntity(EntityType<? extends AsientoEntity> tipo, Level level) {
        super(tipo, level);
        this.noPhysics = true;
    }

    /** Sienta al jugador en el bloque a la altura indicada (en bloques, la parte de arriba del cojín). */
    public static boolean sentar(Level level, BlockPos pos, double altura, Player p) {
        if (p.isPassenger()) return false;
        if (!level.getEntitiesOfClass(AsientoEntity.class, new AABB(pos)).isEmpty()) return false;
        AsientoEntity a = new AsientoEntity(ModEntidades.ASIENTO.get(), level);
        a.bloque = pos.immutable();
        a.setPos(pos.getX() + 0.5, pos.getY() + altura - 0.35, pos.getZ() + 0.5);
        level.addFreshEntity(a);
        return p.startRiding(a);
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide) return;
        if (getPassengers().isEmpty() || level().getBlockState(bloque).isAir()) discard();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.0;
    }

    @Override
    public Vec3 getDismountLocationForPassenger(LivingEntity quien) {
        Vec3 v = super.getDismountLocationForPassenger(quien);
        return v.y < bloque.getY() + 0.9 ? new Vec3(bloque.getX() + 0.5, bloque.getY() + 1.0, bloque.getZ() + 0.5) : v;
    }

    @Override
    public boolean shouldBeSaved() {
        return false;
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
