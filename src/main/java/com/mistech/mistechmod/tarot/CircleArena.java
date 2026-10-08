package com.mistech.mistechmod.tarot;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Arena padrão: disco de pedra-negra com anel de ametista, sigilo de obsidiana chorona no centro, 8 colunas e mureta. */
public class CircleArena implements ArenaLayout {
    public static final CircleArena INSTANCE = new CircleArena(ArcanePlane.ARENA_RADIUS);

    private final int radius;

    public CircleArena(int radius) {
        this.radius = radius;
    }

    @Override
    public double leashRadius() {
        return this.radius + 4.0;
    }

    @Override
    public int clearRadius() {
        return ArenaTools.MAX_CLEAR_RADIUS;
    }

    @Override
    public Vec3 arrival(BlockPos center, int index) {
        double angle = Math.PI / 2.0 + index * 0.7;
        double dist = this.radius - 5;
        return new Vec3(center.getX() + 0.5 + Math.cos(angle) * dist, center.getY() + 1.0, center.getZ() + 0.5 + Math.sin(angle) * dist);
    }

    @Override
    public void build(ServerLevel level, BlockPos center) {
        ArenaTools.clear(level, center, this);
        BlockState floor = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
        BlockState ring = Blocks.AMETHYST_BLOCK.defaultBlockState();
        BlockState sigil = Blocks.CRYING_OBSIDIAN.defaultBlockState();
        BlockState line = Blocks.PURPUR_BLOCK.defaultBlockState();
        BlockState wall = Blocks.POLISHED_BLACKSTONE_BRICK_WALL.defaultBlockState();
        int flags = Block.UPDATE_CLIENTS;
        int radius = this.radius;
        for (int dx = -radius - 1; dx <= radius + 1; dx++) {
            for (int dz = -radius - 1; dz <= radius + 1; dz++) {
                double d = Math.sqrt(dx * dx + dz * dz);
                if (d > radius + 0.5) {
                    continue;
                }
                BlockPos pos = center.offset(dx, 0, dz);
                BlockState state;
                if (d > radius - 1.5) {
                    state = ring;
                } else if (Math.abs(dx) <= 1 && Math.abs(dz) <= 1) {
                    state = sigil;
                } else if ((Math.abs(dx) == Math.abs(dz) || dx == 0 || dz == 0) && d < radius - 3) {
                    state = line;
                } else if (Math.abs(d - radius * 0.5) < 0.5) {
                    state = ring;
                } else {
                    state = floor;
                }
                level.setBlock(pos, state, flags);
                level.setBlock(pos.below(), Blocks.BLACKSTONE.defaultBlockState(), flags);
                if (d > radius - 0.5) {
                    level.setBlock(pos.above(), wall, flags);
                }
            }
        }
        // 8 colunas com luz no topo
        for (int i = 0; i < 8; i++) {
            double angle = i * Math.PI / 4.0 + Math.PI / 8.0;
            BlockPos base = center.offset((int) Math.round(Math.cos(angle) * (radius - 3)), 0,
                    (int) Math.round(Math.sin(angle) * (radius - 3)));
            for (int dy = 1; dy <= 4; dy++) {
                level.setBlock(base.above(dy), Blocks.PURPUR_PILLAR.defaultBlockState(), flags);
            }
            level.setBlock(base.above(5), Blocks.SEA_LANTERN.defaultBlockState(), flags);
            level.setBlock(base.above(6), Blocks.END_ROD.defaultBlockState(), flags);
        }
    }
}
