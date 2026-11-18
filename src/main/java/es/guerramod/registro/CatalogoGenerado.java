package es.guerramod.registro;

import es.guerramod.bloques.BloquesVarios;
import es.guerramod.bloques.hogar.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

/** GENERADO por herramientas/v2_catalogo.py: no editar a mano. */
public final class CatalogoGenerado {
    private CatalogoGenerado() {}

    public static void registrar() {
        parte0();
        parte1();
        parte2();
        parte3();
        parte4();
    }

    private static void parte0() {
        ModBloques.reg("sofa_gris", Categoria.SALON, () -> new SofaBlock(Fabrica.mueble("tela"), new double[][]{{0, 2, 2, 16, 7, 16}, {0.5, 7, 2, 15.5, 9, 13}, {0, 7, 13, 16, 14, 16}, {0.5, 9, 11.5, 15.5, 13.5, 13}, {1, 0, 3, 2.5, 2, 4.5}, {13.5, 0, 3, 15, 2, 4.5}, {1, 0, 14, 2.5, 2, 15.5}, {13.5, 0, 14, 15, 2, 15.5}}, 0.56));
        ModBloques.reg("sofa_beige", Categoria.SALON, () -> new SofaBlock(Fabrica.mueble("tela"), new double[][]{{0, 2, 2, 16, 7, 16}, {0.5, 7, 2, 15.5, 9, 13}, {0, 7, 13, 16, 14, 16}, {0.5, 9, 11.5, 15.5, 13.5, 13}, {1, 0, 3, 2.5, 2, 4.5}, {13.5, 0, 3, 15, 2, 4.5}, {1, 0, 14, 2.5, 2, 15.5}, {13.5, 0, 14, 15, 2, 15.5}}, 0.56));
        ModBloques.reg("sofa_azul", Categoria.SALON, () -> new SofaBlock(Fabrica.mueble("tela"), new double[][]{{0, 2, 2, 16, 7, 16}, {0.5, 7, 2, 15.5, 9, 13}, {0, 7, 13, 16, 14, 16}, {0.5, 9, 11.5, 15.5, 13.5, 13}, {1, 0, 3, 2.5, 2, 4.5}, {13.5, 0, 3, 15, 2, 4.5}, {1, 0, 14, 2.5, 2, 15.5}, {13.5, 0, 14, 15, 2, 15.5}}, 0.56));
        ModBloques.reg("sillon_gris", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("tela"), new double[][]{{0, 2, 2, 16, 7, 16}, {0.5, 7, 2, 15.5, 9, 13}, {0, 7, 13, 16, 14, 16}, {0.5, 9, 11.5, 15.5, 13.5, 13}, {1, 0, 3, 2.5, 2, 4.5}, {13.5, 0, 3, 15, 2, 4.5}, {1, 0, 14, 2.5, 2, 15.5}, {13.5, 0, 14, 15, 2, 15.5}, {0, 2, 2, 2.5, 11, 16}, {13.5, 2, 2, 16, 11, 16}}, 0.56, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("sillon_cuero", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("tela"), new double[][]{{0, 2, 2, 16, 7, 16}, {0.5, 7, 2, 15.5, 9, 13}, {0, 7, 13, 16, 14, 16}, {0.5, 9, 11.5, 15.5, 13.5, 13}, {1, 0, 3, 2.5, 2, 4.5}, {13.5, 0, 3, 15, 2, 4.5}, {1, 0, 14, 2.5, 2, 15.5}, {13.5, 0, 14, 15, 2, 15.5}, {0, 2, 2, 2.5, 11, 16}, {13.5, 2, 2, 16, 11, 16}}, 0.56, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("mesa_centro", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 6, 2, 16, 7, 14}, {1, 2, 3, 15, 2.6, 13}, {1, 0, 3, 2, 6, 4}, {14, 0, 3, 15, 6, 4}, {1, 0, 12, 2, 6, 13}, {14, 0, 12, 15, 6, 13}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.ALMACENES.add(ModBloques.reg("mueble_tv", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 1, 4, 16, 7, 16}, {0, 7, 3.5, 16, 7.6, 16}, {1, 0, 5, 2, 1, 6}, {14, 0, 5, 15, 1, 6}, {1, 0, 14, 2, 1, 15}, {14, 0, 14, 15, 1, 15}}, -1, 9, MuebleBlock.Accion.NINGUNA)));
        ModBloques.reg("television", Categoria.SALON, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 7), new double[][]{{4, 0, 6.5, 12, 0.5, 11}, {7.3, 0.5, 8.6, 8.7, 3.2, 9.6}, {0, 3, 8.5, 16, 13, 9.4}, {0.3, 3.3, 8.4, 15.7, 12.7, 8.5}}, MuebleEncendibleBlock.Efecto.TELE));
        ModBloques.reg("television_pared", Categoria.SALON, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 7), new double[][]{{0, 3, 14.6, 16, 13, 16}, {0.3, 3.3, 14.5, 15.7, 12.7, 14.6}}, MuebleEncendibleBlock.Efecto.TELE));
        ModBloques.ALMACENES.add(ModBloques.regAlto("estanteria", Categoria.SALON, () -> new MuebleAltoBlock(Fabrica.mueble("madera"), new double[][]{{0, 0, 3, 1, 16, 16}, {15, 0, 3, 16, 16, 16}, {1, 0, 15, 15, 16, 16}, {1, 0, 3, 15, 1, 15}, {1, 8, 3, 15, 9, 15}, {1.5, 1, 5, 14.5, 7, 14.5}, {1.5, 9, 5, 14.5, 15, 14.5}}, new double[][]{{0, 0, 3, 1, 15, 16}, {15, 0, 3, 16, 15, 16}, {1, 0, 15, 15, 15, 16}, {1, 0, 3, 15, 1, 15}, {1, 8, 3, 15, 9, 15}, {1, 14, 3, 15, 15, 15}, {1.5, 1, 5, 14.5, 7, 14.5}, {1.5, 9, 5, 14.5, 15, 14.5}}, 27)));
        ModBloques.reg("alfombra_gris", Categoria.SALON, () -> new CarpetBlock(Fabrica.props("tela").noOcclusion()));
        ModBloques.reg("alfombra_beige", Categoria.SALON, () -> new CarpetBlock(Fabrica.props("tela").noOcclusion()));
        ModBloques.reg("alfombra_roja", Categoria.SALON, () -> new CarpetBlock(Fabrica.props("tela").noOcclusion()));
        ModBloques.reg("planta_interior", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("hojas"), new double[][]{{5, 0, 5, 11, 6, 11}, {5.5, 5.5, 5.5, 10.5, 6, 10.5}, {3, 6, 3, 13, 14, 13}, {5, 13, 5, 11, 17, 11}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("cuadro_abstracto", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{1, 3, 15, 15, 13, 16}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("chimenea", Categoria.SALON, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("piedra"), 13), new double[][]{{0, 0, 8, 16, 2, 16}, {0, 2, 8, 2, 12, 16}, {14, 2, 8, 16, 12, 16}, {0, 12, 7, 16, 16, 16}, {2, 2, 15, 14, 12, 16}, {3, 2, 10, 13, 3.5, 13}, {3, 2, 9.5, 13, 2.4, 14}}, MuebleEncendibleBlock.Efecto.FUEGO));
        ModBloques.reg("aire_acondicionado", Categoria.SALON, () -> new MuebleEncendibleBlock(Fabrica.mueble("plastico"), new double[][]{{0, 10, 12, 16, 15, 16}}, MuebleEncendibleBlock.Efecto.AIRE));
        ModBloques.reg("radiador", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{0, 2, 13.5, 16, 11, 16}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("mesa_comedor", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 14, 0, 16, 15, 16}, {1, 0, 1, 2.5, 14, 2.5}, {13.5, 0, 1, 15, 14, 2.5}, {1, 0, 13.5, 2.5, 14, 15}, {13.5, 0, 13.5, 15, 14, 15}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("silla_comedor", Categoria.SALON, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{2.5, 0, 2.5, 3.7, 8, 3.7}, {12.3, 0, 2.5, 13.5, 8, 3.7}, {2.5, 0, 12.3, 3.7, 8, 13.5}, {12.3, 0, 12.3, 13.5, 8, 13.5}, {2.5, 8, 2.5, 13.5, 9, 13.5}, {3, 9, 3, 13, 9.6, 12.5}, {2.5, 9, 12.3, 3.7, 15, 13.5}, {12.3, 9, 12.3, 13.5, 15, 13.5}, {3.7, 11, 12.6, 12.3, 15, 13.3}}, 0.6, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.ALMACENES.add(ModBloques.reg("encimera", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 1, 1, 16, 14, 16}, {0.5, 0, 1.5, 15.5, 1, 15.5}, {0, 14, 0, 16, 16, 16}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("encimera_roble", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 1, 1, 16, 14, 16}, {0.5, 0, 1.5, 15.5, 1, 15.5}, {0, 14, 0, 16, 16, 16}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.reg("fregadero", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 1, 1, 16, 14, 16}, {0.5, 0, 1.5, 15.5, 1, 15.5}, {0, 14, 0, 16, 16, 16}, {7.5, 16, 13, 8.5, 21, 14}, {7.5, 20, 9.5, 8.5, 21, 13}, {7.6, 19.2, 9.6, 8.4, 20, 10.4}}, -1, 0, MuebleBlock.Accion.GRIFO));
        ModBloques.reg("vitroceramica", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{0, 1, 1, 16, 14, 16}, {0.5, 0, 1.5, 15.5, 1, 15.5}, {0, 14, 0, 16, 16, 16}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.ALMACENES.add(ModBloques.reg("lavavajillas", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{0, 1, 1, 16, 14, 16}, {0.5, 0, 1.5, 15.5, 1, 15.5}, {0, 14, 0, 16, 16, 16}}, -1, 9, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("armario_cocina", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 4, 8, 16, 16, 16}}, -1, 9, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.regAlto("frigorifico", Categoria.COCINA, () -> new MuebleAltoBlock(Fabrica.mueble("metal"), new double[][]{{0, 0, 1, 16, 16, 16}}, new double[][]{{0, 0, 1, 16, 14, 16}}, 54)));
        ModBloques.reg("microondas", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{2, 0, 4, 14, 6, 13}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("campana", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{2, 6, 7, 14, 8, 16}, {5.5, 8, 11.5, 10.5, 16, 16}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.ALMACENES.add(ModBloques.reg("lavadora", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0, 0.5, 1, 16, 14, 16}, {0, 14, 1, 16, 14.6, 16}}, -1, 9, MuebleBlock.Accion.NINGUNA)));
        ModBloques.reg("mesa_cocina", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 14, 0, 16, 15, 16}, {1, 0, 1, 2.5, 14, 2.5}, {13.5, 0, 1, 15, 14, 2.5}, {1, 0, 13.5, 2.5, 14, 15}, {13.5, 0, 13.5, 15, 14, 15}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("taburete", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{3.5, 0, 3.5, 4.3, 11, 4.3}, {11.7, 0, 3.5, 12.5, 11, 4.3}, {3.5, 0, 11.7, 4.3, 11, 12.5}, {11.7, 0, 11.7, 12.5, 11, 12.5}, {3.5, 5, 3.5, 12.5, 5.5, 4.3}, {3, 11, 3, 13, 12.5, 13}}, 0.8, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("cafetera", Categoria.COCINA, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{5, 0, 6, 11, 1, 12}, {5, 1, 10, 11, 7, 12}, {5, 6, 6, 11, 7, 10}, {6.5, 1, 6.5, 9.5, 4, 9.5}, {7.3, 5.2, 7.3, 8.7, 6, 8.7}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("inodoro", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("ceramica"), new double[][]{{5.5, 0, 3, 10.5, 4, 10}, {4, 4, 1.5, 12, 6.5, 11.5}, {4, 6.5, 1.5, 12, 7, 11.5}, {3.5, 4, 11.5, 12.5, 12, 15.5}, {3.3, 12, 11.3, 12.7, 12.6, 15.7}, {7, 12.6, 13, 9, 12.9, 14}}, 0.44, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("bide", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("ceramica"), new double[][]{{5.5, 0, 3, 10.5, 4, 10}, {4, 4, 1.5, 12, 6.5, 11.5}, {7.5, 6.5, 10.5, 8.5, 7.5, 11.5}}, 0.41, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("lavabo", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("ceramica"), new double[][]{{6, 0, 9, 10, 11, 14}, {2, 11, 5, 14, 14, 16}, {7.5, 14, 13.5, 8.5, 16.5, 14.5}, {7.5, 15.5, 11.5, 8.5, 16.5, 13.5}}, -1, 0, MuebleBlock.Accion.GRIFO));
        ModBloques.reg("mueble_lavabo", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{1, 0, 6, 15, 11, 16}, {0.5, 11, 5, 15.5, 13, 16}, {7.5, 13, 14, 8.5, 15.5, 15}, {7.5, 14.5, 12, 8.5, 15.5, 14}}, -1, 0, MuebleBlock.Accion.GRIFO));
        ModBloques.reg("plato_ducha", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("ceramica"), new double[][]{{0, 0, 0, 16, 1, 16}, {0, 1, 0, 0.4, 16, 16}, {0.4, 1, 15.6, 16, 16, 16}, {0, 1, 0, 8, 16, 0.4}, {0, 15.6, 0, 8, 16, 0.6}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("alcachofa_ducha", Categoria.BANO, () -> new MuebleEncendibleBlock(Fabrica.mueble("metal"), new double[][]{{7.5, 12, 11, 8.5, 13, 16}, {5.5, 11, 6.5, 10.5, 12, 11.5}}, MuebleEncendibleBlock.Efecto.AGUA));
        ModBloques.reg("banera", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("ceramica"), new double[][]{{0, 0, 0, 16, 8, 16}, {7.5, 8, 14, 8.5, 10, 15}, {7.5, 9.3, 12.5, 8.5, 10, 14}}, 0.3, 0, MuebleBlock.Accion.NINGUNA));
    }

    private static void parte1() {
        ModBloques.reg("espejo_bano", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("cristal"), new double[][]{{1, 4, 15.4, 15, 15, 16}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("toallero", Categoria.BANO, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{2, 10, 14.6, 14, 10.6, 15.3}, {2, 9.5, 15.3, 3, 11, 16}, {13, 9.5, 15.3, 14, 11, 16}, {3.5, 4, 14.2, 12.5, 10.8, 15.6}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.regCama("cama_blanca", Categoria.DORMITORIO, () -> new CamaBlock(Fabrica.mueble("madera").strength(0.4f)));
        ModBloques.regCama("cama_gris", Categoria.DORMITORIO, () -> new CamaBlock(Fabrica.mueble("madera").strength(0.4f)));
        ModBloques.regCama("cama_azul", Categoria.DORMITORIO, () -> new CamaBlock(Fabrica.mueble("madera").strength(0.4f)));
        ModBloques.ALMACENES.add(ModBloques.regAlto("armario_ropero", Categoria.DORMITORIO, () -> new MuebleAltoBlock(Fabrica.mueble("madera"), new double[][]{{0, 0, 2, 16, 16, 16}}, new double[][]{{0, 0, 2, 16, 15, 16}, {0, 15, 1.5, 16, 16, 16}}, 54)));
        ModBloques.ALMACENES.add(ModBloques.reg("comoda", Categoria.DORMITORIO, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 0, 3, 16, 13, 16}, {0, 13, 2.5, 16, 14, 16}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("mesita_noche", Categoria.DORMITORIO, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{2, 1, 3, 14, 9, 15}, {2, 9, 2.7, 14, 9.6, 15}, {2.5, 0, 3.5, 3.5, 1, 4.5}, {12.5, 0, 3.5, 13.5, 1, 4.5}, {2.5, 0, 13.5, 3.5, 1, 14.5}, {12.5, 0, 13.5, 13.5, 1, 14.5}}, -1, 9, MuebleBlock.Accion.NINGUNA)));
        ModBloques.reg("escritorio", Categoria.DORMITORIO, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{0, 12, 2, 16, 13, 16}, {0.5, 0, 3, 1.5, 12, 15}, {10.5, 0, 3, 15.5, 12, 15}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("silla_oficina", Categoria.DORMITORIO, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{4, 0, 7.5, 12, 1, 8.5}, {7.5, 0, 4, 8.5, 1, 12}, {7.5, 1, 7.5, 8.5, 7, 8.5}, {3, 7, 3, 13, 9, 13}, {3.5, 9, 12, 12.5, 16, 13.5}, {2.5, 9, 6, 3.5, 11, 10}, {12.5, 9, 6, 13.5, 11, 10}}, 0.6, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("ordenador", Categoria.DORMITORIO, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 5), new double[][]{{2, 3, 10, 14, 11, 11}, {7, 0.5, 10.5, 9, 3, 11.5}, {5, 0, 9.5, 11, 0.5, 13}, {3, 0, 3, 13, 0.5, 7}, {13.5, 0, 4, 15.5, 0.6, 7}}, MuebleEncendibleBlock.Efecto.TELE));
        ModBloques.LEDS.add(ModBloques.reg("led_techo", Categoria.ILUMINACION, () -> new LedBlock(Fabrica.luz(Fabrica.mueble("metal"), 15), new double[][]{{2, 15.2, 2, 14, 16, 14}, {2.7, 15, 2.7, 13.3, 15.2, 13.3}})));
        ModBloques.LEDS.add(ModBloques.reg("foco_led", Categoria.ILUMINACION, () -> new LedBlock(Fabrica.luz(Fabrica.mueble("metal"), 14), new double[][]{{5, 15.4, 5, 11, 16, 11}, {6, 15.2, 6, 10, 15.4, 10}})));
        ModBloques.LEDS.add(ModBloques.reg("tira_led", Categoria.ILUMINACION, () -> new TiraLedBlock(Fabrica.luz(Fabrica.mueble("metal"), 12))));
        ModBloques.reg("plafon", Categoria.ILUMINACION, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("cristal"), 15), new double[][]{{3, 14.8, 3, 13, 16, 13}, {4, 14.4, 4, 12, 14.8, 12}}, MuebleEncendibleBlock.Efecto.LUZ));
        ModBloques.reg("lampara_colgante", Categoria.ILUMINACION, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 15), new double[][]{{7.8, 8, 7.8, 8.2, 16, 8.2}, {4, 4, 4, 12, 8, 12}, {6.5, 3.6, 6.5, 9.5, 4, 9.5}}, MuebleEncendibleBlock.Efecto.LUZ));
        ModBloques.reg("aplique_pared", Categoria.ILUMINACION, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 13), new double[][]{{6.5, 6, 15, 9.5, 11, 16}, {5.5, 8, 11, 10.5, 12, 15}}, MuebleEncendibleBlock.Efecto.LUZ));
        ModBloques.reg("lampara_pie", Categoria.ILUMINACION, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 14), new double[][]{{5.5, 0, 5.5, 10.5, 0.6, 10.5}, {7.5, 0.6, 7.5, 8.5, 11, 8.5}, {4.5, 11, 4.5, 11.5, 16, 11.5}}, MuebleEncendibleBlock.Efecto.LUZ));
        ModBloques.reg("lampara_mesa", Categoria.ILUMINACION, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 12), new double[][]{{6, 0, 6, 10, 1, 10}, {7.5, 1, 7.5, 8.5, 6, 8.5}, {5, 6, 5, 11, 10, 11}}, MuebleEncendibleBlock.Efecto.LUZ));
        ModBloques.reg("interruptor", Categoria.ILUMINACION, () -> new InterruptorBlock(Fabrica.mueble("plastico")));
        ModBloques.regAlto("puerta_blanca", Categoria.PUERTAS, () -> new DoorBlock(Fabrica.mueble("madera"), BlockSetType.OAK));
        ModBloques.regAlto("puerta_roble", Categoria.PUERTAS, () -> new DoorBlock(Fabrica.mueble("madera"), BlockSetType.OAK));
        ModBloques.regAlto("puerta_nogal", Categoria.PUERTAS, () -> new DoorBlock(Fabrica.mueble("madera"), BlockSetType.OAK));
        ModBloques.regAlto("puerta_cristal", Categoria.PUERTAS, () -> new DoorBlock(Fabrica.mueble("cristal"), BlockSetType.OAK));
        ModBloques.regAlto("puerta_acorazada", Categoria.PUERTAS, () -> new DoorBlock(Fabrica.mueble("madera").strength(12.0f, 30.0f), BlockSetType.OAK));
        ModBloques.reg("ventana_aluminio", Categoria.PUERTAS, () -> new PanelBlock(Fabrica.mueble("cristal")));
        ModBloques.reg("ventana_pvc", Categoria.PUERTAS, () -> new PanelBlock(Fabrica.mueble("cristal")));
        ModBloques.reg("ventana_madera", Categoria.PUERTAS, () -> new PanelBlock(Fabrica.mueble("cristal")));
        ModBloques.reg("persiana_gris", Categoria.PUERTAS, () -> new PersianaBlock(Fabrica.mueble("metal")));
        ModBloques.reg("persiana_blanca", Categoria.PUERTAS, () -> new PersianaBlock(Fabrica.mueble("metal")));
        ModBloques.reg("cesped_artificial", Categoria.JARDIN, () -> new Block(Fabrica.props("cesped")));
        ModBloques.reg("cesped_artificial_fino", Categoria.JARDIN, () -> new CarpetBlock(Fabrica.props("cesped").noOcclusion()));
        ModBloques.reg("seto", Categoria.JARDIN, () -> new Block(Fabrica.props("hojas")));
        ModBloques.reg("valla_madera_blanca", Categoria.JARDIN, () -> new FenceBlock(Fabrica.props("madera")));
        ModBloques.reg("puerta_valla_madera", Categoria.JARDIN, () -> new FenceGateBlock(Fabrica.mueble("madera"), WoodType.OAK));
        ModBloques.reg("verja_forja", Categoria.JARDIN, () -> new PanelBlock(Fabrica.mueble("metal")));
        ModBloques.reg("puerta_verja_forja", Categoria.JARDIN, () -> new FenceGateBlock(Fabrica.mueble("metal"), WoodType.OAK));
        ModBloques.reg("malla_torsion", Categoria.JARDIN, () -> new PanelBlock(Fabrica.mueble("metal")));
        ModBloques.reg("tumbona", Categoria.JARDIN, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{2, 0, 1, 3, 3, 2}, {13, 0, 1, 14, 3, 2}, {2, 0, 14, 3, 3, 15}, {13, 0, 14, 14, 3, 15}, {2, 3, 1, 14, 4, 11}, {2, 4, 11, 14, 9, 15}}, 0.3, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.regAlto("sombrilla", Categoria.JARDIN, () -> new MuebleAltoBlock(Fabrica.mueble("tela"), new double[][]{{7.5, 0, 7.5, 8.5, 16, 8.5}, {5, 0, 5, 11, 1, 11}}, new double[][]{{7.5, 0, 7.5, 8.5, 12, 8.5}, {-6, 10, -6, 22, 11, 22}, {-2, 11, -2, 18, 12, 18}, {3, 12, 3, 13, 13, 13}}, 0));
    }

    private static void parte2() {
        ModBloques.reg("barbacoa", Categoria.JARDIN, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 12), new double[][]{{2, 0, 3, 3, 8, 4}, {13, 0, 3, 14, 8, 4}, {2, 0, 12, 3, 8, 13}, {13, 0, 12, 14, 8, 13}, {2, 8, 3, 14, 11, 13}, {2.5, 11, 3.5, 13.5, 11.2, 12.5}, {2.5, 11.4, 3.5, 13.5, 11.6, 12.5}}, MuebleEncendibleBlock.Efecto.FUEGO));
        ModBloques.reg("silla_plastico", Categoria.JARDIN, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{2.5, 0, 2.5, 3.7, 8, 3.7}, {12.3, 0, 2.5, 13.5, 8, 3.7}, {2.5, 0, 12.3, 3.7, 8, 13.5}, {12.3, 0, 12.3, 13.5, 8, 13.5}, {2.5, 8, 2.5, 13.5, 9, 13.5}, {3, 9, 3, 13, 9.6, 12.5}, {2.5, 9, 12.3, 3.7, 15, 13.5}, {12.3, 9, 12.3, 13.5, 15, 13.5}, {3.7, 11, 12.6, 12.3, 15, 13.3}}, 0.6, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("mesa_plastico", Categoria.JARDIN, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0, 12, 0, 16, 13, 16}, {1, 0, 1, 2.5, 12, 2.5}, {13.5, 0, 1, 15, 12, 2.5}, {1, 0, 13.5, 2.5, 12, 15}, {13.5, 0, 13.5, 15, 12, 15}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("farolillo_jardin", Categoria.JARDIN, () -> new MuebleEncendibleBlock(Fabrica.luz(Fabrica.mueble("metal"), 11), new double[][]{{6.5, 0, 6.5, 9.5, 6, 9.5}, {5.5, 6, 5.5, 10.5, 10, 10.5}, {5, 10, 5, 11, 11, 11}}, MuebleEncendibleBlock.Efecto.LUZ));
        ModBloques.reg("gresite", Categoria.JARDIN, () -> new Block(Fabrica.props("ceramica")));
        ModBloques.reg("borde_piscina", Categoria.JARDIN, () -> new SlabBlock(Fabrica.props("piedra")));
        ModBloques.reg("gotele_blanco", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("yeso")));
        ModBloques.reg("pared_beige", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("yeso")));
        ModBloques.reg("pared_gris", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("yeso")));
        ModBloques.reg("pared_azul", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("yeso")));
        ModBloques.reg("pared_verde", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("yeso")));
        ModBloques.reg("pared_terracota", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("yeso")));
        ModBloques.reg("azulejo_metro", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("ceramica")));
        ModBloques.reg("azulejo_hidraulico", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("ceramica")));
        ModBloques.reg("gres_gris", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("ceramica")));
        ModBloques.reg("marmol_blanco", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("piedra")));
        ModBloques.reg("parquet_roble", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("madera")));
        ModBloques.reg("parquet_espiga", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("madera")));
        ModBloques.reg("tarima_gris", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("madera")));
        ModBloques.reg("ladrillo_caravista", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("ladrillo")));
        ModBloques.reg("teja_arabe", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("ladrillo")));
        ModBloques.reg("hormigon_pulido", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("hormigon")));
        ModBloques.reg("granito_avila", Categoria.CONSTRUCCION, () -> new Block(Fabrica.props("piedra")));
        ModBloques.reg("gres_gris_losa", Categoria.CONSTRUCCION, () -> new SlabBlock(Fabrica.props("ceramica")));
        ModBloques.reg("marmol_blanco_losa", Categoria.CONSTRUCCION, () -> new SlabBlock(Fabrica.props("piedra")));
        ModBloques.reg("parquet_roble_losa", Categoria.CONSTRUCCION, () -> new SlabBlock(Fabrica.props("madera")));
        ModBloques.reg("granito_avila_losa", Categoria.CONSTRUCCION, () -> new SlabBlock(Fabrica.props("piedra")));
        ModBloques.reg("asfalto_desgastado", Categoria.CARRETERAS, () -> new Block(Fabrica.props("asfalto")));
        ModBloques.reg("asfalto_doble_continua", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_borde", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_borde_discontinua", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_detencion", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_flecha_recto", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_flecha_izquierda", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_flecha_derecha", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_flecha_recto_derecha", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_stop", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_ceda", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_amarilla", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_zigzag", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
    }

    private static void parte3() {
        ModBloques.reg("asfalto_aparcamiento", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_zona_azul", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_bus", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_bici", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("carril_bici", Categoria.CARRETERAS, () -> new Block(Fabrica.props("asfalto")));
        ModBloques.reg("asfalto_cuadricula", Categoria.CARRETERAS, () -> new Block(Fabrica.props("asfalto")));
        ModBloques.reg("asfalto_rejilla", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("asfalto"), false));
        ModBloques.reg("asfalto_tapa", Categoria.CARRETERAS, () -> new Block(Fabrica.props("asfalto")));
        ModBloques.reg("acera", Categoria.CARRETERAS, () -> new Block(Fabrica.props("piedra")));
        ModBloques.reg("acera_podotactil_botones", Categoria.CARRETERAS, () -> new Block(Fabrica.props("piedra")));
        ModBloques.reg("acera_podotactil_rayas", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("piedra"), false));
        ModBloques.reg("bordillo", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueOrientable(Fabrica.props("piedra"), false));
        ModBloques.reg("adoquin_granito", Categoria.CARRETERAS, () -> new Block(Fabrica.props("piedra")));
        ModBloques.reg("poste_senal", Categoria.CARRETERAS, () -> new BloquesVarios.BloqueForma(Fabrica.mueble("metal"), 7, 0, 7, 9, 16, 9));
        ModBloques.reg("senal_stop", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_ceda", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_prohibido_entrar", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_circulacion_prohibida", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_20", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_30", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_40", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_50", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_60", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_80", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_90", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_100", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_velocidad_120", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_prohibido_girar_izquierda", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_prohibido_girar_derecha", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_prohibido_adelantar", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_prohibido_estacionar", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_prohibido_parar", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_sentido_obligatorio_derecha", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_sentido_obligatorio_recto", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_rotonda", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_paso_peatones", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_aparcamiento", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_sentido_unico", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_hospital", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_parada_bus", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
    }

    private static void parte4() {
        ModBloques.reg("senal_calle_residencial", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_autopista", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_rotonda", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_peatones", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_ninos", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_curva_derecha", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_obras", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_semaforo", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("senal_peligro_cruce", Categoria.CARRETERAS, () -> new SenalBlock(Fabrica.mueble("metal")));
        ModBloques.reg("semaforo", Categoria.CARRETERAS, () -> new SemaforoBlock(Fabrica.luzFija(Fabrica.mueble("metal"), 6), false));
        ModBloques.reg("semaforo_peatones", Categoria.CARRETERAS, () -> new SemaforoBlock(Fabrica.luzFija(Fabrica.mueble("metal"), 6), true));
        ModBloques.reg("guardarrail", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{7, 0, 13, 9, 11, 15}, {0, 6, 11, 16, 10, 12.5}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("barrera_new_jersey", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("hormigon"), new double[][]{{0, 0, 3, 16, 4, 13}, {0, 4, 5, 16, 8, 11}, {0, 8, 6, 16, 14, 10}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("cono", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{3, 0, 3, 13, 1, 13}, {5, 1, 5, 11, 4, 11}, {5.75, 4, 5.75, 10.25, 7, 10.25}, {6.5, 7, 6.5, 9.5, 10, 9.5}, {7.25, 10, 7.25, 8.75, 12, 8.75}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("bolardo", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{6.5, 0, 6.5, 9.5, 11, 9.5}, {6.4, 8, 6.4, 9.6, 9, 9.6}, {6, 11, 6, 10, 12, 10}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("hito_kilometrico", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("hormigon"), new double[][]{{4, 0, 6, 12, 14, 10}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("captafaro", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{5, 0, 5, 11, 0.8, 11}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("papelera", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{7.5, 0, 7.5, 8.5, 3, 8.5}, {4.5, 3, 4.5, 11.5, 11, 11.5}, {4.3, 10.5, 4.3, 11.7, 11, 11.7}}, -1, 0, MuebleBlock.Accion.PAPELERA));
        ModBloques.ALMACENES.add(ModBloques.reg("contenedor_amarillo", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0.5, 1.5, 1, 15.5, 15, 15}, {0.2, 15, 0.7, 15.8, 16, 15.3}, {1.5, 13, 0.4, 14.5, 14, 1}, {1.5, 0, 2, 3.5, 1.5, 4}, {12.5, 0, 2, 14.5, 1.5, 4}, {1.5, 0, 12, 3.5, 1.5, 14}, {12.5, 0, 12, 14.5, 1.5, 14}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("contenedor_azul", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0.5, 1.5, 1, 15.5, 15, 15}, {0.2, 15, 0.7, 15.8, 16, 15.3}, {1.5, 13, 0.4, 14.5, 14, 1}, {1.5, 0, 2, 3.5, 1.5, 4}, {12.5, 0, 2, 14.5, 1.5, 4}, {1.5, 0, 12, 3.5, 1.5, 14}, {12.5, 0, 12, 14.5, 1.5, 14}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("contenedor_verde", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0.5, 1.5, 1, 15.5, 15, 15}, {0.2, 15, 0.7, 15.8, 16, 15.3}, {1.5, 13, 0.4, 14.5, 14, 1}, {1.5, 0, 2, 3.5, 1.5, 4}, {12.5, 0, 2, 14.5, 1.5, 4}, {1.5, 0, 12, 3.5, 1.5, 14}, {12.5, 0, 12, 14.5, 1.5, 14}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("contenedor_gris", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0.5, 1.5, 1, 15.5, 15, 15}, {0.2, 15, 0.7, 15.8, 16, 15.3}, {1.5, 13, 0.4, 14.5, 14, 1}, {1.5, 0, 2, 3.5, 1.5, 4}, {12.5, 0, 2, 14.5, 1.5, 4}, {1.5, 0, 12, 3.5, 1.5, 14}, {12.5, 0, 12, 14.5, 1.5, 14}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.ALMACENES.add(ModBloques.reg("contenedor_marron", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("plastico"), new double[][]{{0.5, 1.5, 1, 15.5, 15, 15}, {0.2, 15, 0.7, 15.8, 16, 15.3}, {1.5, 13, 0.4, 14.5, 14, 1}, {1.5, 0, 2, 3.5, 1.5, 4}, {12.5, 0, 2, 14.5, 1.5, 4}, {1.5, 0, 12, 3.5, 1.5, 14}, {12.5, 0, 12, 14.5, 1.5, 14}}, -1, 27, MuebleBlock.Accion.NINGUNA)));
        ModBloques.reg("banco_publico", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("madera"), new double[][]{{1, 0, 4, 2, 7, 12}, {14, 0, 4, 15, 7, 12}, {0, 7, 3, 16, 8, 11}, {0, 8, 11, 16, 15, 12.5}, {1, 7, 11.5, 2, 13, 12.5}, {14, 7, 11.5, 15, 13, 12.5}}, 0.5, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.ALMACENES.add(ModBloques.reg("buzon", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.mueble("metal"), new double[][]{{6, 0, 7, 10, 5, 9}, {3, 5, 4, 13, 16, 12}}, -1, 9, MuebleBlock.Accion.NINGUNA)));
        ModBloques.reg("farola_moderna", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.luzFija(Fabrica.mueble("metal"), 15), new double[][]{{7, 0, 7, 9, 12, 9}, {2, 12, 2, 14, 14, 14}, {3, 11.5, 3, 13, 12, 13}}, -1, 0, MuebleBlock.Accion.NINGUNA));
        ModBloques.reg("farola_fernandina", Categoria.CARRETERAS, () -> new MuebleBlock(Fabrica.luzFija(Fabrica.mueble("metal"), 15), new double[][]{{6, 0, 6, 10, 4, 10}, {5, 4, 5, 11, 5, 11}, {5.3, 5, 5.3, 10.7, 13, 10.7}, {6.5, 5, 6.5, 9.5, 12, 9.5}, {4.5, 13, 4.5, 11.5, 14, 11.5}, {6, 14, 6, 10, 15.5, 10}, {7.5, 15.5, 7.5, 8.5, 16, 8.5}}, -1, 0, MuebleBlock.Accion.NINGUNA));
    }
}
