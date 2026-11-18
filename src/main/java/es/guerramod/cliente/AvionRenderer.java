package es.guerramod.cliente;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import es.guerramod.GuerraMod;
import es.guerramod.entidades.AeronaveBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Dibuja aviones y bombarderos con el color del país que los envía. */
public class AvionRenderer<T extends AeronaveBase> extends EntityRenderer<T> {
    public static final ResourceLocation TEXTURA_CAZA = new ResourceLocation(GuerraMod.MODID, "textures/entity/avion_caza.png");
    public static final ResourceLocation TEXTURA_BOMBARDERO = new ResourceLocation(GuerraMod.MODID, "textures/entity/bombardero.png");

    private final AvionModel<T> modelo;
    private final float escala;
    private final ResourceLocation textura;

    public AvionRenderer(EntityRendererProvider.Context ctx, float escala, ResourceLocation textura) {
        super(ctx);
        this.modelo = new AvionModel<>(ctx.bakeLayer(AvionModel.CAPA));
        this.escala = escala;
        this.textura = textura;
        this.shadowRadius = 1.2f * escala;
    }

    @Override
    public void render(T e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int luz) {
        ps.pushPose();
        ps.translate(0.0, 0.45 * escala, 0.0);
        float giro = Mth.rotLerp(pt, e.yRotO, e.getYRot());
        float cabeceo = Mth.lerp(pt, e.xRotO, e.getXRot());
        ps.mulPose(Axis.YP.rotationDegrees(180.0f - giro));
        ps.mulPose(Axis.XP.rotationDegrees(-cabeceo));
        if (e instanceof es.guerramod.entidades.AvionEntity av) {
            ps.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(pt, av.alabeoO, av.alabeo)));
        }
        ps.scale(-escala, -escala, escala);
        modelo.setupAnim(e, 0, 0, e.tickCount + pt, 0, 0);
        int c = e.getColor();
        float r = 0.55f + 0.45f * ((c >> 16) & 0xFF) / 255f;
        float g = 0.55f + 0.45f * ((c >> 8) & 0xFF) / 255f;
        float b = 0.55f + 0.45f * (c & 0xFF) / 255f;
        modelo.renderToBuffer(ps, buf.getBuffer(modelo.renderType(getTextureLocation(e))), luz, OverlayTexture.NO_OVERLAY, r, g, b, 1.0f);
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, luz);
    }

    @Override
    public ResourceLocation getTextureLocation(T e) {
        return textura;
    }
}
