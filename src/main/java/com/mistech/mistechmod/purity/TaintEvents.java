package com.mistech.mistechmod.purity;

import java.util.List;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.purity.block.SaltBathBlock;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.util.TriState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Regras da Mácula no mundo: fontes e limpezas passivas (1x por segundo), efeitos de cada faixa,
 * ferramentas impuras, Banho de Sal, runas seladas e monstros no Solo Maculado.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public final class TaintEvents {
    public static final float SOIL_PER_SECOND = 0.2F;
    public static final float ITEM_PER_SECOND = 0.02F;
    public static final int ITEM_STACKS_MAX = 5;
    public static final float SUN_PER_SECOND = 0.02F;
    public static final float TOOL_PER_USE = 0.05F;
    public static final double PROVOKE_RADIUS = 20.0;

    private TaintEvents() {}

    @SubscribeEvent
    static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (!(player.level() instanceof ServerLevel level) || player.tickCount % 20 != 0) {
            return;
        }
        // ----- fontes -----
        if (level.getBlockState(player.getOnPos()).is(PurityContent.TAINTED_SOIL.get())) {
            Taint.add(player, SOIL_PER_SECOND);
        }
        int stacks = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(PurityContent.TAINTING)) {
                stacks++;
            }
        }
        if (stacks > 0) {
            Taint.add(player, ITEM_PER_SECOND * Math.min(stacks, ITEM_STACKS_MAX));
        }
        // ----- limpeza do sol -----
        if (Taint.inSunlight(level, BlockPos.containing(player.getEyePosition()))) {
            Taint.cleanse(player, SUN_PER_SECOND);
        }

        // ----- efeitos da faixa -----
        float taint = Taint.get(player);
        Taint.Stage stage = Taint.Stage.of(taint);
        Taint.applyHealth(player);
        if (stage == Taint.Stage.PROFANE) {
            level.sendParticles(ParticleTypes.SQUID_INK, player.getX(), player.getY() + 1.0, player.getZ(), 2, 0.3, 0.5, 0.3, 0.01);
            if (player.getRandom().nextInt(30) == 0) {
                player.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0));
            }
        }
        if (stage.ordinal() >= Taint.Stage.CORRUPTED.ordinal() && player.tickCount % 40 == 0 && !player.isCreative() && !player.isSpectator()) {
            provoke(level, player);
        }

        // ----- cópia para o cliente + aviso de mudança de faixa -----
        int tenths = Math.round(taint * 10.0F);
        int before = player.getData(PurityContent.TAINT_CLIENT);
        if (before != tenths) {
            Taint.Stage old = Taint.Stage.of(before / 10.0F);
            player.setData(PurityContent.TAINT_CLIENT, tenths);
            if (old != stage) {
                boolean worse = stage.ordinal() > old.ordinal();
                player.sendOverlayMessage(Component.translatable(worse ? "message.mistech.taint.worse" : "message.mistech.taint.better",
                        stage.displayName()).withStyle(worse ? ChatFormatting.DARK_PURPLE : ChatFormatting.YELLOW));
                level.playSound(null, player.blockPosition(), worse ? SoundEvents.SCULK_SHRIEKER_SHRIEK : SoundEvents.AMETHYST_BLOCK_CHIME,
                        SoundSource.PLAYERS, worse ? 0.3F : 0.8F, worse ? 1.6F : 1.4F);
            }
        }
    }

    /** Corrompida ou pior: monstros sem alvo por perto que te enxergam vêm atrás de você. */
    private static void provoke(ServerLevel level, Player player) {
        List<Monster> monsters = level.getEntitiesOfClass(Monster.class, new AABB(player.blockPosition()).inflate(PROVOKE_RADIUS),
                m -> m.isAlive() && m.getTarget() == null && m.hasLineOfSight(player));
        for (int i = 0; i < Math.min(3, monsters.size()); i++) {
            monsters.get(i).setTarget(player);
        }
    }

    @SubscribeEvent
    static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        refresh(event.getEntity());
    }

    @SubscribeEvent
    static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        refresh(event.getEntity());
    }

    private static void refresh(Player player) {
        if (!player.level().isClientSide()) {
            Taint.applyHealth(player);
            player.setData(PurityContent.TAINT_CLIENT, Math.round(Taint.get(player) * 10.0F));
        }
    }

    // ----- ferramentas impuras -----

    @SubscribeEvent
    static void onBlockDrops(BlockDropsEvent event) {
        if (event.getBreaker() instanceof Player player && event.getTool().is(PurityContent.TAINTING_TOOLS)) {
            Taint.add(player, TOOL_PER_USE);
        }
    }

    @SubscribeEvent
    static void onAttack(AttackEntityEvent event) {
        Player player = event.getEntity();
        if (!player.level().isClientSide() && player.getMainHandItem().is(PurityContent.TAINTING_TOOLS)) {
            Taint.add(player, TOOL_PER_USE);
        }
    }

    // ----- Banho de Sal + runas seladas -----

    @SubscribeEvent
    static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        ItemStack stack = event.getItemStack();
        Player player = event.getEntity();
        // Sal Grosso num caldeirão cheio de água = Banho de Sal
        if (stack.is(PurityContent.COARSE_SALT.get())) {
            BlockState state = event.getLevel().getBlockState(event.getPos());
            if (state.is(Blocks.WATER_CAULDRON) && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL) {
                if (!event.getLevel().isClientSide()) {
                    event.getLevel().setBlock(event.getPos(), PurityContent.SALT_BATH.get().defaultBlockState().setValue(SaltBathBlock.LEVEL, 3), Block.UPDATE_ALL);
                    if (!player.getAbilities().instabuild) {
                        stack.shrink(1);
                    }
                    event.getLevel().playSound(null, event.getPos(), SoundEvents.SAND_PLACE, SoundSource.BLOCKS, 0.8F, 1.4F);
                }
                event.setCanceled(true);
                event.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
        if (isSealed(stack) && Taint.stage(player).pureEfficiency() <= 0) {
            event.setUseItem(TriState.FALSE);
            if (!event.getLevel().isClientSide()) {
                Taint.pureWorks(player);
            }
        }
    }

    @SubscribeEvent
    static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        if (isSealed(event.getItemStack()) && Taint.stage(event.getEntity()).pureEfficiency() <= 0) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            if (!event.getLevel().isClientSide()) {
                Taint.pureWorks(event.getEntity());
            }
        }
    }

    public static boolean isSealed(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(PurityContent.SEALED.get()));
    }

    @SubscribeEvent
    static void onTooltip(ItemTooltipEvent event) {
        if (isSealed(event.getItemStack())) {
            event.getToolTip().add(1, Component.translatable("tooltip.mistech.sealed").withStyle(ChatFormatting.GOLD));
        }
    }

    // ----- monstros no Solo Maculado -----

    @SubscribeEvent
    static void onSpawnPlacement(MobSpawnEvent.SpawnPlacementCheck event) {
        if (event.getDefaultResult() || event.getEntityType().getCategory() != MobCategory.MONSTER
                || event.getLevel().getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL
                || (event.getSpawnType() != EntitySpawnReason.NATURAL && event.getSpawnType() != EntitySpawnReason.CHUNK_GENERATION)) {
            return;
        }
        BlockPos pos = event.getPos();
        if (event.getLevel().getBlockState(pos.below()).is(PurityContent.TAINTED_SOIL.get())
                && event.getLevel().getMaxLocalRawBrightness(pos) <= 7) {
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.SUCCEED);
        }
    }
}
