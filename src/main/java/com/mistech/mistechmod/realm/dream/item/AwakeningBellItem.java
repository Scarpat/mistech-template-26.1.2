package com.mistech.mistechmod.realm.dream.item;

import com.mistech.mistechmod.realm.DreamLayout;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.realm.dream.mob.TheDreamerEntity;
import com.mistech.mistechmod.registry.DreamDeepContent;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Sino do Despertar: tocado no Sono REM (ou na Borda), acorda O Sonhador na sua frente. Gasta o sino. Um Sonhador por vez por perto. */
public class AwakeningBellItem extends Item {
    public AwakeningBellItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!GnomeRealm.isRealm(level) || DreamLayout.ring(player.getX(), player.getZ()) < DreamLayout.RING_REM) {
            player.sendOverlayMessage(Component.translatable("message.mistech.awakening_bell.wrong_place"));
            server.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 0.6F, 2.0F);
            return InteractionResult.FAIL;
        }
        if (!server.getEntitiesOfClass(TheDreamerEntity.class, new AABB(player.blockPosition()).inflate(96.0)).isEmpty()) {
            player.sendOverlayMessage(Component.translatable("message.mistech.awakening_bell.already"));
            return InteractionResult.FAIL;
        }
        TheDreamerEntity boss = DreamDeepContent.THE_DREAMER.get().create(server, EntitySpawnReason.TRIGGERED);
        if (boss == null) {
            return InteractionResult.FAIL;
        }
        Vec3 at = player.position().add(player.getLookAngle().multiply(1, 0, 1).normalize().scale(10.0)).add(0, 4.0, 0);
        boss.snapTo(at.x, at.y, at.z, player.getYRot() + 180.0F, 0.0F);
        boss.setTarget(player);
        server.addFreshEntity(boss);
        server.playSound(null, player.blockPosition(), SoundEvents.BELL_BLOCK, SoundSource.PLAYERS, 2.0F, 0.5F);
        server.playSound(null, player.blockPosition(), SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 2.0F, 0.6F);
        server.sendParticles(ParticleTypes.END_ROD, at.x, at.y + 2, at.z, 60, 2, 2, 2, 0.05);
        player.sendSystemMessage(Component.translatable("message.mistech.the_dreamer.summoned"));
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
