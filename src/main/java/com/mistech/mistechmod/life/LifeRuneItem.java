package com.mistech.mistechmod.life;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.golem.AbstractGolem;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runa da Vida (ritual Tier II: Runa do Tempo + Runas de Água e Terra...). 16 usos. Afinidade: Água + Terra.
 * <ul>
 * <li>Botão direito no ar: cura {@link #HEAL} e dá Regeneração I em você e nos aliados feridos a até {@link #RADIUS} blocos
 * (ninguém ferido = não gasta).</li>
 * <li>Em plantação (trigo, cenoura, abóbora, cacau, frutas doces...): cresce <b>até o fim</b> na hora.</li>
 * <li>Em muda: vira árvore na hora. Outras plantas: uma farinha de osso.</li>
 * <li>Em aldeão zumbi: cura na hora. Em outra criatura não hostil: cura e Regeneração.</li>
 * </ul>
 */
public class LifeRuneItem extends Item {
    public static final float HEAL = 6.0F;
    public static final double RADIUS = 4.0;
    public static final int REGEN_TICKS = 100;
    public static final int COOLDOWN = 20;

    public LifeRuneItem(Properties properties) {
        super(properties);
    }

    /** Aliado de {@code player}: outros jogadores, bichos/familiares dele, aldeões e golens (nunca hostis). */
    public static boolean isAlly(Player player, LivingEntity entity) {
        if (entity == player) {
            return true;
        }
        if (!entity.isAlive() || entity instanceof Enemy) {
            return false;
        }
        if (entity instanceof Player) {
            return true;
        }
        if (entity instanceof OwnableEntity owned && owned.getOwner() == player) {
            return true;
        }
        return entity instanceof AbstractVillager || entity instanceof AbstractGolem;
    }

    /** Cura + Regeneração I, com partículas de coração. */
    public static void mend(ServerLevel level, LivingEntity entity, float amount, int regenTicks) {
        entity.heal(amount);
        if (regenTicks > 0) {
            entity.addEffect(new MobEffectInstance(MobEffects.REGENERATION, regenTicks, 0));
        }
        level.sendParticles(ParticleTypes.HEART, entity.getX(), entity.getY() + entity.getBbHeight() + 0.3, entity.getZ(), 2, 0.3, 0.1, 0.3, 0.0);
    }

    /** Planta que a Runa da Vida faz crescer até o fim (a farinha de osso é repetida até não dar mais). */
    public static boolean growsFully(Block block) {
        return block instanceof CropBlock || block instanceof StemBlock || block instanceof SaplingBlock
                || block instanceof SweetBerryBushBlock || block instanceof CocoaBlock;
    }

    /** Faz a planta em {@code pos} crescer (até o fim, se for de {@link #growsFully}). */
    public static boolean grow(Level level, BlockPos pos) {
        BlockState start = level.getBlockState(pos);
        Block block = start.getBlock();
        boolean any = false;
        int tries = growsFully(block) ? 24 : 1;
        for (int i = 0; i < tries; i++) {
            if (!BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), level, pos)) {
                break;
            }
            any = true;
            if (level.getBlockState(pos).getBlock() != block) {
                break; // muda virou árvore
            }
        }
        if (any && !level.isClientSide()) {
            level.levelEvent(LevelEvent.PARTICLES_AND_SOUND_PLANT_GROWTH, pos, 15);
        }
        return any;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        int healed = 0;
        for (LivingEntity entity : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                e -> e.distanceToSqr(player) <= RADIUS * RADIUS && isAlly(player, e) && e.getHealth() < e.getMaxHealth())) {
            mend(server, entity, HEAL, REGEN_TICKS);
            healed++;
        }
        if (healed == 0) {
            return InteractionResult.FAIL;
        }
        lifeFx(server, player.getX(), player.getY() + 1.0, player.getZ(), RADIUS);
        finish(player, stack, hand);
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        if (!grow(level, pos)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            lifeFx(server, pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 0.5);
            if (player != null) {
                finish(player, context.getItemInHand(), context.getHand());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(player.level() instanceof ServerLevel server)) {
            return target instanceof ZombieVillager || isAlly(player, target) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }
        if (target instanceof ZombieVillager zombie) {
            if (zombie.isConverting()) {
                return InteractionResult.PASS;
            }
            // mesma rotina da maçã dourada + Fraqueza, só que a cura é quase instantânea
            zombie.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
            ItemStack keep = player.getItemInHand(hand);
            player.setItemInHand(hand, new ItemStack(Items.GOLDEN_APPLE));
            zombie.mobInteract(player, hand);
            player.setItemInHand(hand, keep);
            if (!zombie.isConverting()) {
                return InteractionResult.PASS;
            }
            zombie.setVillagerConversionTime(20);
            server.playSound(null, zombie.blockPosition(), SoundEvents.ZOMBIE_VILLAGER_CONVERTED, SoundSource.NEUTRAL, 1.0F, 1.2F);
        } else if ((isAlly(player, target) || !(target instanceof Enemy) && target.isAlive()) && target.getHealth() < target.getMaxHealth()) {
            mend(server, target, HEAL, REGEN_TICKS);
        } else {
            return InteractionResult.PASS;
        }
        lifeFx(server, target.getX(), target.getY() + target.getBbHeight() * 0.5, target.getZ(), 0.6);
        finish(player, stack, hand);
        return InteractionResult.SUCCESS;
    }

    private static void finish(Player player, ItemStack stack, InteractionHand hand) {
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        Affinity.grant(player, RuneElement.LIFE, 1);
    }

    public static void lifeFx(ServerLevel level, double x, double y, double z, double spread) {
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 20, spread, 0.4, spread, 0.0);
        level.sendParticles(ParticleTypes.SPORE_BLOSSOM_AIR, x, y + 0.3, z, 12, spread, 0.3, spread, 0.0);
        level.playSound(null, x, y, z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 1.3F);
        level.playSound(null, x, y, z, SoundEvents.BREWING_STAND_BREW, SoundSource.PLAYERS, 0.5F, 1.6F);
    }
}
