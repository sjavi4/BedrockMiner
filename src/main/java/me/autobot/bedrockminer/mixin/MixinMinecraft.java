package me.autobot.bedrockminer.mixin;

import me.autobot.bedrockminer.Setting;
import me.autobot.bedrockminer.Task;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
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

    @Shadow @Final private ChatListener chatListener;

    @Inject(at = @At("HEAD"), method = "startUseItem", cancellable = true)
    public void onStartUseItem(CallbackInfo ci) {
        // All Use base
        if (level == null || player == null || hitResult == null) {
            return;
        }
        if (!Task.TASKS.isEmpty()) {
            ci.cancel();
            return;
        }
        switch (hitResult.getType()) {
            case MISS -> areaMode();
            case BLOCK -> {
                if (singleMode()) {
                    ci.cancel();
                }
            }
        }
    }

    @Unique
    private boolean singleMode() {
        if (!Setting.ENABLE || Setting.MINEMODE != Setting.Mode.SINGLE) {
            return false;
        }
        if (!player.getMainHandItem().isEmpty() && !(player.getMainHandItem().is(Items.DIAMOND_PICKAXE) || player.getMainHandItem().is(Items.NETHERITE_PICKAXE))) {
            return false;
        }
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        if (blockHitResult == null) {
            return false;
        }
        BlockPos pos = blockHitResult.getBlockPos();
        BlockState state = level.getBlockState(pos);
        Block block = state.getBlock();
        if (!Setting.MINEBLOCKS.contains(block)) {
            return false;
        }
        if (!Task.TASKS.isEmpty()) {
            return false;
        }
        if (!Task.TASKS.containsKey(pos)) {
            Task.addTask(pos);
            chatListener.handleSystemMessage(Component.literal("Add Task"), true);
            return true;
        } else {
            chatListener.handleSystemMessage(Component.literal("Processing Task"), true);
        }
        return false;
    }

    @Unique
    private void areaMode() {
        if (!Setting.ENABLE || Setting.MINEMODE != Setting.Mode.AREA) {
            return;
        }
        //player.position().distanceToSqr()
    }
}
