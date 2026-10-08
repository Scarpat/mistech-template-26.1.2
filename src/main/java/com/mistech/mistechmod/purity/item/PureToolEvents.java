package com.mistech.mistechmod.purity.item;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.Taint;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Ferramentas Puras: não maculam e, ao sol, se consertam sozinhas (1 de durabilidade a cada 5 s por ferramenta no inventário),
 * com a eficiência da faixa de Mácula do dono (Manchada 75%, Maculada 50%, Corrompida ou pior: nada).
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class PureToolEvents {
    public static final int REPAIR_INTERVAL = 5 * 20;

    private PureToolEvents() {}

    /** Força a classe a carregar (registro do @EventBusSubscriber é automático). */
    public static void init() {}

    public static boolean isPureTool(ItemStack stack) {
        return stack.is(PurityContent.PURE_PICKAXE.get()) || stack.is(PurityContent.PURE_AXE.get()) || stack.is(PurityContent.PURE_HOE.get());
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || player.tickCount % REPAIR_INTERVAL != 0) {
            return;
        }
        float efficiency = Taint.stage(player).pureEfficiency();
        if (efficiency <= 0 || player.getRandom().nextFloat() >= efficiency
                || !Taint.inSunlight(player.level(), net.minecraft.core.BlockPos.containing(player.getEyePosition()))) {
            return;
        }
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (isPureTool(stack) && stack.isDamaged()) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
    }
}
