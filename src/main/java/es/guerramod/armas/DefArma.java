package es.guerramod.armas;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;

import java.util.function.Supplier;

/**
 * Datos de un arma de fuego.
 *
 * @param dano          daño por proyectil (corazones x2)
 * @param velocidad     velocidad de salida de la bala (bloques/tick)
 * @param cadencia      ticks mínimos entre disparos
 * @param automatica    si admite fuego automático
 * @param cargador      balas por cargador
 * @param recarga       ticks que tarda la recarga (o por cartucho si porUnidad)
 * @param porUnidad     recarga cartucho a cartucho (escopeta)
 * @param municion      item de munición que gasta
 * @param proyectiles   perdigones por disparo
 * @param dispCadera    dispersión disparando desde la cadera
 * @param dispApuntando dispersión apuntando
 * @param retrocesoV    retroceso vertical (grados)
 * @param retrocesoH    retroceso horizontal máximo (grados)
 * @param zoom          multiplicador de FOV al apuntar
 * @param telescopica   si tiene mira telescópica (pantalla de mira)
 * @param alcance       distancia eficaz (bloques); más allá el daño baja
 * @param sonido        sonido del disparo
 * @param volumen       volumen (16 bloques por punto)
 * @param cerrojo       sonido de cerrojo/corredera tras cada disparo (o null)
 * @param explosiva     dispara cohetes
 * @param miraY         altura de la línea de mira en el modelo (1/16)
 * @param miraZ         posición del ojo (alza) en el modelo
 * @param bocaY         altura de la boca del cañón
 * @param bocaZ         posición de la boca del cañón
 * @param escala        escala del modelo en primera persona
 * @param peso          0..1, cuánto pesa (inercia y velocidad al apuntar)
 */
public record DefArma(String id, float dano, float velocidad, int cadencia, boolean automatica, int cargador, int recarga,
                      boolean porUnidad, Supplier<Item> municion, int proyectiles, float dispCadera, float dispApuntando,
                      float retrocesoV, float retrocesoH, float zoom, boolean telescopica, float alcance,
                      Supplier<SoundEvent> sonido, float volumen, Supplier<SoundEvent> cerrojo, boolean explosiva,
                      float miraY, float miraZ, float bocaY, float bocaZ, float escala, float peso) {
}
