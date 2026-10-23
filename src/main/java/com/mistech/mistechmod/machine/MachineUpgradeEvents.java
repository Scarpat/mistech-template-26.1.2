package com.mistech.mistechmod.machine;

import com.mistech.mistechmod.Mistech;

import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Botão direito numa máquina com uma Melhoria Rúnica: instala (gasta 1). */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MachineUpgradeEvents {
    private MachineUpgradeEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof MachineUpgradeItem upgrade)) {
            return;
        }
        // peça de uma Forja Trituradora montada: a melhoria vai para o núcleo
        net.minecraft.core.BlockPos target = event.getPos();
        var state = event.getLevel().getBlockState(target);
        if (state.hasProperty(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED)
                && state.getValue(com.mistech.mistechmod.machine.forge.CrusherForgeStructure.FORMED)
                && !state.is(com.mistech.mistechmod.machine.forge.ForgeContent.CRUSHER_FORGE.get())) {
            net.minecraft.core.BlockPos core = com.mistech.mistechmod.machine.forge.CrusherForgeStructure.findCore(event.getLevel(), target);
            if (core != null) {
                target = core;
            }
        }
        if (!(event.getLevel().getBlockEntity(target) instanceof MachineBlockEntity machine)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getLevel().isClientSide()) {
            return;
        }
        MachineUpgrade type = upgrade.type();
        if (!machine.acceptsUpgrade(type)) {
            event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.upgrade.not_accepted"));
            return;
        }
        if (!machine.installUpgrade(type)) {
            event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.upgrade.full", type.max()));
            return;
        }
        if (!event.getEntity().getAbilities().instabuild) {
            stack.shrink(1);
        }
        event.getLevel().playSound(null, target, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 1.4F);
        event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.upgrade.installed",
                Component.translatable("item.mistech." + type.id() + "_upgrade"), machine.upgrade(type), type.max()));
    }
}
