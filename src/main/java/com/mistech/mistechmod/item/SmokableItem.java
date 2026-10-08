package com.mistech.mistechmod.item;

import javax.annotation.Nullable;

import com.mistech.mistechmod.registry.MistechComponents;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Item "fumável" (Charuto, Baseado).
 * - Acende com isqueiro ou Runa de Fogo na outra mão (igual ao incenso).
 * - Aceso, queima sozinho (1 s por segundo), mesmo no inventário.
 * - Segurar botão direito dá uma tragada: solta uma baforada de fumaça e gasta {@link #puffCost} s extras.
 * - Shift + botão direito apaga (guarda o tempo que falta).
 * Usa os mesmos componentes do incenso: mistech:incense_lit e mistech:incense_burn.
 */
public class SmokableItem extends Item {
    private static final int PUFF_TICKS = 30;

    private final int burnSeconds;
    private final int puffCost;

    public SmokableItem(Item.Properties properties, int burnSeconds, int puffCost) {
        super(properties);
        this.burnSeconds = burnSeconds;
        this.puffCost = puffCost;
    }

    public int getRemainingSeconds(ItemStack stack) {
        return stack.getOrDefault(MistechComponents.INCENSE_BURN.get(), this.burnSeconds);
    }

    public static boolean isLit(ItemStack stack) {
        return IncenseItem.isLit(stack);
    }

    protected void setLit(ItemStack single) {
        single.set(MistechComponents.INCENSE_LIT.get(), true);
        single.set(MistechComponents.INCENSE_BURN.get(), this.getRemainingSeconds(single));
        single.set(DataComponents.MAX_STACK_SIZE, 1);
    }

    /** Dá para acender agora? (o Cachimbo precisa estar carregado). */
    public boolean canLight(ItemStack stack) {
        return true;
    }

    /** Gasta segundos de queima. @return true se acabou (stack esvaziado). */
    protected boolean burn(ItemStack stack, int seconds) {
        int remaining = this.getRemainingSeconds(stack) - seconds;
        if (remaining <= 0) {
            stack.shrink(1);
            return true;
        }
        stack.set(MistechComponents.INCENSE_BURN.get(), remaining);
        return false;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack other = player.getItemInHand(otherHand);

        if (isLit(stack)) {
            if (player.isShiftKeyDown()) {           // apagar
                if (!level.isClientSide()) {
                    stack.remove(MistechComponents.INCENSE_LIT.get());
                    level.playSound(null, player.getX(), player.getY(), player.getZ(),
                            SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.4F, 1.8F);
                }
                return InteractionResult.SUCCESS;
            }
            player.startUsingItem(hand);             // tragar (segurar)
            return InteractionResult.CONSUME;
        }

        if (IncenseItem.isLighter(other) && this.canLight(stack)) {
            this.lightInHand(level, player, hand, other, otherHand);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    /** Acende UM item da mão (o resto da pilha vai para o inventário) gastando 1 uso do isqueiro. */
    public void lightInHand(Level level, Player player, InteractionHand hand, ItemStack lighter, InteractionHand lighterHand) {
        if (level.isClientSide() || !this.canLight(player.getItemInHand(hand))) {
            return;
        }
        // acende o PRÓPRIO stack da mão (trocar por um stack novo fazia o item sumir)
        this.lightInPlace(player, player.getItemInHand(hand));
        EquipmentSlot slot = lighterHand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
        lighter.hurtAndBreak(1, player, slot);
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.FLINTANDSTEEL_USE, SoundSource.PLAYERS, 1.0F, level.getRandom().nextFloat() * 0.4F + 0.8F);
    }

    /** Acende o PRÓPRIO stack; o excedente da pilha vai para o inventário. */
    public void lightInPlace(Player player, ItemStack stack) {
        if (!this.canLight(stack)) {
            return;
        }
        ItemStack rest = stack.getCount() > 1 ? stack.copyWithCount(stack.getCount() - 1) : ItemStack.EMPTY;
        stack.setCount(1);
        this.setLit(stack);
        if (!rest.isEmpty() && !player.getInventory().add(rest)) {
            player.drop(rest, false);
        }
    }

    // ----- tragada -----

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN; // leva o item até a boca
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return isLit(stack) ? PUFF_TICKS : 0;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        // brasa brilhando mais forte enquanto traga
        if (level.isClientSide() && ticksRemaining % 4 == 0) {
            Vec3 tip = mouthPos(entity, 0.45);
            level.addParticle(ParticleTypes.SMALL_FLAME, tip.x, tip.y, tip.z, 0.0, 0.005, 0.0);
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        if (level instanceof ServerLevel serverLevel && isLit(stack)) {
            // baforada: fumaça saindo da boca na direção do olhar
            Vec3 look = entity.getLookAngle();
            Vec3 mouth = mouthPos(entity, 0.3);
            for (int i = 0; i < 10; i++) {
                double speed = 0.03 + i * 0.008;
                serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, mouth.x, mouth.y, mouth.z, 0,
                        look.x * speed, look.y * speed + 0.01, look.z * speed, 1.0);
            }
            serverLevel.sendParticles(ParticleTypes.POOF, mouth.x + look.x * 0.4, mouth.y, mouth.z + look.z * 0.4,
                    4, 0.1, 0.05, 0.1, 0.01);
            level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.15F, 0.6F);
            this.applyPuffEffects(serverLevel, entity);
            if (!(entity instanceof Player player && player.getAbilities().instabuild)) {
                this.burn(stack, this.puffCost);
            }
        }
        return stack;
    }

    /** Efeitos aplicados a cada tragada (sobrescrito pelo Charuto e pelo Baseado). */
    protected void applyPuffEffects(ServerLevel level, LivingEntity entity) {
    }

    private static Vec3 mouthPos(LivingEntity entity, double forward) {
        Vec3 look = entity.getLookAngle();
        return entity.getEyePosition().add(look.x * forward, -0.15, look.z * forward);
    }

    // ----- queima passiva -----

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        if (!isLit(stack) || !(entity instanceof Player player)) {
            return;
        }
        boolean inHand = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
        if (inHand && level.getGameTime() % 10 == 0) {
            Vec3 tip = mouthPos(player, 0.5);
            level.sendParticles(ParticleTypes.SMOKE, tip.x, tip.y + 0.05, tip.z, 1, 0.02, 0.02, 0.02, 0.003);
        }
        if (level.getGameTime() % 20 != 0 || player.getAbilities().instabuild) {
            return;
        }
        if (this.burn(stack, 1)) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.3F, 1.8F);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(MistechComponents.INCENSE_BURN.get());
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round(13.0F * this.getRemainingSeconds(stack) / this.burnSeconds), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xE8913A;
    }

    @Override
    public Component getName(ItemStack stack) {
        return isLit(stack) ? Component.translatable(this.getDescriptionId() + ".lit") : super.getName(stack);
    }
}
