package com.mistech.mistechmod.herbal;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Raiz de Mandrágora: apertar a raiz faz ela gritar. Criaturas a até {@link #RADIUS} blocos ficam atordoadas
 * (Lentidão IV, Fraqueza II e perdem o alvo); jogadores sem capacete levam Náusea (você, que está segurando, não).
 * Gasta a raiz em {@link #BREAK_CHANCE} das vezes; recarga de {@link #COOLDOWN} ticks.
 */
public class MandrakeRootItem extends Item {
    public static final double RADIUS = 8.0;
    public static final int COOLDOWN = 200;
    public static final float BREAK_CHANCE = 0.35F;

    public MandrakeRootItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            scream(server, player);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
            if (!player.getAbilities().instabuild && server.getRandom().nextFloat() < BREAK_CHANCE) {
                stack.shrink(1);
                player.sendOverlayMessage(Component.translatable("message.mistech.mandrake.root_spent"));
            }
        }
        return InteractionResult.SUCCESS;
    }

    public static void scream(ServerLevel level, Player user) {
        level.playSound(null, user.blockPosition(), SoundEvents.GHAST_SCREAM, SoundSource.PLAYERS, 1.0F, 1.8F);
        level.playSound(null, user.blockPosition(), SoundEvents.FOX_SCREECH, SoundSource.PLAYERS, 0.9F, 0.6F);
        level.sendParticles(ParticleTypes.SONIC_BOOM, user.getX(), user.getEyeY() - 0.3, user.getZ(), 1, 0, 0, 0, 0);
        int stunned = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(user.blockPosition()).inflate(RADIUS),
                e -> e != user && e.isAlive() && e.distanceToSqr(user) <= RADIUS * RADIUS)) {
            if (e instanceof ServerPlayer player) {
                if (player.isSpectator() || player.isCreative()) {
                    continue;
                }
                if (!player.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                    player.sendOverlayMessage(Component.translatable("message.mistech.mandrake.muffled"));
                    continue;
                }
                player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 8 * 20, 0));
                player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 4 * 20, 1));
                continue;
            }
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 6 * 20, 3));
            e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 6 * 20, 1));
            if (e instanceof Mob mob) {
                mob.setTarget(null);
                mob.getNavigation().stop();
            }
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, e.getX(), e.getEyeY() + 0.3, e.getZ(), 1, 0.1, 0.1, 0.1, 0);
            stunned++;
        }
        if (stunned > 0) {
            user.sendOverlayMessage(Component.translatable("message.mistech.mandrake.stunned", stunned));
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.mandrake_root.desc").withStyle(ChatFormatting.GRAY));
    }
}
