package es.guerramod.registro;

import es.guerramod.GuerraMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModPestanas {
    public static final DeferredRegister<CreativeModeTab> PESTANAS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GuerraMod.MODID);

    public static final RegistryObject<CreativeModeTab> GUERRA = PESTANAS.register("guerra", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.guerramod.guerra"))
            .icon(() -> new ItemStack(ModItems.FUSIL.get()))
            .displayItems((params, out) -> {
                for (RegistryObject<? extends net.minecraft.world.item.Item> i : ModItems.LISTA_GUERRA) out.accept(i.get());
                for (RegistryObject<? extends Block> b : ModBloques.GUERRA) out.accept(b.get());
            })
            .build());

    public static final RegistryObject<CreativeModeTab> NACIONES = PESTANAS.register("naciones", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.guerramod.naciones"))
            .icon(() -> new ItemStack(ModBloques.BANDERAS.get(0).get()))
            .displayItems((params, out) -> {
                for (RegistryObject<? extends Block> b : ModBloques.FRONTERAS) out.accept(b.get());
                for (RegistryObject<? extends Block> b : ModBloques.GOBIERNO) out.accept(b.get());
            })
            .build());

    private ModPestanas() {}
}
