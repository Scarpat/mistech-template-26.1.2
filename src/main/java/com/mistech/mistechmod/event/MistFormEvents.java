package com.mistech.mistechmod.event;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.item.MistPipeItem;
import com.mistech.mistechmod.registry.MistechAttachments;
import com.mistech.mistechmod.registry.MistechEffects;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.util.TriState;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Forma de Névoa (efeito mistech:mist_form, dado pelo Cachimbo da Névoa).
 * - O efeito encolhe o jogador (atributo scale ×0,27 → caixa de ~0,16 × 0,49): passa em vãos de 0,5 × 0,5,
 *   mas continua batendo nos blocos (não atravessa parede como o espectador).
 * - Voa enquanto for névoa; ao voltar, ganha Queda Lenta se estiver no ar.
 * - Invisível (o cliente não desenha o corpo), não leva dano (menos o que ignora invencibilidade: vazio, /kill),
 *   mobs não miram nele e ele não interage com nada (quebrar, colocar, bater, abrir, pegar itens).
 * - Se o efeito acabar num lugar apertado demais para o corpo normal, a névoa continua até ter espaço.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class MistFormEvents {
    private MistFormEvents() {}

    public static boolean isMist(Player player) {
        return player.hasEffect(MistechEffects.MIST_FORM);
    }

    /** Cabe o corpo normal aqui? (deitado/rastejando já vale, como o jogo faz ao sair de um vão). */
    public static boolean hasRoomToReform(Player player) {
        AABB box = player.getDefaultDimensions(Pose.SWIMMING).makeBoundingBox(player.position()).deflate(1.0E-7);
        return player.level().noCollision(player, box);
    }

    public static void warnNoRoom(Player player) {
        player.sendOverlayMessage(Component.translatable("message.mistech.mist_form.no_room"));
    }

    /** Mobs que estavam mirando no jogador perdem o alvo. */
    public static void dropAggro(ServerLevel level, Player player) {
        for (Mob mob : level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(32.0), m -> m.getTarget() == player)) {
            mob.setTarget(null);
        }
    }

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        boolean mist = isMist(player);
        if (player.getData(MistechAttachments.MIST_FORM) != mist) {
            player.setData(MistechAttachments.MIST_FORM, mist);
        }

        if (mist) {
            // voo (sem mexer no criativo/espectador, que já voam)
            if (!player.getAbilities().mayfly) {
                player.getAbilities().mayfly = true;
                player.getAbilities().flying = true;
                player.onUpdateAbilities();
                player.setData(MistechAttachments.MIST_FLIGHT, true);
            }
            player.resetFallDistance();

            if (player.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.25, player.getZ(),
                        2, 0.25, 0.2, 0.25, 0.005);
            }

            // sem espaço para voltar: segura a névoa mais um pouco
            MobEffectInstance effect = player.getEffect(MistechEffects.MIST_FORM);
            if (effect != null && effect.getDuration() <= 2 && !effect.isInfiniteDuration() && !hasRoomToReform(player)) {
                player.addEffect(new MobEffectInstance(MistechEffects.MIST_FORM, 20, 0, false, false, true));
                if (player.tickCount % 40 == 0) {
                    warnNoRoom(player);
                }
            }
        } else if (player.getData(MistechAttachments.MIST_FLIGHT)) {
            // acabou a névoa: devolve o corpo
            player.setData(MistechAttachments.MIST_FLIGHT, false);
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
            if (!player.onGround()) {
                player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 60, 0, false, false, true));
            }
            player.resetFallDistance();
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0, player.getZ(), 40, 0.4, 0.8, 0.4, 0.02);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.PLAYER_BREATH, SoundSource.PLAYERS, 1.0F, 0.8F);
            player.getCooldowns().addCooldown(new ItemStack(Mistech.MIST_PIPE.get()), MistPipeItem.COOLDOWN_TICKS);
        }
    }

    // ----- intocável -----

    @SubscribeEvent
    static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof Player player && isMist(player)
                && !event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target instanceof Player player && isMist(player)) {
            event.setCanceled(true);
        }
    }

    // ----- não interage com nada -----

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (isMist(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (isMist(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        // o Cachimbo da Névoa continua funcionando (para voltar mais cedo)
        if (isMist(event.getEntity()) && !event.getItemStack().is(Mistech.MIST_PIPE.get())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (isMist(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    static void onEntityInteractSpecific(PlayerInteractEvent.EntityInteractSpecific event) {
        if (isMist(event.getEntity())) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        if (isMist(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    static void onPickup(ItemEntityPickupEvent.Pre event) {
        if (isMist(event.getPlayer())) {
            event.setCanPickup(TriState.FALSE);
        }
    }
}
