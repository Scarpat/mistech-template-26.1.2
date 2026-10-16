package com.mistech.mistechmod.realm.dream.item;

import java.util.Optional;
import java.util.function.Consumer;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.realm.GnomeRealm;
import com.mistech.mistechmod.realm.dream.mob.CloudWhaleEntity;
import com.mistech.mistechmod.realm.dream.mob.TheDreamerEntity;
import com.mistech.mistechmod.registry.DreamDeepContent;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.TypedEntityData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * Apanha-Sonho Espacial: dentro do Sonho, usar numa criatura a guarda (até {@link #CAPACITY}). Usar num bloco (no Sonho ou
 * na realidade) solta a última guardada; agachado solta todas. Serve para levar a Baleia-Nuvem de um lugar a outro. Cada soltura gasta 1 de durabilidade. Chefes não cabem.
 */
@EventBusSubscriber(modid = Mistech.MODID)
public class SpatialDreamcatcherItem extends Item {
    public static final int CAPACITY = 3;
    private static final String MOBS = "Mobs";
    /** Partes do NBT que não fazem sentido guardadas (posição, laços, quem estava montado...). */
    private static final String[] STRIP = {"Pos", "Motion", "Rotation", "UUID", "FallDistance", "fall_distance", "Fire", "Air", "OnGround",
            "PortalCooldown", "leash", "Leash", "Passengers", "Home"};

    public SpatialDreamcatcherItem(Item.Properties properties) {
        super(properties);
    }

    public static ListTag stored(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getListOrEmpty(MOBS);
    }

    private static void setStored(ItemStack stack, ListTag list) {
        CustomData.update(DataComponents.CUSTOM_DATA, stack, tag -> {
            if (list.isEmpty()) {
                tag.remove(MOBS);
            } else {
                tag.put(MOBS, list);
            }
        });
    }

    // ----- guardar (evento: roda antes do mobInteract, então funciona até em gnomos mercadores e baleias)

    @SubscribeEvent
    static void onInteract(PlayerInteractEvent.EntityInteract event) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(DreamDeepContent.SPATIAL_DREAMCATCHER.get()) || !(event.getTarget() instanceof Mob mob) || !mob.isAlive()) {
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        Player player = event.getEntity();
        Level level = event.getLevel();
        if (level.isClientSide()) {
            return;
        }
        if (!GnomeRealm.isRealm(level)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_dreamcatcher.only_dreams"));
            return;
        }
        if (mob instanceof TheDreamerEntity || mob.typeHolder().is(Tags.EntityTypes.BOSSES)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_dreamcatcher.too_big"));
            return;
        }
        ListTag list = stored(stack);
        if (list.size() >= CAPACITY) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_dreamcatcher.full"));
            return;
        }
        if (mob.hasPassenger(e -> e instanceof Player)) {
            player.sendOverlayMessage(Component.translatable("message.mistech.spatial_dreamcatcher.ridden"));
            return;
        }
        mob.ejectPassengers();
        mob.stopRiding();
        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, mob.registryAccess());
        mob.saveWithoutId(out);
        CompoundTag data = out.buildResult();
        for (String key : STRIP) {
            data.remove(key);
        }
        CompoundTag entry = new CompoundTag();
        entry.putString("Type", EntityType.getKey(mob.getType()).toString());
        entry.putString("Name", mob.getName().getString());
        entry.put("Data", data);
        list.add(entry);
        setStored(stack, list);
        ServerLevel server = (ServerLevel) level;
        server.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + mob.getBbHeight() / 2, mob.getZ(), 40,
                mob.getBbWidth() / 2, mob.getBbHeight() / 2, mob.getBbWidth() / 2, 0.05);
        server.playSound(null, mob.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.4F);
        mob.discard();
        player.sendOverlayMessage(Component.translatable("message.mistech.spatial_dreamcatcher.caught", entry.getStringOr("Name", "?"),
                list.size(), CAPACITY));
        player.getCooldowns().addCooldown(stack, 10);
    }

    // ----- soltar

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        ListTag list = stored(stack);
        if (list.isEmpty()) {
            if (player != null && !level.isClientSide()) {
                player.sendOverlayMessage(Component.translatable("message.mistech.spatial_dreamcatcher.empty"));
            }
            return InteractionResult.PASS;
        }
        if (!(level instanceof ServerLevel server)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos at = context.getClickedPos().relative(context.getClickedFace());
        boolean all = player != null && player.isSecondaryUseActive();
        int released = 0;
        while (!list.isEmpty() && (all || released == 0)) {
            CompoundTag entry = list.getCompoundOrEmpty(list.size() - 1);
            list.remove(list.size() - 1);
            Optional<EntityType<?>> type = EntityType.byString(entry.getStringOr("Type", ""));
            if (type.isEmpty()) {
                continue;
            }
            Entity entity = type.get().create(server, EntitySpawnReason.MOB_SUMMONED);
            if (entity == null) {
                continue;
            }
            TypedEntityData.of(type.get(), entry.getCompoundOrEmpty("Data")).loadInto(entity);
            double ox = released == 0 ? 0 : (server.getRandom().nextDouble() - 0.5) * 3.0;
            double oz = released == 0 ? 0 : (server.getRandom().nextDouble() - 0.5) * 3.0;
            entity.snapTo(at.getX() + 0.5 + ox, at.getY(), at.getZ() + 0.5 + oz, server.getRandom().nextFloat() * 360.0F, 0.0F);
            if (entity instanceof Mob mob) {
                mob.setPersistenceRequired();
            }
            if (entity instanceof CloudWhaleEntity whale) {
                whale.setHome(at); // fica rondando onde foi solta (no Sonho ou fora)
            }
            server.addFreshEntity(entity);
            server.sendParticles(ParticleTypes.PORTAL, entity.getX(), entity.getY() + entity.getBbHeight() / 2, entity.getZ(), 40,
                    entity.getBbWidth() / 2, entity.getBbHeight() / 2, entity.getBbWidth() / 2, 0.3);
            released++;
        }
        setStored(stack, list);
        if (released > 0) {
            server.playSound(null, at, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.2F, 0.6F);
            if (player != null) {
                stack.hurtAndBreak(1, player, context.getHand());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return !stored(stack).isEmpty();
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        ListTag list = stored(stack);
        builder.accept(Component.translatable("tooltip.mistech.spatial_dreamcatcher.count", list.size(), CAPACITY)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
        for (int i = 0; i < list.size(); i++) {
            builder.accept(Component.literal(" • " + list.getCompoundOrEmpty(i).getStringOr("Name", "?")).withStyle(ChatFormatting.DARK_PURPLE));
        }
        builder.accept(Component.translatable("tooltip.mistech.spatial_dreamcatcher.help").withStyle(ChatFormatting.GRAY));
    }
}
