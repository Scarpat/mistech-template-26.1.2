package com.mistech.mistechmod.item.rune;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Runa de Terra: quebra na hora o bloco clicado, se ele puder ser minerado com picareta de madeira
 * (pedra, pedregulho, carvão, arenito...). Os drops caem normalmente. 16 usos.
 */
public class EarthRuneItem extends Item {
    public EarthRuneItem(Properties properties) {
        super(properties);
    }

    public static boolean canBreak(BlockState state, Level level, BlockPos pos) {
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE)
                && !state.is(BlockTags.INCORRECT_FOR_WOODEN_TOOL)
                && state.getDestroySpeed(level, pos) >= 0.0F; // bedrock e afins têm -1
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Player player = context.getPlayer();
        BlockState state = level.getBlockState(pos);

        if (!canBreak(state, level, pos) || (player != null && !level.mayInteract(player, pos))) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state),
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 20, 0.3, 0.3, 0.3, 0.1);
            level.destroyBlock(pos, true, player);
            if (player != null) {
                context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
                com.mistech.mistechmod.affinity.Affinity.grant(context.getPlayer(), com.mistech.mistechmod.item.RuneElement.EARTH, 1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
