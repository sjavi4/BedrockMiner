package me.autobot.bedrockminer;

import com.mojang.datafixers.util.Pair;
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
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
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
    public int retrying = 0;
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
        if (retrying > 1) {
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
                PlayerLookHelper.reset();
                onPlacePiston();
                onPlaceSlime();
                wait = 1;
            }
            case PLACE_SLIME -> {
                onPlaceSlime();
            }
            case PLACE_TORCH -> {
                onPlaceTorch();
            }
            case SLEEP -> {
                PlayerLookHelper.reset();
                onWaitPistonPush();
            }
            case REMOVE -> onBreakRedstoneSource();
//            case BREAK_BEDROCK -> {
//                finishCurrentState = true;
//            }
            case CLEANUP -> {
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
            client.getChatListener().handleSystemMessage(Component.literal("No Haste2!"), true);
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
    }

    public void onPlacePiston() {
        if (!blocks.containsKey(Blocks.PISTON)) {
            removeTask();
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        
        var pistonInfo = blocks.get(Blocks.PISTON);
        Direction pistonFacing = pistonInfo.getSecond().getValue(BlockStateProperties.FACING);
//        if (Direction.Plane.VERTICAL.test(pistonFacing)) {
//            PlayerLookHelper.set(pistonFacing);
//        } else {
//            PlayerLookHelper.set(pistonFacing.getOpposite());
//        }
        //System.out.println(pistonFacing);
        //setLook(player, pistonFacing);
        PlayerLookHelper.set(pistonFacing);
        setPistonBlock(player, gameMode, InventoryHelper.Type.PISTON, pistonInfo.getFirst(), pistonFacing);
        //wait = 1;
        finishCurrentState = true;
        //PlayerLookHelper.reset();
    }
    public void onPlaceSlime() {
        if (!blocks.containsKey(Blocks.SLIME_BLOCK)) {
            finishCurrentState = true;
            return;
        }
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        setBlock(player, gameMode, InventoryHelper.Type.SLIME, blocks.get(Blocks.SLIME_BLOCK).getFirst(), Direction.NORTH);
        //wait = 1;
        finishCurrentState = true;
        PlayerLookHelper.reset();
    }
    public void onPlaceTorch() {
        if (!blocks.containsKey(Blocks.REDSTONE_TORCH) && !blocks.containsKey(Blocks.REDSTONE_WALL_TORCH)) {
            removeTask();
            return;
        }

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;
        
        var torchInfo = blocks.get(Blocks.REDSTONE_TORCH);
        if (torchInfo != null) {
            PlayerLookHelper.set(0, -90);
            setTorchBlock(player, gameMode, torchInfo.getFirst());
            PlayerLookHelper.reset();
            //wait = 1;
            finishCurrentState = true;
            return;
        }
        torchInfo = blocks.get(Blocks.REDSTONE_WALL_TORCH);
        Direction torchFacing = torchInfo.getSecond().getValue(RedstoneWallTorchBlock.FACING);
        PlayerLookHelper.set(torchFacing);
        //setLook(player, torchFacing);
        setBlock(player, gameMode, InventoryHelper.Type.TORCH, torchInfo.getFirst(), torchFacing);

        PlayerLookHelper.reset();;
        finishCurrentState = true;
    }

    public void onWaitPistonPush() {
        wait = 1;
        finishCurrentState = true;
        InventoryHelper.setItemInHand(InventoryHelper.Type.PICKAXE.getSlot(), InventoryHelper.Type.PICKAXE.getItem());
    }

    public void onBreakRedstoneSource() {
        Minecraft client = Minecraft.getInstance();
        //Player player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;

//        if (blocks.containsKey(Blocks.SLIME_BLOCK)) {
//            gameMode.startDestroyBlock(blocks.get(Blocks.SLIME_BLOCK).getFirst(), Direction.UP);
//        }
        if (blocks.containsKey(Blocks.REDSTONE_TORCH)) {
            gameMode.startDestroyBlock(blocks.get(Blocks.REDSTONE_TORCH).getFirst(), Direction.UP);
        }
        if (blocks.containsKey(Blocks.REDSTONE_WALL_TORCH)) {
            gameMode.startDestroyBlock(blocks.get(Blocks.REDSTONE_WALL_TORCH).getFirst(), Direction.UP);
        }
        gameMode.startDestroyBlock(blocks.get(Blocks.PISTON).getFirst(), Direction.DOWN);
        onPlacePistonAgainstBedrock();
        PlayerLookHelper.reset();
        finishCurrentState = true;

        wait = Setting.CLEANUPDELAY;
        InventoryHelper.setItemInHand(InventoryHelper.Type.PICKAXE.getSlot(), InventoryHelper.Type.PICKAXE.getItem());
    }

    public void onPlacePistonAgainstBedrock() {
        var pistonInfo = blocks.get(Blocks.PISTON);
        BlockPos pistonPos = pistonInfo.getFirst();
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        MultiPlayerGameMode gameMode = client.gameMode;

        if (pistonPos.below().asLong() == bedrockPos.asLong()) {
            swapBlock(player, gameMode, InventoryHelper.Type.PISTON, pistonPos, Direction.DOWN);
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (pistonPos.relative(d).asLong() == bedrockPos.asLong()) {
                swapBlock(player, gameMode, InventoryHelper.Type.PISTON, pistonPos, d);
                return;
            }
        }
    }

    public void onCheckBedrockExist() {
        Minecraft client = Minecraft.getInstance();
        ClientLevel level = client.level;
        //client.gameMode.stopDestroyBlock();

        blocks.values().removeIf(pair -> level.getBlockState(pair.getFirst()).isAir());
        for (var entry : blocks.entrySet()) {
            //Block block = entry.getKey();
            var pair = entry.getValue();
            if (!level.getBlockState(pair.getFirst()).isAir()) {
                client.gameMode.startDestroyBlock(pair.getFirst(), Direction.UP);
                finishCurrentState = true;
            }
        }
//        if (level.getBlockState(bedrockPos).is(Blocks.BEDROCK)) {
//            retrying++;
//            state = State.START;
//            client.getChatListener().handleSystemMessage(Component.literal("Retrying Due to Fail."), false);
//            finishCurrentState = false;
//        } else {
//            finishCurrentState = true;
//        }
    }
    private void setTorchBlock(LocalPlayer player, MultiPlayerGameMode gameMode, BlockPos pos) {
        Direction facing = Direction.DOWN;
        var hitVec3d = pos.getCenter().relative(facing, 0.5F);
        var hitResult = new BlockHitResult(hitVec3d, facing.getOpposite(), pos.below(), false);
        InventoryHelper.setItemInHand(InventoryHelper.Type.TORCH.getSlot(), InventoryHelper.Type.TORCH.getItem());

        //ClientPacketListener connection = Minecraft.getInstance().getConnection();
        //connection.send(new ServerboundMovePlayerPacket.Rot(player.getViewYRot(1f), -90F, player.onGround(), player.horizontalCollision));
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
    }
    private void setBlock(LocalPlayer player, MultiPlayerGameMode gameMode, InventoryHelper.Type type, BlockPos pos, Direction facing) {
        var hitPos = pos.relative(facing.getOpposite());
        var hitVec3d = hitPos.getCenter().relative(facing, 0.5F);
        var hitResult = new BlockHitResult(hitVec3d, facing, pos, false);
        InventoryHelper.setItemInHand(type.getSlot(), type.getItem());
        if (Direction.Plane.VERTICAL.test(facing)) {
            PlayerLookHelper.set(facing);
        } else {
            PlayerLookHelper.set(facing.getOpposite());
        }
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
    }

    private void setPistonBlock(LocalPlayer player, MultiPlayerGameMode gameMode, InventoryHelper.Type type, BlockPos pos, Direction facing) {
        var hitVec3d = pos.getCenter().relative(facing.getOpposite(), 0.5F);
        var hitResult = new BlockHitResult(hitVec3d, facing, pos, false);
        InventoryHelper.setItemInHand(type.getSlot(), type.getItem());
//        if (Direction.Plane.VERTICAL.test(facing)) {
//            PlayerLookHelper.set(facing);
//        } else {
//            PlayerLookHelper.set(facing);
//        }
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
    }

    private void swapBlock(LocalPlayer player, MultiPlayerGameMode gameMode, InventoryHelper.Type type, BlockPos pos, Direction facing) {
        var hitVec3d = pos.getCenter().relative(facing.getOpposite(), 0.5F);
        var hitResult = new BlockHitResult(hitVec3d, facing, pos, false);
        InventoryHelper.setItemInHand(type.getSlot(), type.getItem());
        if (Direction.Plane.VERTICAL.test(facing)) {
            PlayerLookHelper.set(facing);
        } else {
            PlayerLookHelper.set(facing.getOpposite());
        }
        gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hitResult);
    }
    public enum State {
        FINISH(null), // Remove Task
        CLEANUP(FINISH), // Clear Blocks
        BREAK_BEDROCK(CLEANUP), // Place New Piston
        REMOVE(CLEANUP), // Remove Torch and piston
        SLEEP(REMOVE),
        PLACE_TORCH(SLEEP),
        PLACE_SLIME(PLACE_TORCH),

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
