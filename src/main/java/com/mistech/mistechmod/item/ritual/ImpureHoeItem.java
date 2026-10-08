package com.mistech.mistechmod.item.ritual;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Enxada Impura: clique em planta madura colhe e replanta; em terra, ara 3×3 (agachado = só 1 bloco). */
public class ImpureHoeItem extends HoeItem {
    /** Raio da área arada (1 = 3×3; a Enxada Infernal usa 2 = 5×5). */
    private final int radius;

    public ImpureHoeItem(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, Properties properties) {
        this(material, attackDamageBaseline, attackSpeedBaseline, properties, 1);
    }

    public ImpureHoeItem(ToolMaterial material, float attackDamageBaseline, float attackSpeedBaseline, Properties properties, int radius) {
        super(material, attackDamageBaseline, attackSpeedBaseline, properties);
        this.radius = radius;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        Player player = context.getPlayer();

        if (state.getBlock() instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) {
                return InteractionResult.PASS;
            }
            if (level instanceof ServerLevel server) {
                List<ItemStack> drops = Block.getDrops(state, server, pos, null, player, context.getItemInHand());
                ItemStack seed = state.getCloneItemStack(server, pos, false);
                boolean replanted = false;
                for (ItemStack drop : drops) {
                    if (!replanted && !seed.isEmpty() && ItemStack.isSameItem(drop, seed)) {
                        drop.shrink(1);
                        replanted = true;
                    }
                    if (!drop.isEmpty()) {
                        Block.popResource(server, pos, drop);
                    }
                }
                server.setBlock(pos, crop.getStateForAge(0), Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.CROP_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
                if (player != null) {
                    context.getItemInHand().hurtAndBreak(1, player, context.getHand().asEquipmentSlot());
                }
            }
            return InteractionResult.SUCCESS;
        }

        InteractionResult center = super.useOn(context);
        if (!center.consumesAction() || player == null || player.isShiftKeyDown() || context.getClickedFace() == Direction.DOWN) {
            return center;
        }
        for (int dx = -this.radius; dx <= this.radius; dx++) {
            for (int dz = -this.radius; dz <= this.radius; dz++) {
                if (dx == 0 && dz == 0) {
                    continue;
                }
                BlockPos other = pos.offset(dx, 0, dz);
                if (!level.getBlockState(other.above()).isAir()) {
                    continue;
                }
                BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(other).add(0, 0.5, 0), Direction.UP, other, false);
                super.useOn(new UseOnContext(level, player, context.getHand(), context.getItemInHand(), hit));
            }
        }
        return center;
    }
}
