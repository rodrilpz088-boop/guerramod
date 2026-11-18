package es.guerramod.registro;

import es.guerramod.GuerraMod;
import es.guerramod.bloques.BombaNuclearBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBloqueEntidades {
    public static final DeferredRegister<BlockEntityType<?>> TIPOS =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, GuerraMod.MODID);

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<BombaNuclearBlockEntity>> BOMBA_NUCLEAR = TIPOS.register("bomba_nuclear",
            () -> BlockEntityType.Builder.of(BombaNuclearBlockEntity::new, ModBloques.BOMBA_NUCLEAR.get()).build(null));

    private ModBloqueEntidades() {}
}
