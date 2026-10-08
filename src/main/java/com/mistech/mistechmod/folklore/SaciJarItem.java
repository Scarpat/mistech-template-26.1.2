package com.mistech.mistechmod.folklore;

import com.mistech.mistechmod.Mistech;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;

/** Saci Engarrafado: clique num bloco para soltar o Saci (ele vira seu familiar). O pote de vidro volta. */
public class SaciJarItem extends Item {
    public SaciJarItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel level) || player == null) {
            return InteractionResult.SUCCESS;
        }
        if (FolkFamiliar.countOwned(level, FolkloreContent.SACI.get(), player.getUUID()) > 0) {
            player.sendOverlayMessage(Component.translatable("message.mistech.ritual.familiar_exists", Component.translatable("entity.mistech.saci")));
            return InteractionResult.FAIL;
        }
        SaciEntity saci = FolkloreContent.SACI.get().create(level, EntitySpawnReason.MOB_SUMMONED);
        if (saci == null) {
            return InteractionResult.FAIL;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        saci.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180.0F, 0.0F);
        saci.bindTo(player);
        ItemStack stack = context.getItemInHand();
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            saci.setFedTicks(data.copyTag().getIntOr(SaciEntity.FED_TAG, FolkFamiliar.FOOD_TICKS));
        }
        level.addFreshEntity(saci);
        level.sendParticles(ParticleTypes.POOF, saci.getX(), saci.getY() + 0.5, saci.getZ(), 25, 0.4, 0.6, 0.4, 0.05);
        level.playSound(null, pos, SoundEvents.WITCH_CELEBRATE, net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.8F);
        if (!player.hasInfiniteMaterials()) {
            stack.shrink(1);
            player.getInventory().placeItemBackInInventory(new ItemStack(Mistech.GLASS_JAR_ITEM.get()));
        }
        return InteractionResult.SUCCESS;
    }
}
