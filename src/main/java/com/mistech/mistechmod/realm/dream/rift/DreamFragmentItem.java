package com.mistech.mistechmod.realm.dream.rift;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.registry.DreamRiftContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Fragmento de Sonho: botão direito num bloco da Superfície abre uma Fenda Onírica em cima dele, que transforma a área em
 * volta numa região do Sonho ({@link DreamRiftBlockEntity}). O comum e o amplificado sorteiam a região; os sintonizados
 * trazem sempre a mesma. Gasta o fragmento.
 */
public class DreamFragmentItem extends Item {
    private final int radius;
    private final @Nullable DreamRegion region;

    public DreamFragmentItem(Item.Properties properties, int radius, @Nullable DreamRegion region) {
        super(properties);
        this.radius = radius;
        this.region = region;
    }

    public int radius() {
        return this.radius;
    }

    public @Nullable DreamRegion region() {
        return this.region;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        if (level.dimension() != Level.OVERWORLD) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.dream_fragment.overworld_only"));
            }
            return InteractionResult.FAIL;
        }
        BlockPos clicked = context.getClickedPos();
        BlockPos pos = level.getBlockState(clicked).canBeReplaced() ? clicked : clicked.above();
        if (!canOpenAt(level, pos) || player != null && !level.mayInteract(player, pos)) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.mistech.dream_fragment.no_space"));
            }
            return InteractionResult.FAIL;
        }
        DreamRegion chosen = this.region != null ? this.region : DreamRegion.random(level.getRandom());
        Direction facing = player != null ? player.getDirection().getOpposite() : Direction.NORTH;
        BlockState lower = DreamRiftContent.DREAM_RIFT.get().defaultBlockState().setValue(DreamRiftBlock.FACING, facing);
        level.setBlock(pos, lower, Block.UPDATE_ALL);
        level.setBlock(pos.above(), lower.setValue(DreamRiftBlock.HALF, DreamRiftBlock.Half.UPPER), Block.UPDATE_ALL);
        if (level.getBlockEntity(pos) instanceof DreamRiftBlockEntity rift) {
            rift.setup(chosen, this.radius, level.getRandom().nextLong(), player != null ? player.getUUID() : null);
        }
        level.playSound(null, pos, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 0.5F, 1.9F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.2F, 0.7F);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 80, 0.3, 0.8, 0.3, 0.15);
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.mistech.dream_fragment.opened",
                    Component.translatable(chosen.translationKey())));
            if (!player.getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
            player.getCooldowns().addCooldown(context.getItemInHand(), 40);
        }
        return InteractionResult.SUCCESS_SERVER;
    }

    private static boolean canOpenAt(ServerLevel level, BlockPos pos) {
        return level.isInWorldBounds(pos.above()) && replaceable(level, pos) && replaceable(level, pos.above())
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static boolean replaceable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.canBeReplaced() && state.getFluidState().isEmpty();
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> tooltip, TooltipFlag flag) {
        tooltip.accept(this.region != null
                ? Component.translatable("tooltip.mistech.dream_fragment.region", Component.translatable(this.region.translationKey()))
                        .withStyle(ChatFormatting.LIGHT_PURPLE)
                : Component.translatable("tooltip.mistech.dream_fragment.random").withStyle(ChatFormatting.LIGHT_PURPLE));
        tooltip.accept(Component.translatable("tooltip.mistech.dream_fragment.radius", this.radius).withStyle(ChatFormatting.GRAY));
        tooltip.accept(Component.translatable("tooltip.mistech.dream_fragment.use").withStyle(ChatFormatting.DARK_GRAY));
    }
}
