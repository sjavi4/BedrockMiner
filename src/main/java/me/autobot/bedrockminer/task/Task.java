package me.autobot.bedrockminer.task;

import com.mojang.datafixers.util.Pair;
import me.autobot.bedrockminer.Setting;
import me.autobot.bedrockminer.helper.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static me.autobot.bedrockminer.helper.Consts.*;

public class Task {
    public static Map<BlockPos, Task> TASKS = new LinkedHashMap<>();
    public State state = State.START;
    public final BlockPos bedrockPos;
    public boolean finishCurrentState = false;
    public int wait = 0;
    public Map<Block, Pair<BlockPos, BlockState>> blocks = new HashMap<>();
    int ticks = 0;
    public boolean remove = false;
    public static void addTask(BlockPos pos) {
        Task task = new Task(pos);
        TASKS.put(pos, task);
    }


    private Task(BlockPos blockPos) {
        this.bedrockPos = blockPos;
    }

    public void tick() {
        if (!Setting.ENABLE) {
            removeTask();
            return;
        }
        if (remove) {
            return;
        }
        if (ticks > 20) {
            removeTask();
            return;
        }
        if (wait > 0) {
            wait--;
            return;
        }
        if (finishCurrentState) {
            state = state.getNext();
            finishCurrentState = false;
        }
        ticks++;
        switch (state) {
            case START -> onStart();
            case PREPARE -> onPreparePlace();
            case PLACE_PISTON -> {
                onPlacePiston();
                onPlaceSlime();
            }
            case PLACE_TORCH -> onPlaceTorch();
            case SLEEP -> onWaitPistonPush();
            case REMOVE -> onBreakRedstoneSource();
            case CLEANUP -> {
                PlayerLookHelper.reset();
                onCheckBedrockExist();
            }
            case FINISH -> removeTask();
        }
    }
    public void removeTask() {
        remove = true;
    }
    public void onStart() {
        if (!gameMode.getPlayerMode().isSurvival()) {
            sendMsg("Not Survival!", true);
            removeTask();
            return;
        }

        itemCacheMap.get(Items.DIAMOND_PICKAXE).switchTo();
//        if (player.getMainHandItem() != pickaxe.getItem()) {
//            InventoryHelper.setItemInHand(pickaxe.getSlot(), pickaxe.getItem());
//            wait = 1;
//            return;
//        }
        float speed = player.getDestroySpeed(Blocks.PISTON.defaultBlockState());
        if (speed <= 10F) {
            sendMsg("No Instant Mine!", true);
            removeTask();
            return;
        }

        finishCurrentState = true;
    }

    public void onPreparePlace() {
        Pair<BlockPos, BlockState> pistonInfo = PositionFinder.findPistonPos(bedrockPos);
        if (pistonInfo == null) {
            sendMsg("Cannot Place Piston!", true);
            removeTask();
            return;
        }
        Pair<BlockPos, BlockState> slimeInfo = null;
        Pair<BlockPos, BlockState> torchInfo = PositionFinder.findTorchPos(pistonInfo.getFirst(), pistonInfo.getSecond());
        if (torchInfo == null) {
            slimeInfo = PositionFinder.findSlimePos(pistonInfo.getFirst(), pistonInfo.getSecond());
            if (slimeInfo == null) {
                sendMsg("Cannot Place Slime!", true);
                removeTask();
                return;
            }

            torchInfo = PositionFinder.findTorchAttachToSlime(slimeInfo.getFirst());
        }
        if (torchInfo == null) {
            sendMsg("Cannot Place Torch!", true);
            removeTask();
            return;
        }

        if (slimeInfo != null) {
            // Place Slime
            blocks.put(Blocks.SLIME_BLOCK, slimeInfo);
        }

        blocks.put(Blocks.PISTON, pistonInfo);
        blocks.put(torchInfo.getSecond().getBlock(), torchInfo);
        finishCurrentState = true;
        Direction facing = pistonInfo.getSecond().getValue(BlockStateProperties.FACING);
        PlayerLookHelper.set(facing);
        if (!Direction.Plane.VERTICAL.test(facing)) {
            wait = Setting.PISTON_PLACE_DELAY;
        }
    }

    public void onPlacePiston() {
        if (!blocks.containsKey(Blocks.PISTON)) {
            removeTask();
            return;
        }
        var pistonInfo = blocks.get(Blocks.PISTON);
        BlockPlaceHelper.placePiston(pistonInfo.getFirst(), pistonInfo.getSecond());

        finishCurrentState = true;
        BlockPos pistonPos = blocks.get(Blocks.PISTON).getFirst();
        BlockState pistonState = blocks.get(Blocks.PISTON).getSecond();
        if (pistonPos.below().asLong() == bedrockPos.asLong()) {
            blocks.put(Blocks.PISTON, Pair.of(pistonPos, pistonState.setValue(BlockStateProperties.FACING, Direction.DOWN)));
        } else {
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (pistonPos.relative(d).asLong() == bedrockPos.asLong()) {
                    blocks.put(Blocks.PISTON, Pair.of(pistonPos, pistonState.setValue(BlockStateProperties.FACING, d)));
                    break;
                }
            }
        }
        Direction facing = blocks.get(Blocks.PISTON).getSecond().getValue(BlockStateProperties.FACING);
        PlayerLookHelper.set(facing);
        if (!Direction.Plane.VERTICAL.test(facing)) {
            wait = Setting.PISTON_PLACE_DELAY;
        }
    }
    public void onPlaceSlime() {
        if (!blocks.containsKey(Blocks.SLIME_BLOCK)) {
            finishCurrentState = true;
            return;
        }
        setBlock(Items.SLIME_BLOCK, blocks.get(Blocks.SLIME_BLOCK).getFirst(), Direction.NORTH);
        finishCurrentState = true;
    }
    public void onPlaceTorch() {
        if (!blocks.containsKey(Blocks.REDSTONE_TORCH) && !blocks.containsKey(Blocks.REDSTONE_WALL_TORCH)) {
            removeTask();
            return;
        }
        
        var torchInfo = blocks.containsKey(Blocks.REDSTONE_TORCH) ? blocks.get(Blocks.REDSTONE_TORCH) : blocks.get(Blocks.REDSTONE_WALL_TORCH);
        BlockPlaceHelper.placeTorch(torchInfo.getFirst(), torchInfo.getSecond());
        finishCurrentState = true;
    }

    public void onWaitPistonPush() {
        wait = Setting.PISTON_PUSH_DELAY;
        finishCurrentState = true;
        itemCacheMap.get(Items.DIAMOND_PICKAXE).switchTo();
        //InventoryHelper.setItemInHand(InventoryHelper.Type.PICKAXE.getSlot(), InventoryHelper.Type.PICKAXE.getItem());
    }

    public void onBreakRedstoneSource() {
        if (blocks.containsKey(Blocks.SLIME_BLOCK)) {
            gameMode.startDestroyBlock(blocks.get(Blocks.SLIME_BLOCK).getFirst(), Direction.UP);
        } else if (blocks.containsKey(Blocks.REDSTONE_TORCH)) {
            gameMode.startDestroyBlock(blocks.get(Blocks.REDSTONE_TORCH).getFirst(), Direction.UP);
        }
        if (blocks.containsKey(Blocks.REDSTONE_WALL_TORCH)) {
            gameMode.startDestroyBlock(blocks.get(Blocks.REDSTONE_WALL_TORCH).getFirst(), Direction.UP);
        }

        gameMode.startDestroyBlock(blocks.get(Blocks.PISTON).getFirst(), Direction.UP);
        onPlacePistonAgainstBedrock();
        finishCurrentState = true;

        wait = Setting.CLEANUP_DELAY;
        itemCacheMap.get(Items.DIAMOND_PICKAXE).switchTo();
        //InventoryHelper.setItemInHand(InventoryHelper.Type.PICKAXE.getSlot(), InventoryHelper.Type.PICKAXE.getItem());
    }
    public void onPlacePistonAgainstBedrock() {
        var pistonInfo = blocks.get(Blocks.PISTON);
        BlockPos pistonPos = pistonInfo.getFirst();
        BlockPlaceHelper.placePiston(pistonPos, pistonInfo.getSecond());
    }

    public void onCheckBedrockExist() {
        blocks.values().removeIf(pair -> level.getBlockState(pair.getFirst()).isAir());
        for (var entry : blocks.entrySet()) {
            var pair = entry.getValue();
            if (!level.getBlockState(pair.getFirst()).isAir()) {
                gameMode.startDestroyBlock(pair.getFirst(), Direction.UP);
                finishCurrentState = true;
            }
        }
    }
    private void setBlock(Item item, BlockPos pos, Direction facing) {
        var hitPos = pos.relative(facing.getOpposite());
        var hitVec3d = hitPos.getCenter().relative(facing, 0.5F);
        var hitResult = new BlockHitResult(hitVec3d, facing, pos, false);
        ItemCache cache = itemCacheMap.get(item);
        if (cache == null) {
            removeTask();
            return;
        }
        cache.switchTo();
        gameMode.useItemOn(localPlayer, InteractionHand.MAIN_HAND, hitResult);
    }

    public enum State {
        FINISH(null), // Remove Task
        CLEANUP(FINISH), // Clear Blocks
        REMOVE(CLEANUP), // Remove Torch and piston
        SLEEP(REMOVE),
        PLACE_TORCH(SLEEP),

        PLACE_PISTON(PLACE_TORCH),
        PREPARE(PLACE_PISTON), // Place Piston or Slime
        START(PREPARE); // Register Task, check conditions

        private final State next;
        State(State next) {
            this.next = next;
        }

        public State getNext() {
            return next;
        }
    }
}
