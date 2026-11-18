package es.guerramod.registro;

import es.guerramod.GuerraMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Una pestaña del modo creativo por cada apartado. */
public final class ModPestanas {
    public static final DeferredRegister<CreativeModeTab> PESTANAS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, GuerraMod.MODID);

    static {
        for (Categoria c : Categoria.values()) {
            PESTANAS.register(c.id, () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.guerramod." + c.id))
                    .icon(() -> c.lista.isEmpty() ? ItemStack.EMPTY : new ItemStack(c.lista.get(0).get()))
                    .displayItems((params, out) -> {
                        for (Supplier<? extends ItemLike> i : c.lista) out.accept(i.get());
                    })
                    .build());
        }
    }

    private ModPestanas() {}
}
