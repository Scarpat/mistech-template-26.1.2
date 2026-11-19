package com.mistech.mistechmod.endgame;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.machine.MachineBlockEntity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Botão direito numa máquina com o Núcleo Celeste: instala (gasta 1). Os engastes da máquina param de se gastar. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class CelestialCoreEvents {
    private CelestialCoreEvents() {}

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(EndgameContent.CELESTIAL_CORE.get())
                || !(event.getLevel().getBlockEntity(event.getPos()) instanceof MachineBlockEntity machine)) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (machine.hasCelestialCore()) {
            event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.celestial_core.already"));
            return;
        }
        if (!machine.installCelestialCore()) {
            event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.celestial_core.no_sockets"));
            return;
        }
        if (!event.getEntity().getAbilities().instabuild) {
            stack.shrink(1);
        }
        var pos = event.getPos();
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.8F, 1.6F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.05);
        event.getEntity().sendOverlayMessage(Component.translatable("message.mistech.celestial_core.installed"));
    }
}
