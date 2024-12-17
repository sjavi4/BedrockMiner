package me.autobot.bedrockminer.helper;

import com.mojang.datafixers.util.Pair;
import me.autobot.bedrockminer.mixin.MixinGetterBaseTorchBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import static me.autobot.bedrockminer.helper.Consts.*;

public class PositionFinder {

    public static Pair<BlockPos, BlockState> findPistonPos(BlockPos bedrockPos) {
        BlockPos pistonPos = bedrockPos.above();
        Block block = Blocks.PISTON;

        // Above + Facing UP
        // Above + Facing Horizontal
        if (level.getBlockState(pistonPos).canBeReplaced()) {

            // Can Push Up
            if (findPushable(pistonPos.above(), pistonPos, Direction.UP)) {
                return Pair.of(pistonPos, createBlockState(block, Direction.UP));
            }
            // Can Push Side
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (findPushable(pistonPos.relative(d), pistonPos, d)) {
                    return Pair.of(pistonPos, createBlockState(block, d));
                }
            }
        }

        // Bedrock Sides + Facing UP
        // Bedrock Sides + Facing Horizontal

        for (Direction d : Direction.Plane.HORIZONTAL) {
            pistonPos = bedrockPos.relative(d);
            if (level.getBlockState(pistonPos).canBeReplaced()) {

                // Can Push Up
                if (findPushable(pistonPos.above(), pistonPos, Direction.UP)) {
                    return Pair.of(pistonPos, createBlockState(block, Direction.UP));
                }
                // Can Push Side
                for (Direction side : Direction.Plane.HORIZONTAL) {
                    if (findPushable(pistonPos.relative(side), pistonPos, side)) {
                        return Pair.of(pistonPos, createBlockState(block, side));
                    }
                }
            }
        }
        return null;
    }
    private static boolean findPushable(BlockPos pushBlockPos, BlockPos pistonPos, Direction direction) {
        // blockState = block being push
        // BlockPos = piston position
        return PistonBaseBlock.isPushable(level.getBlockState(pushBlockPos), level, pistonPos, direction, true, direction);
    }

    /**
     * @apiNote Find order<br>
     * 1. Piston horizontal faces: North East South West<br>
     * 2. Piston QC horizontal faces: North East South West<br>
     * Then search for supporting block<br>
     * 1. Block Top<br>
     * 2. Block Side<br>
     */
    public static Pair<BlockPos, BlockState> findTorchPos(BlockPos pistonPos, BlockState pistonState) {

        Direction pistonFacing = pistonState.getValue(BlockStateProperties.FACING);
        // piston Y and piston Y + 1
        for (int pistonOffset = 0; pistonOffset < 2; pistonOffset++) {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                // Skip piston facing
                if (pistonFacing == d && pistonOffset == 0) {
                    continue;
                }
                BlockPos torchPos = pistonPos.above(pistonOffset).relative(d);

                if (!level.getBlockState(torchPos).canBeReplaced()) {
                    continue;
                }

                // Still work if support blocks are powered blocks
                // Has native support blocks
                if (((MixinGetterBaseTorchBlock) Blocks.REDSTONE_TORCH).getCanSurvive(null, level, torchPos)) {
                    return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_TORCH, Direction.DOWN));
                }
                for (Direction attach : Direction.Plane.HORIZONTAL) {
                    if (torchPos.relative(attach).asLong() == pistonPos.asLong() || torchPos.relative(attach.getOpposite()).asLong() == pistonPos.above().asLong()) {
                        continue;
                    }
                    if (WallTorchBlock.canSurvive(level, torchPos, attach)) {
                        return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_WALL_TORCH, attach));
                    }
                }
            }
        }

        // piston Y + 2
        BlockPos torchPos = pistonPos.above(2);
        if (!level.getBlockState(torchPos).canBeReplaced()) {
            return null;
        }
        for (Direction attach : Direction.Plane.HORIZONTAL) {
            if (WallTorchBlock.canSurvive(level, torchPos, attach)) {
                return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_WALL_TORCH, attach));
            }
        }
        return null;
    }

    public static Pair<BlockPos, BlockState> findSlimePos(BlockPos pistonPos, BlockState pistonState) {
        // torch = Piston HORIZONTAL
        // slime = torch below / torch side (except piston pos)
        Block block = Blocks.SLIME_BLOCK;
        Direction pistonFacing = pistonState.getValue(BlockStateProperties.FACING);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // Skip piston facing
            if (pistonFacing == d) {
                continue;
            }
            BlockPos torchPos = pistonPos.relative(d);
            BlockPos torchBelow = torchPos.below();
            if (level.getBlockState(torchBelow).canBeReplaced()) {
                return Pair.of(torchBelow, createBlockState(block, d));
            }
//
//            for (Direction attach : Direction.Plane.HORIZONTAL) {
//                BlockPos sidePos = torchPos.relative(attach);
//                if (sidePos == pistonPos) {
//                    continue;
//                }
//                if (level.getBlockState(sidePos).canBeReplaced()) {
//                    return Pair.of(sidePos, createBlockState(block, sidePos, level, itemStack, d));
//                }
//            }
        }
        return null;
    }

    public static Pair<BlockPos, BlockState> findTorchAttachToSlime(BlockPos slimePos) {
        BlockPos torchPos = slimePos.above();
        if (level.getBlockState(torchPos).canBeReplaced()) {
            return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_TORCH, Direction.DOWN));
        }
        return null;
    }

    public static BlockState createBlockState(Block block, Direction d) {
        BlockState s = block.defaultBlockState();
        if (s.hasProperty(BlockStateProperties.FACING)) {
            return s.setValue(BlockStateProperties.FACING, d);
        } else if (s.hasProperty(RedstoneWallTorchBlock.FACING)) {
            return s.setValue(RedstoneWallTorchBlock.FACING, d);
        }
        return s;
    }


}
