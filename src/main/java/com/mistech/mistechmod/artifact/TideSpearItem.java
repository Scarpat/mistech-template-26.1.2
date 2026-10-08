package com.mistech.mistechmod.artifact;

import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.mithril.GearTier;
import com.mistech.mistechmod.item.mithril.MithrilGear;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Lança das Marés (Oricalco, 6 engastes; Água + Ar). Um tridente que sempre volta (Lealdade III embutida).
 * Agachado, soltar = correnteza (como Correnteza III) na água, na chuva ou em cima de uma nuvem — sem precisar do encantamento.
 * Energia no lugar da durabilidade; cada correnteza gasta {@link #RIPTIDE_COST} FE.
 */
public class TideSpearItem extends TridentItem implements MithrilGear.Gear {
    public static final int RIPTIDE_COST = 400;
    public static final int LOYALTY = 3;

    public TideSpearItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public GearTier tier() {
        return GearTier.ORICHALCUM;
    }

    /** Está na água, na chuva ou pisando numa nuvem? */
    public static boolean canRiptide(LivingEntity entity) {
        if (entity.isInWaterOrRain()) {
            return true;
        }
        BlockPos below = BlockPos.containing(entity.getX(), entity.getY() - 0.2, entity.getZ());
        return entity.level().getBlockState(below).getBlock() instanceof com.mistech.mistechmod.sky.CloudBlock;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || this.getUseDuration(stack, entity) - timeLeft < 10) {
            return false;
        }
        if (!MithrilGear.isCharged(stack) && !player.hasInfiniteMaterials()) {
            return false;
        }
        if (player.isShiftKeyDown()) {
            if (!canRiptide(player)) {
                if (!level.isClientSide()) {
                    player.sendOverlayMessage(Component.translatable("item.mistech.tide_spear.needs_water").withStyle(ChatFormatting.AQUA));
                }
                return false;
            }
            Vec3 push = player.getLookAngle().scale(3.0);
            player.push(push.x, push.y, push.z);
            player.startAutoSpinAttack(20, 8.0F, stack);
            if (player.onGround()) {
                player.move(MoverType.SELF, new Vec3(0.0, 1.2, 0.0));
            }
            if (!level.isClientSide()) {
                if (!player.hasInfiniteMaterials()) {
                    MithrilGear.setEnergy(stack, MithrilGear.getEnergy(stack) - RIPTIDE_COST);
                }
                level.playSound(null, player, SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
            }
            return true;
        }
        return super.releaseUsing(stack, level, entity, timeLeft); // arremesso (volta pela Lealdade)
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity entity, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, entity, slot);
        if (entity.tickCount % 20 == 0) {
            ensureLoyalty(stack, level);
        }
    }

    /** Lealdade III embutida (colocada no item se faltar). */
    public static void ensureLoyalty(ItemStack stack, Level level) {
        Holder<Enchantment> loyalty = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LOYALTY);
        if (EnchantmentHelper.getItemEnchantmentLevel(loyalty, stack) < LOYALTY) {
            EnchantmentHelper.updateEnchantments(stack, mutable -> mutable.set(loyalty, LOYALTY));
        }
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        return MithrilGear.damageItem(stack, amount, entity);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return MithrilGear.barWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return GearTier.ORICHALCUM.barColor();
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, builder, flag);
        builder.accept(Component.translatable("item.mistech.tide_spear.desc").withStyle(ChatFormatting.DARK_GRAY));
        MithrilGear.tooltip(stack, builder);
    }
}
