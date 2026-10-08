package com.mistech.mistechmod.ranged;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.item.RuneElement;
import com.mistech.mistechmod.life.LifeRuneItem;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Virote Arcano (Besta Mágica): voa reto (sem gravidade) por até {@link #LIFETIME} ticks, deixa rastro na cor da runa
 * e, ao acertar, causa {@link #damage} e o efeito do elemento ({@link BoltEffects}). O Arcano persegue inimigos.
 */
public class ArcaneBoltEntity extends ThrowableItemProjectile {
    public static final int LIFETIME = 80;
    private RuneElement element = RuneElement.FIRE;
    private float damage = ArcaneCrossbowItem.BOLT_DAMAGE;

    public ArcaneBoltEntity(EntityType<? extends ArcaneBoltEntity> type, Level level) {
        super(type, level);
    }

    public ArcaneBoltEntity(Level level, LivingEntity owner, RuneElement element, float damage) {
        super(RangedContent.ARCANE_BOLT_ENTITY.get(), owner, level, colored(element));
        this.element = element;
        this.damage = damage;
    }

    private static ItemStack colored(RuneElement element) {
        ItemStack stack = new ItemStack(RangedContent.ARCANE_BOLT.get());
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(), List.of(), List.of(element.color())));
        return stack;
    }

    public RuneElement element() {
        return this.element;
    }

    @Override
    protected Item getDefaultItem() {
        return RangedContent.ARCANE_BOLT.get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.tickCount > LIFETIME) {
            this.discard();
            return;
        }
        level.sendParticles(new DustParticleOptions(this.element.color(), 1.3F), this.getX(), this.getY(), this.getZ(), 2, 0.05, 0.05, 0.05, 0.0);
        if (this.element == RuneElement.ARCANE && this.tickCount > 2) {
            LivingEntity target = null;
            double best = 16.0 * 16.0;
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(16.0),
                    e -> e instanceof Enemy && e.isAlive())) {
                double d = e.distanceToSqr(this);
                if (d < best) {
                    best = d;
                    target = e;
                }
            }
            if (target != null) {
                Vec3 vel = this.getDeltaMovement();
                Vec3 want = target.getBoundingBox().getCenter().subtract(this.position()).normalize().scale(vel.length());
                this.setDeltaMovement(vel.scale(0.8).add(want.scale(0.2)));
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult hit) {
        super.onHitEntity(hit);
        if (!(this.level() instanceof ServerLevel level) || !(hit.getEntity() instanceof LivingEntity target)) {
            return;
        }
        Player shooter = this.getOwner() instanceof Player p ? p : null;
        if (this.element == RuneElement.LIFE && shooter != null && LifeRuneItem.isAlly(shooter, target)) {
            BoltEffects.apply(level, this, shooter, hit.getLocation(), null, null);
            return;
        }
        float amount = this.damage * (this.element == RuneElement.LUCK && level.getRandom().nextFloat() < 0.35F ? 2.0F : 1.0F);
        target.hurtServer(level, level.damageSources().thrown(this, this.getOwner()), amount);
        target.invulnerableTime = 0;
        BoltEffects.apply(level, this, shooter, hit.getLocation(), target, null);
    }

    @Override
    protected void onHitBlock(BlockHitResult hit) {
        super.onHitBlock(hit);
        if (this.level() instanceof ServerLevel level) {
            BoltEffects.apply(level, this, this.getOwner() instanceof Player p ? p : null, hit.getLocation(), null, hit);
        }
    }

    @Override
    protected void onHit(HitResult hit) {
        super.onHit(hit);
        if (!this.level().isClientSide()) {
            this.discard();
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putString("Element", this.element.id());
        output.putFloat("Damage", this.damage);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        RuneElement e = RuneElement.byId(input.getStringOr("Element", "fire"));
        this.element = e == null ? RuneElement.FIRE : e;
        this.damage = input.getFloatOr("Damage", ArcaneCrossbowItem.BOLT_DAMAGE);
    }

}
