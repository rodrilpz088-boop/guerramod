package es.guerramod.metro;

import es.guerramod.registro.ModEntidades;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Vagón de metro autopropulsado. Avanza solo por la vía, para 6 segundos encima de una «señal de parada» (anuncia la
 * estación según el cartel más cercano) y, al llegar al final de la línea, da la vuelta.
 */
public class MetroEntity extends AbstractMinecart {
    private double empujeX, empujeZ;
    private double guardadoX, guardadoZ;
    private int parada, atascado;
    private long ultimaParada = Long.MIN_VALUE;

    public MetroEntity(EntityType<? extends MetroEntity> t, Level l) {
        super(t, l);
    }

    public MetroEntity(Level l, double x, double y, double z) {
        super(ModEntidades.METRO.get(), l, x, y, z);
    }

    public void arrancar(double x, double z) {
        empujeX = x;
        empujeZ = z;
    }

    @Override
    public Type getMinecartType() {
        return Type.RIDEABLE;
    }

    @Override
    protected Item getDropItem() {
        return BloquesMetro.VAGON.get();
    }

    @Override
    public BlockState getDefaultDisplayBlockState() {
        return BloquesMetro.MODELO_VAGON.get().defaultBlockState();
    }

    @Override
    public int getDefaultDisplayOffset() {
        return 4;
    }

    @Override
    public boolean canBeRidden() {
        return true;
    }

    @Override
    public InteractionResult interact(Player p, InteractionHand h) {
        InteractionResult r = super.interact(p, h);
        if (r.consumesAction()) return r;
        if (p.isSecondaryUseActive() || isVehicle()) return InteractionResult.PASS;
        if (!level().isClientSide) return p.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        if (!level().isClientSide) {
            if (parada > 0) {
                parada--;
                setDeltaMovement(Vec3.ZERO);
                if (parada == 20) level().playSound(null, blockPosition(), SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.NEUTRAL, 1f, 1.5f);
                if (parada == 0) {
                    empujeX = guardadoX;
                    empujeZ = guardadoZ;
                    level().playSound(null, blockPosition(), SoundEvents.IRON_DOOR_CLOSE, SoundSource.NEUTRAL, 1f, 0.8f);
                }
            } else if (empujeX * empujeX + empujeZ * empujeZ > 1e-4) {
                BlockPos aqui = blockPosition();
                BlockState abajo = level().getBlockState(aqui.below());
                if (abajo.is(BloquesMetro.SENAL_PARADA.get()) && level().getGameTime() - ultimaParada > 200) {
                    parar();
                }
                // al final de la vía: da la vuelta
                if (getDeltaMovement().horizontalDistanceSqr() < 0.0004) {
                    if (++atascado > 40) {
                        empujeX = -empujeX;
                        empujeZ = -empujeZ;
                        atascado = 0;
                    }
                } else atascado = 0;
            }
        }
        super.tick();
    }

    private void parar() {
        ultimaParada = level().getGameTime();
        guardadoX = empujeX;
        guardadoZ = empujeZ;
        empujeX = empujeZ = 0;
        parada = 120;
        setDeltaMovement(Vec3.ZERO);
        String estacion = "";
        BlockPos c = blockPosition();
        double mejor = Double.MAX_VALUE;
        for (BlockPos q : BlockPos.betweenClosed(c.offset(-8, -2, -8), c.offset(8, 5, 8))) {
            String n = BloquesMetro.estacionDe(level().getBlockState(q).getBlock());
            if (!n.isEmpty() && q.distSqr(c) < mejor) {
                mejor = q.distSqr(c);
                estacion = n;
            }
        }
        level().playSound(null, c, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1.2f, 1.2f);
        level().playSound(null, c, SoundEvents.IRON_DOOR_OPEN, SoundSource.NEUTRAL, 1f, 0.8f);
        Component msg = Component.literal("🚇 " + (estacion.isEmpty() ? "Estación" : "Estación: " + estacion)).withStyle(ChatFormatting.AQUA);
        if (level() instanceof ServerLevel sl) {
            for (ServerPlayer p : sl.players()) if (p.distanceToSqr(this) < 20 * 20) p.displayClientMessage(msg, true);
        }
    }

    @Override
    protected void moveAlongTrack(BlockPos pos, BlockState state) {
        super.moveAlongTrack(pos, state);
        Vec3 v = getDeltaMovement();
        double d2 = v.horizontalDistanceSqr();
        double d3 = empujeX * empujeX + empujeZ * empujeZ;
        if (d3 > 1.0E-4 && d2 > 0.001) {
            double d4 = Math.sqrt(d2), d5 = Math.sqrt(d3);
            empujeX = v.x / d4 * d5;
            empujeZ = v.z / d4 * d5;
        }
    }

    @Override
    protected void applyNaturalSlowdown() {
        double d0 = empujeX * empujeX + empujeZ * empujeZ;
        if (d0 > 1.0E-7) {
            d0 = Math.sqrt(d0);
            empujeX /= d0;
            empujeZ /= d0;
            Vec3 v = getDeltaMovement().multiply(0.85, 0.0, 0.85).add(empujeX * 0.06, 0.0, empujeZ * 0.06);
            setDeltaMovement(v);
        } else {
            setDeltaMovement(getDeltaMovement().multiply(0.9, 0.0, 0.9));
        }
        super.applyNaturalSlowdown();
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag t) {
        super.addAdditionalSaveData(t);
        t.putDouble("EmpujeX", parada > 0 ? guardadoX : empujeX);
        t.putDouble("EmpujeZ", parada > 0 ? guardadoZ : empujeZ);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag t) {
        super.readAdditionalSaveData(t);
        empujeX = t.getDouble("EmpujeX");
        empujeZ = t.getDouble("EmpujeZ");
    }
}
