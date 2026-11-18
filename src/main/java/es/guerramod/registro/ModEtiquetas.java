package es.guerramod.registro;

import es.guerramod.GuerraMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModEtiquetas {
    /** Bloques que las balas no pueden romper (las bombas sí). */
    public static final TagKey<Block> A_PRUEBA_DE_BALAS =
            TagKey.create(Registries.BLOCK, new ResourceLocation(GuerraMod.MODID, "a_prueba_de_balas"));
    /** Bloques que protegen de la radiación cuando están encima de ti. */
    public static final TagKey<Block> ANTI_RADIACION =
            TagKey.create(Registries.BLOCK, new ResourceLocation(GuerraMod.MODID, "anti_radiacion"));

    private ModEtiquetas() {}
}
