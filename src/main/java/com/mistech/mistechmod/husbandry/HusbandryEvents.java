package com.mistech.mistechmod.husbandry;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Shift + clique com um Pote de Vidro vazio numa galinha/vaca/ovelha/lula adulta: o bicho entra no pote. */
@EventBusSubscriber(modid = Mistech.MODID)
public final class HusbandryEvents {
    private HusbandryEvents() {}

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack held = event.getItemStack();
        Entity target = event.getTarget();
        if (!player.isShiftKeyDown() || !held.is(Mistech.GLASS_JAR_ITEM.get()) || held.has(net.minecraft.core.component.DataComponents.BLOCK_ENTITY_DATA)
                || JarCreatures.kindOf(target.getType()) == null || !(target instanceof Mob mob) || !mob.isAlive()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        if (mob.isBaby()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.creature_jar.baby"));
            return;
        }
        if (mob.isLeashed()) {
            mob.dropLeash();
        }
        mob.ejectPassengers();
        ItemStack jar = new ItemStack(HusbandryContent.CREATURE_JAR_ITEM.get());
        jar.set(MistechComponents.JAR_CREATURE.get(), JarCreatures.capture(mob));
        player.setItemInHand(event.getHand(), ItemUtils.createFilledResult(held, player, jar, false));
        level.sendParticles(ParticleTypes.POOF, mob.getX(), mob.getY() + mob.getBbHeight() * 0.5, mob.getZ(), 15, 0.3, 0.3, 0.3, 0.02);
        level.playSound(null, mob.blockPosition(), SoundEvents.BOTTLE_FILL, SoundSource.PLAYERS, 1.0F, 0.8F);
        mob.discard();
    }
}
