package es.guerramod.items;

import es.guerramod.entidades.BalaEntity;
import es.guerramod.entidades.CoheteEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.RegistryObject;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Supplier;

/** Arma de fuego genérica: pistola, fusil, escopeta, francotirador o lanzacohetes. */
public class ArmaItem extends Item {
    private final float dano, velocidad, dispersion, poder, tono;
    private final int cadencia, proyectiles;
    private final boolean cohete;
    private final Supplier<Item> municion;
    private final RegistryObject<SoundEvent> sonido;

    public ArmaItem(float dano, float velocidad, float dispersion, int cadencia, int proyectiles, float poder,
                    boolean cohete, Supplier<Item> municion, RegistryObject<SoundEvent> sonido, float tono) {
        super(new Item.Properties().stacksTo(1));
        this.dano = dano;
        this.velocidad = velocidad;
        this.dispersion = dispersion;
        this.cadencia = cadencia;
        this.proyectiles = proyectiles;
        this.poder = poder;
        this.cohete = cohete;
        this.municion = municion;
        this.sonido = sonido;
        this.tono = tono;
    }

    public boolean esFrancotirador() {
        return dano >= 20;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player p, InteractionHand mano) {
        ItemStack st = p.getItemInHand(mano);
        if (!level.isClientSide) {
            if (!p.getAbilities().instabuild && !gastarMunicion(p)) {
                p.displayClientMessage(Component.literal("Sin munición: necesitas " + municion.get().getDescription().getString())
                        .withStyle(ChatFormatting.RED), true);
                level.playSound(null, p.getX(), p.getY(), p.getZ(), SoundEvents.DISPENSER_FAIL, SoundSource.PLAYERS, 1.0f, 1.4f);
                p.getCooldowns().addCooldown(this, 10);
                return InteractionResultHolder.fail(st);
            }
            for (int i = 0; i < proyectiles; i++) {
                ThrowableItemProjectile pr = cohete ? new CoheteEntity(level, p) : new BalaEntity(level, p, dano, poder);
                pr.shootFromRotation(p, p.getXRot(), p.getYRot(), 0.0f, velocidad, dispersion);
                level.addFreshEntity(pr);
            }
            level.playSound(null, p.getX(), p.getY(), p.getZ(), sonido.get(), SoundSource.PLAYERS, cohete ? 2.0f : 1.6f,
                    tono * (0.95f + level.random.nextFloat() * 0.1f));
        }
        p.getCooldowns().addCooldown(this, cadencia);
        return InteractionResultHolder.consume(st);
    }

    private boolean gastarMunicion(Player p) {
        Item m = municion.get();
        if (p.getOffhandItem().is(m)) {
            p.getOffhandItem().shrink(1);
            return true;
        }
        for (ItemStack s : p.getInventory().items) {
            if (s.is(m)) {
                s.shrink(1);
                return true;
            }
        }
        return false;
    }

    @Override
    public void appendHoverText(ItemStack st, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        if (cohete) {
            tip.add(Component.literal("Dispara cohetes explosivos").withStyle(ChatFormatting.GRAY));
        } else {
            tip.add(Component.literal("Daño: " + (int) dano + (proyectiles > 1 ? " x" + proyectiles : "")).withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Rompe cristales y bloques blandos").withStyle(ChatFormatting.DARK_GRAY));
        }
        tip.add(Component.literal("Munición: ").append(municion.get().getDescription()).withStyle(ChatFormatting.DARK_GRAY));
        if (esFrancotirador()) tip.add(Component.literal("Agáchate para apuntar con la mira").withStyle(ChatFormatting.DARK_GRAY));
    }
}
