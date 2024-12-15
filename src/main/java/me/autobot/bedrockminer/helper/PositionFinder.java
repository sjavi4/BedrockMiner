package me.autobot.bedrockminer.helper;

import com.mojang.datafixers.util.Pair;
import me.autobot.bedrockminer.mixin.MixinGetterBaseTorchBlock;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.piston.PistonBaseBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

public class PositionFinder {

    private static final Minecraft client = Minecraft.getInstance();
    private static ClientLevel level = client.level;
    private static LocalPlayer player = client.player;

    public static void init() {
        level = client.level;
        player = client.player;
    }

    public static Pair<BlockPos, BlockState> findPistonPos(BlockPos bedrockPos) {
        BlockPos pistonPos = bedrockPos.above();
        Block block = Blocks.PISTON;
        ItemStack itemStack = InventoryHelper.Type.PISTON.getItem();

        if (itemStack == null) {
            return null;
        }

        // Above + Facing UP
        // Above + Facing Horizontal
        if (level.getBlockState(pistonPos).canBeReplaced()) {

            // Can Push Up
            if (iteratePushable(pistonPos.above(), pistonPos, Direction.UP)) {
                return Pair.of(pistonPos, createBlockState(block, pistonPos, itemStack, Direction.UP));
            }
            // Can Push Side
//            for (Direction d : Direction.Plane.HORIZONTAL) {
//                if (iteratePushable(pistonPos.relative(d), pistonPos, d)) {
//                    return Pair.of(pistonPos, createBlockState(block, pistonPos, itemStack, d));
//                }
//            }
        }

        // Bedrock Sides + Facing UP
        // Bedrock Sides + Facing Horizontal

//        for (Direction d : Direction.Plane.HORIZONTAL) {
//            pistonPos = bedrockPos.relative(d);
//            if (level.getBlockState(pistonPos).canBeReplaced()) {
//
//                // Can Push Up
//                if (iteratePushable(pistonPos.above(), pistonPos, Direction.UP)) {
//                    return Pair.of(pistonPos, createBlockState(block, pistonPos, itemStack, Direction.UP));
//                }
//                // Can Push Side
////                for (Direction side : Direction.Plane.HORIZONTAL) {
////                    if (iteratePushable(pistonPos.relative(side), pistonPos, side)) {
////                        return Pair.of(pistonPos, createBlockState(block, pistonPos, itemStack, side));
////                    }
////                }
//            }
//        }
        return null;
    }
    private static boolean iteratePushable(BlockPos pushBlockPos, BlockPos pistonPos, Direction direction) {
        // blockState = block being push
        // BlockPos = piston position
        return PistonBaseBlock.isPushable(level.getBlockState(pushBlockPos), level, pistonPos, direction, true, direction);
    }

    public static Pair<BlockPos, BlockState> findTorchPos(BlockPos pistonPos, BlockState pistonState) {
        ItemStack itemStack = InventoryHelper.Type.TORCH.getItem();

        if (itemStack == null) {
            return null;
        }

        Direction pistonFacing = pistonState.getValue(BlockStateProperties.FACING);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // Skip piston facing
            if (pistonFacing == d) {
                continue;
            }
            BlockPos torchPos = pistonPos.relative(d);

            if (!level.getBlockState(torchPos).canBeReplaced()) {
                continue;
            }

            // Still work if support blocks are powered blocks

            // Has native support blocks
            if (((MixinGetterBaseTorchBlock) Blocks.REDSTONE_TORCH).getCanSurvive(null, level, torchPos)) {
                return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_TORCH, torchPos, itemStack, Direction.DOWN));
            }
            for (Direction attach : Direction.Plane.HORIZONTAL) {
                if (torchPos.relative(attach).asLong() == pistonPos.asLong()) {
                    continue;
                }
                if (WallTorchBlock.canSurvive(level, torchPos, attach)) {
                    return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_WALL_TORCH, torchPos, itemStack, attach));
                }
            }
        }
        return null;
    }

    public static Pair<BlockPos, BlockState> findSlimePos(BlockPos pistonPos, BlockState pistonState) {
        // torch = Piston HORIZONTAL
        // slime = torch below / torch side (except piston pos)
        Block block = Blocks.SLIME_BLOCK;
        ItemStack itemStack = InventoryHelper.Type.SLIME.getItem();

        if (itemStack == null) {
            return null;
        }

        Direction pistonFacing = pistonState.getValue(BlockStateProperties.FACING);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            // Skip piston facing
            if (pistonFacing == d) {
                continue;
            }
            BlockPos torchPos = pistonPos.relative(d);
            BlockPos torchBelow = torchPos.below();
            if (level.getBlockState(torchBelow).canBeReplaced()) {
                return Pair.of(torchBelow, createBlockState(block, torchBelow, itemStack, d));
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
            return Pair.of(torchPos, createBlockState(Blocks.REDSTONE_TORCH, torchPos, InventoryHelper.Type.TORCH.getItem(), Direction.DOWN));
        }
        return null;
    }

    public static BlockState createBlockState(Block block, BlockPos pos, ItemStack itemStack, Direction d) {
        BlockState s = block.getStateForPlacement(new BlockPlaceContext(level, player, InteractionHand.MAIN_HAND, itemStack, new BlockHitResult(player.getViewVector(1f), d.getOpposite(), pos, false)));
        if (s.hasProperty(BlockStateProperties.FACING)) {
            s = s.getBlock().withPropertiesOf(s.setValue(BlockStateProperties.FACING, d));
        } else if (s.hasProperty(RedstoneWallTorchBlock.FACING)) {
            s = s.getBlock().withPropertiesOf(s.setValue(RedstoneWallTorchBlock.FACING, d));
        }
        return s;
    }


}
