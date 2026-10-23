package com.mistech.mistechmod.herbal;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Pó de Mandrágora (Raiz triturada): "farinha de osso" forte. Usado numa planta, faz crescer de uma vez as plantas
 * numa área 3×3 (até {@link #PASSES} adubadas em cada). Não espalha grama nem flores.
 */
public class MandrakePowderItem extends Item {
    public static final int PASSES = 4;

    public MandrakePowderItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos center = context.getClickedPos();
        if (!(level.getBlockState(center).getBlock() instanceof BonemealableBlock) || level.getBlockState(center).getBlock() instanceof GrassBlock) {
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        int grown = 0;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-1, -1, -1), center.offset(1, 1, 1))) {
            for (int i = 0; i < PASSES; i++) {
                BlockState state = server.getBlockState(pos);
                if (!(state.getBlock() instanceof BonemealableBlock plant) || state.getBlock() instanceof GrassBlock
                        || !plant.isValidBonemealTarget(server, pos, state)) {
                    break;
                }
                plant.performBonemeal(server, server.getRandom(), pos.immutable(), state);
                grown++;
            }
            if (grown > 0) {
                server.sendParticles(ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 3, 0.3, 0.3, 0.3, 0);
            }
        }
        if (grown == 0) {
            return InteractionResult.PASS;
        }
        server.playSound(null, center, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 1.0F, 0.6F);
        server.playSound(null, center, SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.BLOCKS, 0.4F, 0.5F);
        if (context.getPlayer() == null || !context.getPlayer().getAbilities().instabuild) {
            context.getItemInHand().shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(Component.translatable("item.mistech.mandrake_powder.desc").withStyle(ChatFormatting.GRAY));
    }
}
