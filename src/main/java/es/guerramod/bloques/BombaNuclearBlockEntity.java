package es.guerramod.bloques;

import es.guerramod.radiacion.Nuclear;
import es.guerramod.registro.ModBloqueEntidades;
import es.guerramod.registro.ModSonidos;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.UUID;

public class BombaNuclearBlockEntity extends BlockEntity {
    private static final int CUENTA_ATRAS = 200;
    private int cuenta = -1;
    @Nullable
    private UUID dueno;

    public BombaNuclearBlockEntity(BlockPos pos, BlockState state) {
        super(ModBloqueEntidades.BOMBA_NUCLEAR.get(), pos, state);
    }

    /** Devuelve true si se acaba de activar. */
    public boolean activar(@Nullable Player quien) {
        if (cuenta >= 0) return false;
        cuenta = CUENTA_ATRAS;
        if (quien != null) dueno = quien.getUUID();
        setChanged();
        return true;
    }

    public void tickServidor() {
        if (cuenta < 0 || !(level instanceof ServerLevel sl)) return;
        if (cuenta % 20 == 0) {
            sl.playSound(null, worldPosition, ModSonidos.ALARMA.get(), SoundSource.BLOCKS, 6.0f, 1.0f);
            Component aviso = Component.literal("☢ DETONACIÓN NUCLEAR EN " + (cuenta / 20) + " s ☢")
                    .withStyle(ChatFormatting.DARK_RED, ChatFormatting.BOLD);
            for (ServerPlayer p : sl.players()) {
                if (p.blockPosition().distSqr(worldPosition) < 128 * 128) p.displayClientMessage(aviso, true);
            }
        }
        if (--cuenta <= 0) {
            Entity autor = dueno == null ? null : sl.getPlayerByUUID(dueno);
            BlockPos p = worldPosition;
            sl.removeBlock(p, false);
            Nuclear.detonar(sl, Vec3.atCenterOf(p), autor);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("cuenta", cuenta);
        if (dueno != null) tag.putUUID("dueno", dueno);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        cuenta = tag.contains("cuenta") ? tag.getInt("cuenta") : -1;
        dueno = tag.hasUUID("dueno") ? tag.getUUID("dueno") : null;
    }
}
