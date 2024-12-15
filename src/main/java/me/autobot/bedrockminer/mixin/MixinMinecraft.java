package me.autobot.bedrockminer.mixin;

import me.autobot.bedrockminer.Setting;
import me.autobot.bedrockminer.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Minecraft.class)
public class MixinMinecraft {

    @Shadow
    @Nullable
    public ClientLevel level;

    @Shadow
    @Nullable
    public LocalPlayer player;

    @Shadow
    @Nullable
    public HitResult hitResult;


    @Shadow @Nullable public MultiPlayerGameMode gameMode;

    @Shadow @Final private ChatListener chatListener;

    @Inject(at = @At("HEAD"), method = "startUseItem")
    public void onStartUseItem(CallbackInfo ci) {
        // All Use base
        if (level == null || player == null || hitResult == null) {
            return;
        }
        switch (hitResult.getType()) {
            case MISS -> areaMode();
            case BLOCK -> singleMode();
        }
        //ci.cancel();
    }

    @Inject(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;swing(Lnet/minecraft/world/InteractionHand;)V"), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void onContinueAttack(boolean bl, CallbackInfo ci, BlockHitResult blockHitResult, BlockPos blockPos, Direction direction) {
        //System.out.println("ContinueAttack");
    }

    @Unique
    private void singleMode() {
        if (!Setting.ENABLE || Setting.MINEMODE != Setting.Mode.SINGLE) {
            return;
        }
        if (!player.getMainHandItem().isEmpty() && !(player.getMainHandItem().is(Items.DIAMOND_PICKAXE) || player.getMainHandItem().is(Items.NETHERITE_PICKAXE))) {
            return;
        }
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        if (blockHitResult == null) {
            return;
        }
        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!Setting.MINEBLOCKS.contains(block)) {
            return;
        }
        if (!Task.TASKS.isEmpty()) {
            return;
        }
        if (!Task.TASKS.containsKey(pos)) {
            Task.addTask(pos);
            chatListener.handleSystemMessage(Component.literal("Add Task"), true);
        } else {
            chatListener.handleSystemMessage(Component.literal("Processing Task"), true);
        }
    }

    @Unique
    private void areaMode() {
        if (!Setting.ENABLE || Setting.MINEMODE != Setting.Mode.AREA) {
            return;
        }
        //player.position().distanceToSqr()
    }
}
