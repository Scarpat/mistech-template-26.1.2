package com.mistech.mistechmod.refining;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.mistech.mistechmod.celestial.CelestialContent;
import com.mistech.mistechmod.machine.MachineBlockEntity;
import com.mistech.mistechmod.machine.arcane.ArcaneMachineBlock;
import com.mistech.mistechmod.purity.PurityContent;
import com.mistech.mistechmod.purity.block.PurityFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * Núcleo do Prisma Astral (tier 5× da cadeia): à noite e a céu aberto, transmuta lotes de até {@value #BATCH} minérios em
 * 5 pós cada (receitas {@code transmutation}). Por lote: {@value #ESSENCE_PER_BATCH} mB de Essência Purificada
 * (proporcional ao tamanho) e {@value #SECONDS} s; 1 Fragmento Estelar a cada {@value #ORES_PER_FRAGMENT} minérios
 * (5% de chance de não gastar). {@value #FE} FE/t.
 * <p>
 * Lua nova ×1,5 ... lua cheia ×0,75; chuva ×0,5; tempestade, dia ou telhado = parado. Estrutura em
 * {@link AstralPrismStructure} (conferida a cada 2 s).
 * Espaços: 0 = minérios, 1 = Fragmentos Estelares, 2..3 = saída. info0 = tanque (mB), info1 = velocidade (%), info2 = estado.
 */
public class AstralPrismBlockEntity extends MachineBlockEntity implements ArcaneMachineBlock.EssenceTankHolder {
    public static final int TANK = 8000;
    public static final int BATCH = 8, ESSENCE_PER_BATCH = 500, SECONDS = 30, FE = 200, ORES_PER_FRAGMENT = 16;
    public static final int SLOT_ORE = 0, SLOT_FRAGMENT = 1;
    /** Velocidade por fase da lua (0 = cheia, 4 = nova). */
    private static final double[] MOON = {0.75, 0.9, 1.05, 1.2, 1.5, 1.2, 1.05, 0.9};

    public static final int ST_WORKING = 0, ST_STRUCTURE = 1, ST_DAY = 2, ST_NO_SKY = 3, ST_STORM = 4, ST_NO_ESSENCE = 5,
            ST_NO_FRAGMENT = 6, ST_NO_ENERGY = 7, ST_IDLE = 8, ST_FULL = 9;
    public static final String[] STATUS_KEYS = {"working", "structure", "day", "no_sky", "storm", "no_essence", "no_fragment",
            "no_energy", "idle", "full"};

    private final PurityFluids.Tanks tank = new PurityFluids.Tanks(TANK, new FluidResource[] {PurityFluids.pure()},
            new boolean[] {true}, new boolean[] {false}, this::setChanged);
    private int batch;
    private int fragmentCredit;
    private boolean formed;
    private int structureTimer;
    private double carry;
    private @Nullable RecipeHolder<OreRefiningRecipe> lastRecipe;

    public AstralPrismBlockEntity(BlockPos pos, BlockState state) {
        super(RefiningContent.ASTRAL_PRISM_BE.get(), pos, state, RefiningContent.PRISM_LAYOUT, 1000, 0);
        this.maxProgress = SECONDS * 20 * 2;
    }

    public PurityFluids.Tanks getTank() {
        return this.tank;
    }

    public boolean formed() {
        return this.formed;
    }

    private @Nullable OreRefiningRecipe recipe(ServerLevel level, ItemStack ore) {
        if (ore.isEmpty()) {
            return null;
        }
        var found = level.recipeAccess().getRecipeFor(RefiningContent.TYPE.get(),
                new OreRefiningRecipe.Input(OreRefiningRecipe.Stage.TRANSMUTATION, ore), level, this.lastRecipe);
        found.ifPresent(r -> this.lastRecipe = r);
        return found.map(RecipeHolder::value).orElse(null);
    }

    /** Velocidade do céu agora (0 = parado) e o estado que explica. */
    private double sky(ServerLevel level, BlockPos pos) {
        if (!level.isDarkOutside()) {
            this.setStatus(ST_DAY);
            return 0;
        }
        if (!level.canSeeSky(pos.above(2))) {
            this.setStatus(ST_NO_SKY);
            return 0;
        }
        if (level.isThundering()) {
            this.setStatus(ST_STORM);
            return 0;
        }
        int phase = level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, pos).index() & 7;
        double f = MOON[phase] * com.mistech.mistechmod.celestial.MachineSky.prism(level);
        if (level.isRaining()) {
            f *= 0.5;
        }
        return f;
    }

    private void setStatus(int status) {
        this.info2 = status;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.info0 = this.tank.amount(0);
        if (--this.structureTimer <= 0) {
            this.structureTimer = 40;
            this.formed = AstralPrismStructure.firstError(level, pos) == null;
        }
        if (!this.formed) {
            this.setStatus(ST_STRUCTURE);
            this.info1 = 0;
            this.setLit(level, pos, state, false);
            return;
        }
        double sky = this.sky(level, pos);
        this.info1 = (int) Math.round(sky * 100);
        ItemStack ore = this.items.get(SLOT_ORE);
        OreRefiningRecipe recipe = this.recipe(level, ore);
        if (this.batch == 0) {
            this.progress = 0;
            if (recipe == null) {
                this.setStatus(sky > 0 ? ST_IDLE : this.info2);
                this.setLit(level, pos, state, false);
                return;
            }
            if (sky <= 0) {
                this.setLit(level, pos, state, false);
                return;
            }
            if (!this.startBatch(recipe, ore)) {
                this.setLit(level, pos, state, false);
                return;
            }
        }
        if (sky <= 0) {
            this.setLit(level, pos, state, false);
            return;
        }
        int cost = this.energyStep(FE);
        if (!this.hasEnergy(cost)) {
            this.setStatus(ST_NO_ENERGY);
            this.setLit(level, pos, state, false);
            return;
        }
        this.drainEnergy(cost);
        double step = this.workStep() * sky + this.carry;
        int whole = (int) Math.floor(step);
        this.carry = step - whole;
        this.progress += whole;
        this.setStatus(ST_WORKING);
        if (level.getGameTime() % 5 == 0) {
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 2.2, pos.getZ() + 0.5, 2, 0.2, 0.6, 0.2, 0.01);
        }
        if (this.progress >= this.maxProgress) {
            this.finishBatch(level, pos, recipe, ore);
        }
        this.setChanged();
        this.setLit(level, pos, state, true);
    }

    /** Paga a essência e o fragmento do lote; false (com o estado) se faltar algo. */
    private boolean startBatch(OreRefiningRecipe recipe, ItemStack ore) {
        int n = Math.min(BATCH, ore.getCount());
        int essence = (int) Math.ceil(ESSENCE_PER_BATCH * n / (double) BATCH);
        ItemStack sample = recipe.result().create();
        sample.setCount(Math.min(sample.getMaxStackSize(), sample.getCount() * n));
        if (!this.canOutputProduct(sample)) {
            this.setStatus(ST_FULL);
            return false;
        }
        if (this.tank.amount(0) < essence) {
            this.setStatus(ST_NO_ESSENCE);
            return false;
        }
        if (this.fragmentCredit < n && this.level != null && com.mistech.mistechmod.celestial.MachineSky.freeFragments(this.level)) {
            this.fragmentCredit += ORES_PER_FRAGMENT; // Chuva de Estrelas: o céu paga o fragmento
        }
        if (this.fragmentCredit < n) {
            ItemStack fragment = this.items.get(SLOT_FRAGMENT);
            if (fragment.isEmpty()) {
                this.setStatus(ST_NO_FRAGMENT);
                return false;
            }
            if (this.level == null || this.level.getRandom().nextFloat() >= 0.05F) {
                fragment.shrink(1);
            }
            this.fragmentCredit += ORES_PER_FRAGMENT;
        }
        this.fragmentCredit -= n;
        this.tank.take(0, essence);
        this.batch = n;
        this.progress = 0;
        this.carry = 0;
        return true;
    }

    private void finishBatch(ServerLevel level, BlockPos pos, @Nullable OreRefiningRecipe recipe, ItemStack ore) {
        int n = recipe == null ? 0 : Math.min(this.batch, ore.getCount());
        for (int i = 0; i < n; i++) {
            ItemStack product = recipe.result().create();
            if (this.hasCelestialCore()) {
                product.grow(1); // Núcleo Celeste: 6 pós por minério (Fase 9)
            }
            this.insertProduct(level, product);
            for (ItemStack extra : recipe.rollByproducts(level.getRandom())) {
                ItemStack rest = this.insertOutput(extra);
                if (!rest.isEmpty()) {
                    net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, rest);
                }
            }
        }
        ore.shrink(n);
        this.batch = 0;
        this.progress = 0;
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.6F);
        level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.6F, 1.6F);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.6, pos.getZ() + 0.5, 30, 0.6, 0.6, 0.6, 0.05);
    }

    // ------------------------------------------------------------------ essência na mão, Jade, salvar

    @Override
    public boolean handles(ItemStack stack) {
        return stack.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get()) || stack.is(PurityContent.PURIFIED_ESSENCE_BUCKET.get());
    }

    @Override
    public void interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        boolean bottle = held.is(PurityContent.PURIFIED_ESSENCE_BOTTLE.get());
        int amount = bottle ? PurityFluids.BOTTLE : PurityFluids.BUCKET;
        if (this.tank.space(0) < amount) {
            player.sendOverlayMessage(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.purified_essence"),
                    this.tank.amount(0), TANK));
            return;
        }
        this.tank.add(0, amount);
        PurityFluids.swap(player, hand, new ItemStack(bottle ? Items.GLASS_BOTTLE : Items.BUCKET));
        player.level().playSound(null, this.worldPosition, bottle ? SoundEvents.BOTTLE_EMPTY : SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    @Override
    protected boolean acceptsCelestialCoreWithoutSockets() {
        return true;
    }

    public static boolean isFragment(ItemStack stack) {
        return stack.is(CelestialContent.STAR_FRAGMENT.get());
    }

    @Override
    public void addInfo(List<Component> lines) {
        lines.add(Component.translatable("gui.mistech.astral_prism.status." + STATUS_KEYS[Math.min(this.info2, STATUS_KEYS.length - 1)]));
        lines.add(Component.translatable("info.mistech.tank", Component.translatable("block.mistech.purified_essence"), this.tank.amount(0), TANK));
        if (this.batch > 0) {
            lines.add(Component.translatable("info.mistech.astral_prism.batch", this.batch));
        }
        super.addInfo(lines);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.tank.setAmount(0, input.getIntOr("Essence", 0));
        this.batch = input.getIntOr("Batch", 0);
        this.fragmentCredit = input.getIntOr("FragmentCredit", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("Essence", this.tank.amount(0));
        output.putInt("Batch", this.batch);
        output.putInt("FragmentCredit", this.fragmentCredit);
    }
}
