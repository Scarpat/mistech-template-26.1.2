package com.mistech.mistechmod.lore;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Machado do Mapinguari: cortar um tronco (sem agachar) derruba a árvore inteira — todos os troncos ligados, até
 * {@link #MAX_LOGS} (dá conta das Árvores Ancestrais). Gasta 1 de durabilidade a cada 3 troncos.
 */
public class MapinguariAxeItem extends AxeItem {
    public static final int MAX_LOGS = 600;

    public MapinguariAxeItem(ToolMaterial material, float damage, float speed, Item.Properties properties) {
        super(material, damage, speed, properties);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (level instanceof ServerLevel server && miner instanceof Player player && !player.isShiftKeyDown() && state.is(BlockTags.LOGS)) {
            this.fell(server, player, stack, pos);
        }
        return result;
    }

    private void fell(ServerLevel level, Player player, ItemStack stack, BlockPos start) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> seen = new HashSet<>();
        queue.add(start);
        seen.add(start);
        int cut = 0;
        while (!queue.isEmpty() && cut < MAX_LOGS && !stack.isEmpty()) {
            BlockPos pos = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos next = pos.offset(dx, dy, dz);
                        if (seen.contains(next) || next.distSqr(start) > 40 * 40) {
                            continue;
                        }
                        seen.add(next);
                        if (level.getBlockState(next).is(BlockTags.LOGS)) {
                            queue.add(next);
                            level.destroyBlock(next, true, player);
                            if (++cut % 3 == 0) {
                                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                            }
                        }
                    }
                }
            }
            // raízes (sapopemas) também descem
            BlockPos below = pos.below();
            if (!seen.contains(below) && below.getY() >= start.getY() - 3 && level.getBlockState(below).is(BlockTags.LOGS)) {
                seen.add(below);
                queue.add(below);
                level.destroyBlock(below, true, player);
                cut++;
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.mistech.mapinguari_axe.tip").withStyle(ChatFormatting.GOLD));
        builder.accept(Component.translatable("item.mistech.mapinguari_axe.sneak").withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
