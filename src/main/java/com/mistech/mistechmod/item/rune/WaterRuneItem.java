package com.mistech.mistechmod.item.rune;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Runa de Água: coloca uma fonte de água como um balde de água (sem esvaziar). 16 usos. */
public class WaterRuneItem extends Item {
    public WaterRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResult.PASS;
        }
        BlockPos pos = hit.getBlockPos();
        BlockPos next = pos.relative(hit.getDirection());
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(next, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }

        // blocos "alagáveis" (ex.: escada, laje) recebem a água dentro deles, como no balde
        BlockState clicked = level.getBlockState(pos);
        BlockPos placePos = clicked.getBlock() instanceof LiquidBlockContainer container
                && container.canPlaceLiquid(player, level, pos, clicked, Fluids.WATER) ? pos : next;

        // reaproveita a lógica do balde de água (evapora no Nether, alaga blocos, sons...)
        BucketItem waterBucket = (BucketItem) Items.WATER_BUCKET;
        if (waterBucket.emptyContents(player, level, placePos, hit)) {
            stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            com.mistech.mistechmod.affinity.Affinity.grant(player, com.mistech.mistechmod.item.RuneElement.WATER, 1);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.FAIL;
    }
}
