package com.mistech.mistechmod.beyond;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Âncora de Portal (crafting: Runa do Espaço, Obsidiana Chorona e Magnetita). Colocada no Mundo Superior; botão direito vincula
 * você a ela. A partir daí, <b>todo portal do Nether que te trouxer de volta te deixa na âncora</b> (em vez de no portal ligado).
 * Agachado: desfaz o vínculo. Se a âncora sumir, o vínculo some junto.
 */
public class PortalAnchorBlock extends Block {
    public PortalAnchorBlock(Properties properties) {
        super(properties);
    }

    public static String format(BlockPos pos) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ();
    }

    public static @Nullable BlockPos parse(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        String[] parts = text.split(",");
        if (parts.length != 3) {
            return null;
        }
        try {
            return new BlockPos(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        if (server.dimension() != Level.OVERWORLD) {
            player.sendOverlayMessage(Component.translatable("message.mistech.portal_anchor.overworld_only"));
            return InteractionResult.FAIL;
        }
        if (player.isShiftKeyDown()) {
            player.setData(PaleNetherContent.PORTAL_ANCHOR, "");
            player.sendOverlayMessage(Component.translatable("message.mistech.portal_anchor.unbound"));
            server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_DEPLETE.value(), SoundSource.BLOCKS, 0.8F, 1.2F);
            return InteractionResult.SUCCESS;
        }
        player.setData(PaleNetherContent.PORTAL_ANCHOR, format(pos));
        player.sendOverlayMessage(Component.translatable("message.mistech.portal_anchor.bound").withColor(0xB05CFF));
        server.playSound(null, pos, SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.BLOCKS, 1.0F, 1.0F);
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.3, 0.4, 0.3, 0.05);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0) {
            level.addParticle(ParticleTypes.PORTAL, pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.3, 0.0);
        }
    }
}
