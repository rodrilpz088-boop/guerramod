package es.guerramod.bloques.hogar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/** Bloques de luz que se pueden encender/apagar con el interruptor y el mando. */
public interface Luminaria {
    IntegerProperty COLOR = IntegerProperty.create("color", 0, 15);

    String[] NOMBRES_COLOR = {"Blanco frío", "Blanco cálido", "Rojo", "Naranja", "Amarillo", "Lima", "Verde", "Turquesa",
            "Cian", "Azul claro", "Azul", "Violeta", "Morado", "Magenta", "Rosa", "Ámbar"};
    int[] RGB = {0xF2F6FF, 0xFFD69A, 0xFF2020, 0xFF7A10, 0xFFE020, 0xA8FF30, 0x20E040, 0x20FFC0,
            0x20E8FF, 0x60B0FF, 0x2050FF, 0x8A40FF, 0xB020FF, 0xFF20D0, 0xFF70B0, 0xFFB020};

    default boolean controlable(BlockState st) {
        return st.hasProperty(BlockStateProperties.LIT);
    }

    /** Enciende/apaga (encender != null) y/o cambia el color (color != null) de todas las luces del radio. Devuelve cuántas. */
    static int controlar(Level level, BlockPos centro, int radio, Boolean encender, Integer color) {
        int n = 0;
        for (BlockPos p : BlockPos.betweenClosed(centro.offset(-radio, -radio, -radio), centro.offset(radio, radio, radio))) {
            BlockState st = level.getBlockState(p);
            if (!(st.getBlock() instanceof Luminaria l) || !l.controlable(st)) continue;
            BlockState nuevo = st;
            if (encender != null) nuevo = nuevo.setValue(BlockStateProperties.LIT, encender);
            if (color != null && nuevo.hasProperty(COLOR)) nuevo = nuevo.setValue(COLOR, color);
            if (nuevo != st) {
                level.setBlock(p, nuevo, Block.UPDATE_ALL);
                n++;
            }
        }
        return n;
    }

    /** Primer estado encendido/apagado que encuentre (para alternar todas igual). */
    static Boolean estado(Level level, BlockPos centro, int radio) {
        for (BlockPos p : BlockPos.betweenClosed(centro.offset(-radio, -radio, -radio), centro.offset(radio, radio, radio))) {
            BlockState st = level.getBlockState(p);
            if (st.getBlock() instanceof Luminaria l && l.controlable(st)) return st.getValue(BlockStateProperties.LIT);
        }
        return null;
    }
}
