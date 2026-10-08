package com.mistech.mistechmod.artifact;

import java.util.function.Consumer;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.item.mithril.GearRunes;
import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;
import com.mistech.mistechmod.item.mithril.MithrilItem;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Marreta Vulcânica (Adamantina, 3 engastes; Magma + Fogo): minera em área 3×3 (agachado = 1×1) e derrete os drops
 * (receitas de fornalha; ver {@link ArtifactEvents#onBlockDrops}). Agachar + botão direito liga/desliga o derreter.
 * Com Runa da Terra engastada, a área vira 5×5. Cada bloco extra gasta energia como um ponto de desgaste.
 */
public class VolcanicHammerItem extends MithrilItem {
    /** Dureza máxima dos blocos extras em relação ao bloco quebrado. */
    public static final float HARDNESS_SLACK = 1.5F;

    public VolcanicHammerItem(GearTier tier, Properties properties) {
        super(tier, properties);
    }

    public static boolean autosmelt(ItemStack stack) {
        return stack.getOrDefault(ArtifactContent.AUTOSMELT.get(), true);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!player.isSecondaryUseActive()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            boolean on = !autosmelt(stack);
            stack.set(ArtifactContent.AUTOSMELT.get(), on);
            player.sendOverlayMessage(Component.translatable(on ? "item.mistech.volcanic_hammer.smelt_on" : "item.mistech.volcanic_hammer.smelt_off")
                    .withStyle(on ? ChatFormatting.GOLD : ChatFormatting.GRAY));
            level.playSound(null, player.blockPosition(), on ? SoundEvents.FIRECHARGE_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.6F, 1.2F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.mineBlock(stack, level, state, pos, miner);
        if (level instanceof ServerLevel server && miner instanceof Player player && !player.isShiftKeyDown()
                && MithrilGear.isCharged(stack) && state.getDestroySpeed(level, pos) > 0.0F) {
            int radius = GearRunes.count(GearRunes.tally(stack), RuneElement.EARTH) > 0.0 ? 2 : 1;
            breakArea(server, player, stack, pos, state.getDestroySpeed(level, pos), radius);
        }
        return result;
    }

    /** Quebra o plano em volta de {@code center} (perpendicular ao olhar). */
    private static void breakArea(ServerLevel level, Player player, ItemStack stack, BlockPos center, float centerHardness, int radius) {
        Vec3 look = player.getLookAngle();
        Direction.Axis normal = Math.abs(look.y) > 0.7 ? Direction.Axis.Y
                : Math.abs(look.x) > Math.abs(look.z) ? Direction.Axis.X : Direction.Axis.Z;
        for (int a = -radius; a <= radius; a++) {
            for (int b = -radius; b <= radius; b++) {
                if (a == 0 && b == 0) {
                    continue;
                }
                BlockPos pos = switch (normal) {
                    case Y -> center.offset(a, 0, b);
                    case X -> center.offset(0, a, b);
                    case Z -> center.offset(a, b, 0);
                };
                if (!MithrilGear.isCharged(stack)) {
                    return;
                }
                BlockState state = level.getBlockState(pos);
                float hardness = state.getDestroySpeed(level, pos);
                if (state.isAir() || hardness < 0.0F || hardness > centerHardness * HARDNESS_SLACK + 0.5F
                        || level.getBlockEntity(pos) != null || !stack.isCorrectToolForDrops(state)
                        || !level.mayInteract(player, pos)) {
                    continue;
                }
                Block.dropResources(state, level, pos, null, player, stack); // passa pelo BlockDropsEvent (derreter)
                level.destroyBlock(pos, false, player);
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(autosmelt(stack) ? "item.mistech.volcanic_hammer.smelt_on" : "item.mistech.volcanic_hammer.smelt_off")
                .withStyle(autosmelt(stack) ? ChatFormatting.GOLD : ChatFormatting.GRAY));
        builder.accept(Component.translatable("item.mistech.volcanic_hammer.desc").withStyle(ChatFormatting.DARK_GRAY));
        super.appendHoverText(stack, context, display, builder, flag);
    }
}
