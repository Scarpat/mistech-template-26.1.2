package com.mistech.mistechmod.machine;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.mistech.mistechmod.Mistech;
import com.mistech.mistechmod.recipe.CrushingRecipe;
import com.mistech.mistechmod.registry.MistechBlockEntities;
import com.mistech.mistechmod.registry.MistechRecipes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;

/**
 * Mineradora (multibloco, ver {@link MinerStructure}). Enquanto tiver energia, quebra bloco por bloco da chunk
 * onde está, camada por camada, começando logo abaixo da base e descendo até o fundo do mundo.
 * Tudo vai para o Barril Espacial do centro; barril cheio = pausa.
 *
 * <p>4 espaços de runa (cada runa conta 1 nível; runas repetidas somam):
 * <ul>
 * <li>Espaço: área em chunks 1×1 → 3×3 → 5×5 → 7×7 → 9×9 (centrada na chunk da Mineradora).</li>
 * <li>Tempo: velocidade 2 → 4 → 10 → 20 → 40 blocos/s.</li>
 * <li>Energia: gasto por bloco 100 → 80 → 60 → 40 → 20 FE.</li>
 * <li>Fogo: derrete os drops (minério bruto vira lingote, pedregulho vira pedra...).</li>
 * <li>Terra: minério vira pó (receitas do Triturador, com o bônus da Runa da Terra). Com Fogo junto, o pó vira lingote.</li>
 * <li>Vazio: liga o filtro de lixo (9 espaços fantasmas); o que bater com o filtro é destruído.</li>
 * </ul>
 * Espaços: 0..3 = runas, 4..12 = filtro. Picareta virtual de diamante.
 * info0 = situação (bits 0-2) + níveis de Espaço/Tempo/Energia (3 bits cada, a partir do bit 3) + Fogo/Terra/Vazio
 * (bits 12, 13, 14); info1 = camada (Y) atual.
 */
public class MinerBlockEntity extends MachineBlockEntity {
    public static final int CAPACITY = 100_000;
    public static final int MAX_INPUT = 2_000;
    public static final int RUNE_SLOTS = 4;
    public static final int FILTER_START = RUNE_SLOTS, FILTER_SLOTS = 9;

    /** Ticks entre operações e blocos por operação, por nível de Tempo (0 a 4). */
    public static final int[] INTERVAL_BY_TIME = {10, 5, 2, 1, 1};
    public static final int[] BLOCKS_BY_TIME = {1, 1, 1, 1, 2};
    public static final int BASE_ENERGY_PER_BLOCK = 100;
    public static final int ENERGY_SAVED_PER_RUNE = 20;
    /** Quantas posições vazias (ar, líquido...) pode pular por operação sem gastar nada. */
    public static final int SCAN_LIMIT = 1024;
    /**
     * A cada quantos blocos minerados cada runa instalada gasta 1 uso. 0 = runas não se gastam
     * (são melhorias permanentes enquanto estiverem na Mineradora).
     */
    public static final int RUNE_BLOCKS_PER_USE = 0;

    public static final int STATUS_WORKING = 0, STATUS_NO_ENERGY = 1, STATUS_FULL = 2, STATUS_INCOMPLETE = 3,
            STATUS_FINISHED = 4, STATUS_WAITING = 5, STATUS_NO_BARREL = 6;

    private static ItemStack tool;

    private int status = STATUS_INCOMPLETE;
    private boolean structureOk;
    private int structureCheck;
    /** Cursor: camada atual, índice dentro da camada e raio (em chunks) com que o cursor começou. */
    private int layerY = Integer.MIN_VALUE;
    private int index;
    private int cursorRadius = -1;
    private int minedSinceWear;
    /** Visual das peças já conferido desde que carregou / brocas das pernas girando agora. */
    private boolean formedChecked, shownWorking;

    // níveis das runas (recalculados a cada tick)
    private int space, time, energyLvl;
    private boolean fire, earth, voidFilter;

    public MinerBlockEntity(BlockPos pos, BlockState state) {
        super(MistechBlockEntities.MINER.get(), pos, state, MachineLayouts.MINER, MAX_INPUT, 0);
    }

    public static ItemStack tool() {
        if (tool == null) {
            tool = new ItemStack(Items.DIAMOND_PICKAXE);
        }
        return tool;
    }

    public static boolean isMinerRune(ItemStack stack) {
        return stack.is(Mistech.FIRE_RUNE.get()) || stack.is(Mistech.EARTH_RUNE.get()) || stack.is(Mistech.SPACE_RUNE.get())
                || stack.is(Mistech.TIME_RUNE.get()) || stack.is(Mistech.ENERGY_RUNE.get()) || stack.is(Mistech.VOID_RUNE.get());
    }

    private int countRunes(Item rune) {
        int count = 0;
        for (int i = 0; i < RUNE_SLOTS; i++) {
            if (this.items.get(i).is(rune)) {
                count++;
            }
        }
        return count;
    }

    private void readRunes() {
        this.space = this.countRunes(Mistech.SPACE_RUNE.get());
        this.time = this.countRunes(Mistech.TIME_RUNE.get());
        this.energyLvl = this.countRunes(Mistech.ENERGY_RUNE.get());
        this.fire = this.countRunes(Mistech.FIRE_RUNE.get()) > 0;
        this.earth = this.countRunes(Mistech.EARTH_RUNE.get()) > 0;
        this.voidFilter = this.countRunes(Mistech.VOID_RUNE.get()) > 0;
    }

    public static int energyPerBlock(int energyLevel) {
        return BASE_ENERGY_PER_BLOCK - ENERGY_SAVED_PER_RUNE * Math.min(4, energyLevel);
    }

    /** Menor Y e lado (em blocos) da área; a área é centrada na chunk do núcleo. */
    private int areaMinX(BlockPos pos) {
        return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(pos.getX()) - this.space);
    }

    private int areaMinZ(BlockPos pos) {
        return SectionPos.sectionToBlockCoord(SectionPos.blockToSectionCoord(pos.getZ()) - this.space);
    }

    private int areaWidth() {
        return (this.space * 2 + 1) * 16;
    }

    /** Primeira camada minerada: logo abaixo da base (o núcleo fica 1 acima da base). */
    private int topLayer(BlockPos pos) {
        return pos.getY() - 2;
    }

    @Override
    protected void tick(ServerLevel level, BlockPos pos, BlockState state) {
        this.readRunes();
        if (--this.structureCheck <= 0) {
            this.structureCheck = 20;
            boolean ok = MinerStructure.isComplete(level, pos);
            if (ok != this.structureOk || !this.formedChecked) {
                // peça saiu: o resto volta ao normal; peça voltou: remonta o visual
                MinerStructure.setFormed(level, pos, ok);
                this.formedChecked = true;
                this.shownWorking = false;
            }
            this.structureOk = ok;
        }
        // mudou a área (runa do Espaço entrou/saiu) ou primeira vez: recomeça do topo
        if (this.cursorRadius != this.space || this.layerY == Integer.MIN_VALUE) {
            this.cursorRadius = this.space;
            this.layerY = this.topLayer(pos);
            this.index = 0;
            this.setChanged();
        }

        if (!this.structureOk) {
            this.status = STATUS_INCOMPLETE;
            this.progress = 0;
        } else if (this.layerY < level.getMinY()) {
            this.status = STATUS_FINISHED;
            this.progress = 0;
        } else {
            if (this.status == STATUS_INCOMPLETE) {
                this.status = STATUS_WORKING; // estrutura voltou: mostra "Minerando" até a próxima operação dizer outra coisa
            }
            this.maxProgress = INTERVAL_BY_TIME[Math.min(4, this.time)] * 2;
            this.progress += this.workStep();
            if (this.progress >= this.maxProgress) {
                this.progress = 0;
                int blocks = BLOCKS_BY_TIME[Math.min(4, this.time)];
                for (int i = 0; i < blocks; i++) {
                    if (!this.mineNext(level, pos)) {
                        break;
                    }
                }
                this.setChanged();
            }
        }

        this.info0 = (this.status & 7) | (this.space << 3) | (this.time << 6) | (this.energyLvl << 9)
                | (this.fire ? 1 << 12 : 0) | (this.earth ? 1 << 13 : 0) | (this.voidFilter ? 1 << 14 : 0);
        this.info1 = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, this.layerY));
        boolean working = this.status == STATUS_WORKING && this.structureOk;
        this.setLit(level, pos, state, working);
        if (working != this.shownWorking) {
            this.shownWorking = working;
            MinerStructure.setWorking(level, pos, working);
        }
    }

    /** Anda o cursor até achar um bloco e minera. false = parou (sem energia, barril cheio, acabou, chunk descarregada). */
    private boolean mineNext(ServerLevel level, BlockPos pos) {
        int width = this.areaWidth();
        int minX = this.areaMinX(pos), minZ = this.areaMinZ(pos);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int scanned = 0; scanned < SCAN_LIMIT; scanned++) {
            if (this.layerY < level.getMinY()) {
                this.status = STATUS_FINISHED;
                return false;
            }
            cursor.set(minX + this.index % width, this.layerY, minZ + this.index / width);
            if (!level.isLoaded(cursor)) {
                this.status = STATUS_WAITING;
                return false;
            }
            BlockState target = level.getBlockState(cursor);
            if (!MiningHelper.canMine(level, cursor, target, tool())) {
                this.advance(width);
                continue;
            }
            int cost = energyPerBlock(this.energyLvl); // Impulso de Gnomo acelera sem encarecer o bloco
            if (!this.hasEnergy(cost)) {
                this.status = STATUS_NO_ENERGY;
                return false;
            }
            ResourceHandler<ItemResource> barrel = MiningHelper.container(level, pos.below(), Direction.UP);
            if (barrel == null) {
                this.status = STATUS_NO_BARREL;
                return false;
            }
            BlockPos targetPos = cursor.immutable();
            List<ItemStack> drops = this.process(level, MiningHelper.drops(level, targetPos, target, tool()), target);
            if (!MiningHelper.insertAll(barrel, drops)) {
                this.status = STATUS_FULL;
                return false;
            }
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, target),
                    targetPos.getX() + 0.5, targetPos.getY() + 0.5, targetPos.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.05);
            level.destroyBlock(targetPos, false);
            this.drainEnergy(cost);
            this.wearRunes();
            this.advance(width);
            this.status = STATUS_WORKING;
            return true;
        }
        this.status = STATUS_WORKING; // só pulou vazio; continua na próxima operação
        return false;
    }

    private void advance(int width) {
        if (++this.index >= width * width) {
            this.index = 0;
            this.layerY--;
        }
    }

    // ----- drops: filtro, Terra e Fogo -----

    private List<ItemStack> process(ServerLevel level, List<ItemStack> raw, BlockState mined) {
        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : raw) {
            if (drop.isEmpty() || this.isTrash(drop)) {
                continue;
            }
            result.add(drop.copy());
        }
        // Terra: minério → pó (usa o próprio bloco de minério na receita do Triturador)
        if (this.earth && mined.is(Tags.Blocks.ORES) && !result.isEmpty()) {
            ItemStack oreItem = new ItemStack(mined.getBlock().asItem());
            Optional<RecipeHolder<CrushingRecipe>> crushing = oreItem.isEmpty() ? Optional.empty()
                    : level.recipeAccess().getRecipeFor(MistechRecipes.CRUSHING_TYPE.get(), new SingleRecipeInput(oreItem), level);
            if (crushing.isPresent()) {
                CrushingRecipe recipe = crushing.get().value();
                ItemStack dust = recipe.result().create();
                if (recipe.bonusChance() > 0 && level.getRandom().nextFloat() < recipe.bonusChance()) {
                    dust.grow(1);
                }
                if (recipe.runeBonus()) {
                    dust.grow(1);
                }
                result.clear();
                if (!this.isTrash(dust)) {
                    result.add(dust);
                }
            }
        }
        // Fogo: derrete o que tiver receita de fornalha
        if (this.fire) {
            for (int i = 0; i < result.size(); i++) {
                ItemStack stack = result.get(i);
                Optional<RecipeHolder<SmeltingRecipe>> smelting = level.recipeAccess()
                        .getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(stack), level);
                if (smelting.isPresent()) {
                    ItemStack smelted = smelting.get().value().assemble(new SingleRecipeInput(stack));
                    if (!smelted.isEmpty()) {
                        smelted.setCount(smelted.getCount() * stack.getCount());
                        result.set(i, smelted);
                    }
                }
            }
            result.removeIf(this::isTrash);
        }
        return result;
    }

    /** Bate com o filtro de lixo? (só com Runa do Vazio instalada; compara só o item) */
    private boolean isTrash(ItemStack stack) {
        if (!this.voidFilter) {
            return false;
        }
        for (int i = FILTER_START; i < FILTER_START + FILTER_SLOTS; i++) {
            ItemStack sample = this.items.get(i);
            if (!sample.isEmpty() && ItemStack.isSameItem(sample, stack)) {
                return true;
            }
        }
        return false;
    }

    /** Desgaste opcional das runas ({@link #RUNE_BLOCKS_PER_USE}). */
    private void wearRunes() {
        if (RUNE_BLOCKS_PER_USE <= 0 || ++this.minedSinceWear < RUNE_BLOCKS_PER_USE) {
            return;
        }
        this.minedSinceWear = 0;
        for (int i = 0; i < RUNE_SLOTS; i++) {
            ItemStack rune = this.items.get(i);
            if (rune.isEmpty() || !rune.isDamageableItem()) {
                continue;
            }
            int damage = rune.getDamageValue() + 1;
            if (damage >= rune.getMaxDamage()) {
                this.items.set(i, ItemStack.EMPTY);
            } else {
                rune.setDamageValue(damage);
            }
        }
    }

    // ----- Câmara Coletora (o barril embaixo) -----

    public static final int BUTTON_OPEN_CHAMBER = 0;

    /** O barril embaixo, se a Mineradora estiver montada. */
    public com.mistech.mistechmod.block.@org.jspecify.annotations.Nullable SpatialBarrelBlockEntity chamber() {
        if (this.level == null) {
            return null;
        }
        BlockPos below = this.worldPosition.below();
        BlockState barrel = this.level.getBlockState(below);
        if (!barrel.is(Mistech.SPATIAL_BARREL.get()) || !barrel.getValue(com.mistech.mistechmod.block.SpatialBarrelBlock.FORMED)) {
            return null;
        }
        return this.level.getBlockEntity(below) instanceof com.mistech.mistechmod.block.SpatialBarrelBlockEntity be ? be : null;
    }

    /** Abre o barril de dentro da Mineradora (clique agachado no núcleo ou botão da tela). */
    public boolean openChamber(net.minecraft.world.entity.player.Player player) {
        com.mistech.mistechmod.block.SpatialBarrelBlockEntity barrel = this.chamber();
        if (barrel == null) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.mistech.miner_no_chamber"));
            return false;
        }
        player.openMenu(barrel);
        return true;
    }

    @Override
    public boolean onMenuButton(net.minecraft.world.entity.player.Player player, int id) {
        return id == BUTTON_OPEN_CHAMBER && this.openChamber(player);
    }

    // ----- espaços -----

    /** Amostras do filtro não são itens de verdade: somem antes de o bloco derrubar o conteúdo. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        for (int i = FILTER_START; i < FILTER_START + FILTER_SLOTS; i++) {
            this.items.set(i, ItemStack.EMPTY);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void loadExtra(ValueInput input) {
        this.layerY = input.getIntOr("LayerY", Integer.MIN_VALUE);
        this.index = input.getIntOr("Index", 0);
        this.cursorRadius = input.getIntOr("CursorRadius", -1);
        this.minedSinceWear = input.getIntOr("MinedSinceWear", 0);
    }

    @Override
    protected void saveExtra(ValueOutput output) {
        output.putInt("LayerY", this.layerY);
        output.putInt("Index", this.index);
        output.putInt("CursorRadius", this.cursorRadius);
        output.putInt("MinedSinceWear", this.minedSinceWear);
    }
}
