package com.mistech.mistechmod.astrology;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.block.AstralPillarBlock;
import com.mistech.mistechmod.registry.MistechAttachments;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Pilares de Conexão Astral.
 * <ul>
 * <li>Multibloco: Tronco (ou Madeira) Turvo embaixo, Bloco de Lápis-Lazúli no meio, Bloco de Magia Impura em cima.
 * Botão direito com a Pedra Guia no Bloco de Magia Impura → vira o pilar (a pedra não é gasta).</li>
 * <li>Nos 4 blocos de ouro dos pontos cardeais do Campo de Astrologia (2 blocos da mesa, na mesma altura dela),
 * cada pilar vale +12,5% nos bônus. A contagem é guardada no jogador ao usar a mesa.</li>
 * </ul>
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class AstralPillars {
    private AstralPillars() {}

    /** Quantos pilares estão nos pontos cardeais da mesa. */
    public static int count(Level level, BlockPos tablePos) {
        int count = 0;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            BlockPos pos = tablePos.relative(dir, 2);
            BlockState state = level.getBlockState(pos);
            if (state.is(Mistech.ASTRAL_PILLAR.get()) && state.getValue(AstralPillarBlock.PART) == AstralPillarBlock.Part.BOTTOM
                    && level.getBlockState(pos.below()).is(Blocks.GOLD_BLOCK)) {
                count++;
            }
        }
        return count;
    }

    /** Atualiza a Conexão Astral do jogador pela mesa que ele está usando (e reaplica os bônus se mudou). */
    public static void refresh(ServerPlayer player, BlockPos tablePos) {
        int pillars = count(player.level(), tablePos);
        if (player.getData(MistechAttachments.ASTRAL_LINK) != pillars) {
            player.setData(MistechAttachments.ASTRAL_LINK, pillars);
            AstrologyPerks.apply(player);
            if (pillars > 0) {
                player.sendOverlayMessage(Component.translatable("message.mistech.astral_link", pillars,
                        String.format(java.util.Locale.ROOT, "%.1f", pillars * AstrologyPerks.PILLAR_BONUS * 100)));
            }
        }
    }

    /** Pedra Guia no Bloco de Magia Impura do multibloco → Pilar de Conexão Astral. */
    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!event.getItemStack().is(Mistech.GUIDE_STONE.get())) {
            return;
        }
        Level level = event.getLevel();
        BlockPos top = event.getPos();
        if (!level.getBlockState(top).is(Mistech.IMPURE_MAGIC_BLOCK.get())) {
            return;
        }
        // Barril Espacial embaixo = Mineradora (ver MinerStructure), não é pilar
        if (level.getBlockState(top.below()).is(Mistech.SPATIAL_BARREL.get())) {
            return;
        }
        BlockPos middle = top.below();
        BlockPos bottom = middle.below();
        boolean lapis = level.getBlockState(middle).is(Blocks.LAPIS_BLOCK);
        BlockState base = level.getBlockState(bottom);
        boolean murky = base.is(com.mistech.mistechmod.registry.MurkyWood.LOG.get()) || base.is(com.mistech.mistechmod.registry.MurkyWood.WOOD.get());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!lapis || !murky) {
            event.getEntity().sendOverlayMessage(Component.translatable(!lapis ? "message.mistech.astral_pillar_needs_lapis"
                    : "message.mistech.astral_pillar_needs_murky"));
            server.sendParticles(ParticleTypes.SMOKE, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.01);
            return;
        }
        AstralPillarBlock.placePillar(server, bottom, Mistech.ASTRAL_PILLAR.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 3; i++) {
            server.sendParticles(ParticleTypes.END_ROD, bottom.getX() + 0.5, bottom.getY() + 0.5 + i, bottom.getZ() + 0.5, 10, 0.3, 0.4, 0.3, 0.03);
        }
        server.sendParticles(ParticleTypes.WITCH, top.getX() + 0.5, top.getY() + 1.0, top.getZ() + 0.5, 20, 0.2, 0.3, 0.2, 0.05);
        server.playSound(null, top, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.4F);
        server.playSound(null, top, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
    }
}
