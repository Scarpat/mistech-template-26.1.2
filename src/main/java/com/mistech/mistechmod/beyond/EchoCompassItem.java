package com.mistech.mistechmod.beyond;

import java.util.Optional;
import java.util.function.Consumer;

import com.mojang.datafixers.util.Pair;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.structure.BuiltinStructures;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Bússola do Eco (ritual Tier I). Botão direito: sintoniza na <b>Cidade Antiga</b> mais próxima de onde você está
 * (só no Overworld). Usa o componente vanilla de ímã ({@code lodestone_tracker}) sem precisar de um ímã de verdade.
 */
public class EchoCompassItem extends Item {
    public static final int SEARCH_RADIUS_CHUNKS = 100;
    public static final int COOLDOWN = 100;

    public EchoCompassItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        player.getCooldowns().addCooldown(stack, COOLDOWN);
        if (server.dimension() != Level.OVERWORLD) {
            player.sendOverlayMessage(Component.translatable("item.mistech.echo_compass.wrong_dimension").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        Optional<Holder.Reference<Structure>> city = server.registryAccess().lookupOrThrow(Registries.STRUCTURE).get(BuiltinStructures.ANCIENT_CITY);
        Pair<BlockPos, Holder<Structure>> found = city.isEmpty() ? null : server.getChunkSource().getGenerator()
                .findNearestMapStructure(server, HolderSet.direct(city.get()), player.blockPosition(), SEARCH_RADIUS_CHUNKS, false);
        if (found == null) {
            player.sendOverlayMessage(Component.translatable("item.mistech.echo_compass.none").withStyle(ChatFormatting.GRAY));
            return InteractionResult.FAIL;
        }
        BlockPos target = found.getFirst();
        stack.set(DataComponents.LODESTONE_TRACKER, new LodestoneTracker(Optional.of(GlobalPos.of(server.dimension(), target)), false));
        int distance = (int) Math.sqrt(player.blockPosition().distToCenterSqr(target.getX(), player.getY(), target.getZ()));
        player.sendOverlayMessage(Component.translatable("item.mistech.echo_compass.found", distance).withColor(0x29DFEB));
        server.sendParticles(ParticleTypes.SCULK_SOUL, player.getX(), player.getY() + 1.0, player.getZ(), 12, 0.3, 0.4, 0.3, 0.02);
        server.playSound(null, player.blockPosition(), SoundEvents.SCULK_CLICKING, SoundSource.PLAYERS, 1.0F, 0.8F);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        builder.accept(Component.translatable(tracker == null ? "item.mistech.echo_compass.unset" : "item.mistech.echo_compass.set")
                .withStyle(ChatFormatting.GRAY));
    }
}
