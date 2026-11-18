package es.guerramod.cliente;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class Teclas {
    public static final String CATEGORIA = "key.categories.guerramod";
    public static final KeyMapping MINIMAPA = new KeyMapping("key.guerramod.minimapa", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_M, CATEGORIA);
    public static final KeyMapping DISPARAR = new KeyMapping("key.guerramod.disparar", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_R, CATEGORIA);
    public static final KeyMapping BOMBA = new KeyMapping("key.guerramod.bomba", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_B, CATEGORIA);

    private Teclas() {}
}
