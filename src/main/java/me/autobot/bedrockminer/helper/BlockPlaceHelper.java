package me.autobot.bedrockminer.helper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import static me.autobot.bedrockminer.helper.Consts.*;

public class BlockPlaceHelper {

    /**
     * @param torchPos Where does the Torch place
     * @apiNote No need to send look packet
     */
    public static void placeTorch(BlockPos torchPos, BlockState torchState) {
        Direction facing = Direction.UP;
        if (torchState.is(Blocks.REDSTONE_WALL_TORCH)) {
            facing = torchState.getValue(RedstoneWallTorchBlock.FACING);
        }
        // Find Position to attach
        Vec3 hitVec = torchPos.getCenter().relative(facing.getOpposite(), 0.5F);
        // facing = The block face to attach
        // pos = The hit block position
        BlockHitResult hitResult = new BlockHitResult(hitVec, facing, torchPos.relative(facing.getOpposite()), false);
        itemCacheMap.get(Items.REDSTONE_TORCH).switchTo();
        //InventoryHelper.setItemInHand(InventoryHelper.Type.TORCH.getSlot(), InventoryHelper.Type.TORCH.getItem());
        gameMode.useItemOn(localPlayer, InteractionHand.MAIN_HAND, hitResult);
    }

    public static void placePiston(BlockPos pistonPos, BlockState pistonState) {
        Direction facing = pistonState.getValue(BlockStateProperties.FACING);
        // Find Position to attach
        Vec3 hitVec = pistonPos.getCenter();
        // facing = The block face to attach
        // pos = The hit block position

        BlockHitResult hitResult = new BlockHitResult(hitVec, facing, pistonPos, false);
        itemCacheMap.get(Items.PISTON).switchTo();
        //InventoryHelper.setItemInHand(InventoryHelper.Type.PISTON.getSlot(), InventoryHelper.Type.PISTON.getItem());
        gameMode.useItemOn(localPlayer, InteractionHand.MAIN_HAND, hitResult);
    }
}
