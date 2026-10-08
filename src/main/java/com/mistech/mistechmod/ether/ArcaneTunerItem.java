package com.mistech.mistechmod.ether;

import java.util.function.Consumer;

import com.mistech.mistechmod.ether.EtherNetworks.Net;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Diapasão Arcano: liga as coisas da Rede Etérea (e, depois, do Arquivo Espacial).
 * <ul>
 * <li>Nexo Etéreo: copia a sintonia.</li>
 * <li>Ponto/Receptor Etéreo: cola a sintonia copiada.</li>
 * <li>Pilar de Transmissão: 1º clique escolhe, 2º clique em outro pilar liga (ou desliga, se já estiverem ligados).</li>
 * <li>Agachar + botão direito no ar: esquece tudo.</li>
 * </ul>
 */
public class ArcaneTunerItem extends Item {
    public ArcaneTunerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        BlockPos pos = context.getClickedPos();
        if (!(level instanceof ServerLevel server) || player == null) {
            return InteractionResult.SUCCESS;
        }
        var be = level.getBlockEntity(pos);
        if (be instanceof EtherealNexusBlockEntity nexus) {
            stack.set(EtherContent.NETWORK.get(), nexus.network());
            Net net = EtherNetworks.get(server.getServer()).get(nexus.network());
            say(player, Component.translatable("message.mistech.tuner.copied", net == null ? "?" : net.name), ChatFormatting.LIGHT_PURPLE);
            ping(server, pos, 1.6F);
            return InteractionResult.SUCCESS;
        }
        if (be instanceof EtherealEndpointBlockEntity endpoint) {
            Integer id = stack.get(EtherContent.NETWORK.get());
            Net net = id == null ? null : EtherNetworks.get(server.getServer()).get(id);
            if (net == null) {
                say(player, Component.translatable("message.mistech.tuner.no_network"), ChatFormatting.RED);
                return InteractionResult.FAIL;
            }
            endpoint.setNetwork(id);
            say(player, Component.translatable("message.mistech.tuner.tuned", net.name), ChatFormatting.AQUA);
            ping(server, pos, 1.2F);
            return InteractionResult.SUCCESS;
        }
        if (be instanceof TransmissionPylonBlockEntity pylon) {
            GlobalPos first = stack.get(EtherContent.TUNER_PYLON.get());
            GlobalPos here = GlobalPos.of(server.dimension(), pos.immutable());
            if (first == null || first.equals(here) || !first.dimension().equals(server.dimension())
                    || !(server.getBlockEntity(first.pos()) instanceof TransmissionPylonBlockEntity other)) {
                stack.set(EtherContent.TUNER_PYLON.get(), here);
                say(player, Component.translatable("message.mistech.tuner.pylon_first"), ChatFormatting.LIGHT_PURPLE);
                ping(server, pos, 1.0F);
                return InteractionResult.SUCCESS;
            }
            stack.remove(EtherContent.TUNER_PYLON.get());
            if (pylon.isLinked(first.pos())) {
                pylon.removeLink(first.pos());
                other.removeLink(pos);
                say(player, Component.translatable("message.mistech.tuner.pylon_unlinked"), ChatFormatting.GRAY);
                ping(server, pos, 0.7F);
                return InteractionResult.SUCCESS;
            }
            double dist = Math.sqrt(first.pos().distSqr(pos));
            if (dist > PylonMesh.LINK_RANGE) {
                say(player, Component.translatable("message.mistech.tuner.pylon_far", (int) dist, (int) PylonMesh.LINK_RANGE), ChatFormatting.RED);
                return InteractionResult.FAIL;
            }
            if (!pylon.canLink() || !other.canLink()) {
                say(player, Component.translatable("message.mistech.tuner.pylon_full", PylonMesh.MAX_LINKS), ChatFormatting.RED);
                return InteractionResult.FAIL;
            }
            pylon.addLink(first.pos());
            other.addLink(pos);
            say(player, Component.translatable("message.mistech.tuner.pylon_linked", (int) dist), ChatFormatting.LIGHT_PURPLE);
            beam(server, first.pos(), pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            stack.remove(EtherContent.NETWORK.get());
            stack.remove(EtherContent.TUNER_PYLON.get());
            say(player, Component.translatable("message.mistech.tuner.cleared"), ChatFormatting.GRAY);
        }
        return InteractionResult.SUCCESS;
    }

    private static void say(Player player, Component text, ChatFormatting color) {
        player.sendOverlayMessage(text.copy().withStyle(color));
    }

    private static void ping(ServerLevel level, BlockPos pos, float pitch) {
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, pitch);
        level.sendParticles(ParticleTypes.ENCHANT, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.3);
    }

    private static void beam(ServerLevel level, BlockPos a, BlockPos b) {
        Vec3 from = Vec3.atCenterOf(a).add(0, 0.4, 0), to = Vec3.atCenterOf(b).add(0, 0.4, 0);
        Vec3 step = to.subtract(from);
        int n = (int) (step.length() * 2);
        for (int i = 0; i <= n; i++) {
            Vec3 p = from.add(step.scale(i / (double) n));
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 1, 0, 0, 0, 0);
        }
        level.playSound(null, b, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.6F);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Integer id = stack.get(EtherContent.NETWORK.get());
        if (id != null) {
            builder.accept(Component.translatable("item.mistech.arcane_tuner.network", id).withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        GlobalPos pylon = stack.get(EtherContent.TUNER_PYLON.get());
        if (pylon != null) {
            builder.accept(Component.translatable("item.mistech.arcane_tuner.pylon", pylon.pos().toShortString()).withStyle(ChatFormatting.AQUA));
        }
        builder.accept(Component.translatable("item.mistech.arcane_tuner.desc").withStyle(ChatFormatting.DARK_GRAY));
    }
}
