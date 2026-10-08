package com.mistech.mistechmod.accessory;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import com.mistech.mistechmod.Mistech;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

/**
 * Integração opcional com o Curios: se ele estiver instalado, anéis e amuletos do Mistech também valem nos espaços do
 * Curios (anel, colar, amuleto/charm). Feito por reflexão para não precisar do Curios para compilar nem para jogar.
 * <p>
 * Os itens entram nos espaços do Curios pelas tags {@code curios:ring}, {@code curios:necklace} e {@code curios:charm}
 * (data/curios/tags/item) e o jogador ganha esses espaços por {@code data/mistech/curios/entities/mistech_player.json}.
 * Se a API do Curios mudar e a reflexão falhar, a integração desliga sozinha (avisa uma vez no log) e o mod continua
 * só com os espaços próprios (tecla J).
 */
public final class CuriosCompat {
    private static final boolean LOADED = ModList.get() != null && ModList.get().isLoaded("curios");
    private static boolean broken;
    private static Method getInventory;
    private static Method findCurios;
    private static Method slotStack;

    private CuriosCompat() {}

    public static boolean active() {
        return LOADED && !broken;
    }

    /** Acessórios do Mistech equipados nos espaços do Curios (lista vazia sem Curios). */
    public static List<ItemStack> equipped(LivingEntity entity) {
        if (!active()) {
            return List.of();
        }
        try {
            if (getInventory == null) {
                Class<?> api = Class.forName("top.theillusivec4.curios.api.CuriosApi");
                getInventory = api.getMethod("getCuriosInventory", LivingEntity.class);
                Class<?> handler = Class.forName("top.theillusivec4.curios.api.type.capability.ICuriosItemHandler");
                findCurios = handler.getMethod("findCurios", Predicate.class);
                slotStack = Class.forName("top.theillusivec4.curios.api.SlotResult").getMethod("stack");
            }
            Object result = getInventory.invoke(null, entity);
            Object handler = result instanceof Optional<?> optional ? optional.orElse(null) : result;
            if (handler == null) {
                return List.of();
            }
            Predicate<ItemStack> ours = stack -> stack.getItem() instanceof AccessoryItem;
            List<?> found = (List<?>) findCurios.invoke(handler, ours);
            if (found.isEmpty()) {
                return List.of();
            }
            List<ItemStack> stacks = new ArrayList<>(found.size());
            for (Object slot : found) {
                if (slotStack.invoke(slot) instanceof ItemStack stack && !stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            return stacks;
        } catch (Throwable error) {
            broken = true;
            Mistech.LOGGER.warn("Mistech: integração com o Curios desligada (API diferente da esperada): {}", error.toString());
            return List.of();
        }
    }
}
