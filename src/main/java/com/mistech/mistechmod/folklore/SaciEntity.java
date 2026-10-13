package com.mistech.mistechmod.folklore;

import java.util.ArrayList;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.Mistech;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * Saci (familiar, ritual "Aprisionar o Saci" em tempestade). Pula numa perna só, de gorro vermelho e cachimbo.
 * <ul>
 * <li>Alimentado (Folha de Tabaco Seca): o dono ganha <b>Velocidade</b> e <b>Salto</b> perto dele (o redemoinho),
 * e de vez em quando o Saci traz um <b>achado</b>.</li>
 * <li>Com fome: faz <b>travessura</b> — esconde uma pilha do inventário do dono em algum canto por perto.</li>
 * </ul>
 * Agachado com um Pote de Vidro vazio: o dono guarda o Saci de volta na garrafa.
 */
public class SaciEntity extends FolkFamiliar {
    public static final int GIFT_EVERY = 180;
    public static final int PRANK_EVERY = 240;
    public static final String FED_TAG = "SaciFed";

    private int giftTimer = GIFT_EVERY;
    private int prankTimer = PRANK_EVERY;

    public SaciEntity(EntityType<? extends SaciEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return createAnimalAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.JUMP_STRENGTH, 0.6);
    }

    @Override
    protected void workSecond(ServerLevel level, Player owner) {
        RandomSource random = this.getRandom();
        if (this.isFed()) {
            if (owner.distanceToSqr(this) <= 16 * 16) {
                owner.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 0, true, false, true));
                owner.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 60, 0, true, false, true));
            }
            if (--this.giftTimer <= 0) {
                this.giftTimer = GIFT_EVERY;
                if (random.nextFloat() < 0.3F) {
                    this.gift(level, owner);
                }
            }
        } else if (--this.prankTimer <= 0) {
            this.prankTimer = PRANK_EVERY;
            if (random.nextFloat() < 0.3F) {
                this.prank(level, owner);
            }
        }
        // redemoinho em volta enquanto anda
        if (this.getDeltaMovement().horizontalDistanceSqr() > 0.002) {
            for (int i = 0; i < 6; i++) {
                double a = (this.tickCount + i * 10) * 0.6;
                level.sendParticles(ParticleTypes.POOF, this.getX() + Math.cos(a) * 0.6, this.getY() + i * 0.15, this.getZ() + Math.sin(a) * 0.6,
                        1, 0, 0, 0, 0.0);
            }
        }
    }

    private void gift(ServerLevel level, Player owner) {
        RandomSource random = this.getRandom();
        List<ItemStack> options = new ArrayList<>(List.of(
                new ItemStack(Items.EMERALD, 1 + random.nextInt(2)),
                new ItemStack(Items.GOLD_NUGGET, 2 + random.nextInt(5)),
                new ItemStack(Mistech.TOBACCO_SEEDS.get(), 1 + random.nextInt(3)),
                new ItemStack(Mistech.DRIED_TOBACCO_LEAF.get(), 1 + random.nextInt(2)),
                new ItemStack(Items.RABBIT_FOOT),
                new ItemStack(Items.AMETHYST_SHARD, 1 + random.nextInt(3)),
                new ItemStack(Items.FEATHER, 2 + random.nextInt(4))));
        ItemStack stack = options.get(random.nextInt(options.size()));
        ItemEntity item = new ItemEntity(level, owner.getX(), owner.getY() + 0.5, owner.getZ(), stack);
        item.setPickUpDelay(10);
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.POOF, owner.getX(), owner.getY() + 0.5, owner.getZ(), 10, 0.3, 0.3, 0.3, 0.02);
        this.playSound(SoundEvents.WITCH_CELEBRATE, 0.8F, 1.7F);
        owner.sendOverlayMessage(Component.translatable("message.mistech.saci.gift", stack.getHoverName()).withStyle(ChatFormatting.GOLD));
    }

    /** Esconde uma pilha do inventário (fora da barra e da armadura) num canto por perto. */
    private void prank(ServerLevel level, Player owner) {
        if (owner.isCreative()) {
            return;
        }
        Inventory inv = owner.getInventory();
        RandomSource random = this.getRandom();
        List<Integer> slots = new ArrayList<>();
        for (int i = 9; i < 36; i++) {
            if (!inv.getItem(i).isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) {
            return;
        }
        int slot = slots.get(random.nextInt(slots.size()));
        ItemStack taken = inv.removeItemNoUpdate(slot);
        double a = random.nextDouble() * Math.PI * 2;
        double dist = 6 + random.nextDouble() * 6;
        int x = (int) Math.floor(owner.getX() + Math.cos(a) * dist);
        int z = (int) Math.floor(owner.getZ() + Math.sin(a) * dist);
        BlockPos top = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
        if (Math.abs(top.getY() - owner.getY()) > 10) {
            top = owner.blockPosition();
        }
        ItemEntity item = new ItemEntity(level, top.getX() + 0.5, top.getY() + 0.2, top.getZ() + 0.5, taken);
        item.setPickUpDelay(40);
        item.setUnlimitedLifetime();
        level.addFreshEntity(item);
        level.sendParticles(ParticleTypes.POOF, top.getX() + 0.5, top.getY() + 0.5, top.getZ() + 0.5, 15, 0.3, 0.5, 0.3, 0.02);
        level.playSound(null, owner.blockPosition(), SoundEvents.WITCH_CELEBRATE, SoundSource.NEUTRAL, 1.0F, 1.8F);
        owner.sendSystemMessage(Component.translatable("message.mistech.saci.prank", taken.getHoverName()).withStyle(ChatFormatting.RED));
    }

    @Override
    protected @Nullable InteractionResult interactSpecial(Player player, InteractionHand hand, ItemStack held) {
        if (held.getItem() instanceof com.mistech.mistechmod.drink.CachacaItem) {
            // oferenda: dobra o tempo alimentado e garante um achado na hora
            if (this.level() instanceof ServerLevel level) {
                held.consume(1, player);
                player.getInventory().placeItemBackInInventory(new ItemStack(Items.GLASS_BOTTLE));
                this.setFedTicks(Math.max(this.fedTicks * 2, this.fedTicks + FOOD_TICKS));
                this.giftTimer = GIFT_EVERY;
                this.gift(level, player);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 1.0, this.getZ(), 12, 0.4, 0.4, 0.4, 0.0);
                player.sendOverlayMessage(Component.translatable("message.mistech.saci.cachaca", (this.fedTicks + 1199) / 1200)
                        .withStyle(ChatFormatting.GOLD));
            }
            return InteractionResult.SUCCESS;
        }
        if (player.isShiftKeyDown() && held.is(Mistech.GLASS_JAR_ITEM.get())) {
            if (this.level() instanceof ServerLevel level) {
                held.consume(1, player);
                ItemStack jar = new ItemStack(FolkloreContent.SACI_JAR.get());
                CompoundTag tag = new CompoundTag();
                tag.putInt(FED_TAG, this.fedTicks);
                jar.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                player.getInventory().placeItemBackInInventory(jar);
                level.sendParticles(ParticleTypes.POOF, this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.3, 0.5, 0.3, 0.05);
                this.playSound(SoundEvents.BOTTLE_FILL, 1.0F, 1.0F);
                this.discard();
            }
            return InteractionResult.SUCCESS;
        }
        return null;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.getRandom().nextInt(3) == 0 ? SoundEvents.WITCH_CELEBRATE : null;
    }

    @Override
    public float getVoicePitch() {
        return 1.8F;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }
}
