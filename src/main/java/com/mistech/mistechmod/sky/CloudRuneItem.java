package com.mistech.mistechmod.sky;

import com.mistech.mistechmod.affinity.Affinity;
import com.mistech.mistechmod.item.RuneElement;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Runa das Nuvens (ritual Tier II: Runa de Vapor + 2 Runas de Ar...). 16 usos.
 * <ul>
 * <li>Botão direito: cria uma nuvem (30 s) onde o jogador mira, até {@link #REACH} blocos. Olhando para baixo no ar: sob os pés.</li>
 * <li>Em <b>lã branca</b>: até 8 blocos de lã ligados viram Nuvem Estável (permanente).</li>
 * </ul>
 * Afinidade: Ar + Vapor (Fogo + Água).
 */
public class CloudRuneItem extends Item {
    public static final double REACH = 5.0;
    public static final int COOLDOWN = 10;

    public CloudRuneItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        boolean done = false;
        HitResult hit = player.pick(REACH, 0.0F, false);
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK
                && server.getBlockState(blockHit.getBlockPos()).is(Blocks.WHITE_WOOL)) {
            done = Clouds.condenseWool(server, blockHit.getBlockPos(), player) > 0;
        } else {
            BlockPos target;
            if (!player.onGround() && player.getXRot() > 60.0F) {
                target = BlockPos.containing(player.getX(), player.getY() - 1.0, player.getZ()); // caindo e olhando para baixo
            } else if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
                target = blockHit.getBlockPos().relative(blockHit.getDirection());
            } else {
                Vec3 end = player.getEyePosition().add(player.getLookAngle().scale(REACH));
                target = BlockPos.containing(end);
            }
            done = Clouds.placeTemporary(server, target, player);
            if (done) {
                server.playSound(null, target, SoundEvents.WOOL_PLACE, SoundSource.PLAYERS, 1.0F, 1.3F);
            }
        }
        if (!done) {
            return InteractionResult.FAIL;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        Affinity.grant(player, RuneElement.CLOUD, 1);
        return InteractionResult.SUCCESS;
    }
}
