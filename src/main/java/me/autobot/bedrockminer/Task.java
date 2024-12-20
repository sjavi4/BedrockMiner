package me.autobot.bedrockminer;

import com.mojang.datafixers.util.Pair;
import me.autobot.bedrockminer.helper.BlockPlaceHelper;
import me.autobot.bedrockminer.helper.InventoryHelper;
import me.autobot.bedrockminer.helper.PlayerLookHelper;
import me.autobot.bedrockminer.helper.PositionFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class Task {
    public static Map<BlockPos, Task> TASKS = new ConcurrentHashMap<>();
    public State state = State.START;
    public final BlockPos bedrockPos;
    public boolean finishCurrentState = false;
    public int wait = 0;
    public Map<Block, Pair<BlockPos, BlockState>> blocks = new HashMap<>();
    int ticks = 0;
    public static Task addTask(BlockPos pos) {
        Task task = new Task(pos);
        PositionFinder.init();
        TASKS.put(pos, task);
        return task;
    }


    private Task(BlockPos blockPos) {
        this.bedrockPos = blockPos;
    }

    public void tick() {
        if (!Setting.ENABLE) {
            removeTask();
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
        TASKS.remove(bedrockPos);
    }
    public void onStart() {
        Minecraft client = Minecraft.getInstance();
        if (!client.gameMode.getPlayerMode().isSurvival()) {
            client.getChatListener().handleSystemMessage(Component.literal("Not Survival!"), true);
            removeTask();
            return;
        }
        // Instant mine
        InventoryHelper.Type pickaxe = InventoryHelper.Type.PICKAXE;
        if (pickaxe.getSlot() == -1 || pickaxe.getItem() == null) {
            client.getChatListener().handleSystemMessage(Component.literal("No Pickaxe!"), true);
            removeTask();
            return;
        }

        InventoryHelper.Type piston = InventoryHelper.Type.PISTON;
        InventoryHelper.Type torch = InventoryHelper.Type.TORCH;

        if (piston.getSlot() == -1 || piston.getItem() == null) {
            client.getChatListener().handleSystemMessage(Component.literal("No Piston!"), true);
            removeTask();
            return;
        }
        if (torch.getSlot() == -1 || torch.getItem() == null) {
            client.getChatListener().handleSystemMessage(Component.literal("No Torch!"), true);
            removeTask();
            return;
        }
        if (piston.getItem().getCount() < 2) {
            client.getChatListener().handleSystemMessage(Component.literal("Piston < 2!"), true);
            removeTask();
            return;
        }
        if (torch.getItem().getCount() < 1) {
            client.getChatListener().handleSystemMessage(Component.literal("Torch < 1!"), true);
            removeTask();
            return;
        }

        if (client.player.getMainHandItem() != pickaxe.getItem()) {
            InventoryHelper.setItemInHand(pickaxe.getSlot(), pickaxe.getItem());
            wait = 1;
            return;
        }
        float speed = client.player.getDestroySpeed(Blocks.PISTON.defaultBlockState());
        if (speed <= 10F) {
            client.getChatListener().handleSystemMessage(Component.literal("No Instant Mine!"), true);
            removeTask();
            return;
        }

        finishCurrentState = true;
    }

    public void onPreparePlace() {
        Minecraft client = Minecraft.getInstance();

        Pair<BlockPos, BlockState> pistonInfo = PositionFinder.findPistonPos(bedrockPos);
        if (pistonInfo == null) {
            client.getChatListener().handleSystemMessage(Component.literal("Cannot Place Piston!"), true);
            removeTask();
            return;
        }
        Pair<BlockPos, BlockState> slimeInfo = null;
        Pair<BlockPos, BlockState> torchInfo = PositionFinder.findTorchPos(pistonInfo.getFirst(), pistonInfo.getSecond());
        if (torchInfo == null) {
            slimeInfo = PositionFinder.findSlimePos(pistonInfo.getFirst(), pistonInfo.getSecond());
            if (slimeInfo == null) {
                client.getChatListener().handleSystemMessage(Component.literal("Cannot Place Slime!"), true);
                removeTask();
                return;
            }

            torchInfo = PositionFinder.findTorchAttachToSlime(slimeInfo.getFirst());
        }
        if (torchInfo == null) {
            client.getChatListener().handleSystemMessage(Component.literal("Cannot Place Torch!"), true);
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

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        var pistonInfo = blocks.get(Blocks.PISTON);
        BlockPlaceHelper.placePiston(player, client.gameMode, pistonInfo.getFirst(), pistonInfo.getSecond());

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
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        setBlock(player, client.gameMode, InventoryHelper.Type.SLIME, blocks.get(Blocks.SLIME_BLOCK).getFirst(), Direction.NORTH);
        finishCurrentState = true;
    }
    public void onPlaceTorch() {
        if (!blocks.containsKey(Blocks.REDSTONE_TORCH) && !blocks.containsKey(Blocks.REDSTONE_WALL_TORCH)) {
            removeTask();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        
        var torchInfo = blocks.containsKey(Blocks.REDSTONE_TORCH) ? blocks.get(Blocks.REDSTONE_TORCH) : blocks.get(Blocks.REDSTONE_WALL_TORCH);
        BlockPlaceHelper.placeTorch(player, gameMode, torchInfo.getFirst(), torchInfo.getSecond());
        finishCurrentState = true;
    }

    public void onWaitPistonPush() {
        wait = Setting.PISTON_PUSH_DELAY;
        finishCurrentState = true;
        InventoryHelper.setItemInHand(InventoryHelper.Type.PICKAXE.getSlot(), InventoryHelper.Type.PICKAXE.getItem());
    }

    public void onBreakRedstoneSource() {
        Minecraft client = Minecraft.getInstance();
        MultiPlayerGameMode gameMode = client.gameMode;

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
        InventoryHelper.setItemInHand(InventoryHelper.Type.PICKAXE.getSlot(), InventoryHelper.Type.PICKAXE.getItem());
    }
    public void onPlacePistonAgainstBedrock() {
        var pistonInfo = blocks.get(Blocks.PISTON);
        BlockPos pistonPos = pistonInfo.getFirst();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;

        BlockPlaceHelper.placePiston(player, gameMode, pistonPos, pistonInfo.getSecond());
    }

    public void onCheckBedrockExist() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;

        blocks.values().removeIf(pair -> level.getBlockState(pair.getFirst()).isAir());
        for (var entry : blocks.entrySet()) {
            var pair = entry.getValue();
            if (!level.getBlockState(pair.getFirst()).isAir()) {
                client.gameMode.startDestroyBlock(pair.getFirst(), Direction.UP);
                finishCurrentState = true;
            }
        }
    }
    private void setBlock(LocalPlayer player, MultiPlayerGameMode gameMode, InventoryHelper.Type type, BlockPos pos, Direction facing) {
        var hitPos = pos.relative(facing.getOpposite());
        var hitVec3d = hitPos.getCenter().relative(facing, 0.5F);
        var hitResult = new BlockHitResult(hitVec3d, facing, pos, false);
        InventoryHelper.setItemInHand(type.getSlot(), type.getItem());
//        if (Direction.Plane.VERTICAL.test(facing)) {
//            PlayerLookHelper.set(facing);
//        } else {
//            PlayerLookHelper.set(facing.getOpposite());
//        }
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
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
