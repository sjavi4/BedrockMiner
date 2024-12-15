package me.autobot.bedrockminer.mixin;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class MixinClientPacketListener {

    //@Inject(method = "handleBlockUpdate", at = @At(value = "INVOKE", shift = At.Shift., target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/util/thread/BlockableEventLoop;)V"))
    @Inject(method = "handleBlockUpdate", at = @At(value = "HEAD"))
    public void onBlockUpdate(ClientboundBlockUpdatePacket packet, CallbackInfo ci) {
        BlockPos pos = packet.getPos();
        BlockState state = packet.getBlockState();

//        for (Task task : Task.TASKS.values()) {
//            if (pos.asLong() == task.bedrockPos.asLong()) {
//                if (state.isAir()) {
//                    task.removeTask();
//                }
//            }
////            loop: for (var pair : task.blocks.values()) {
////                if (pair.getFirst().asLong() == pos.asLong()) {
////                    switch (task.state) {
////                        case PLACE_SLIME -> {
////                            if (state.is(Blocks.SLIME_BLOCK)) {
////                                task.finishCurrentState = true;
////                                break loop;
////                            }
////                        }
////                    }
////                }
////            }
////            loop: for (var pair : task.blocks.values()) {
////                if (pair.getFirst().asLong() == pos.asLong()) {
////                    switch (task.state) {
////                        case PLACE_PISTON -> {
////                            if (state.is(Blocks.PISTON) && !state.getValue(PistonBaseBlock.EXTENDED)) {
////                                task.finishCurrentState = true;
////                                break loop;
////                            }
////                        }
////                        case PLACE_SLIME -> {
////                            if (state.is(Blocks.SLIME_BLOCK)) {
////                                task.finishCurrentState = true;
////                                break loop;
////                            }
////                        }
////                        case PLACE_TORCH -> {
////                            if (state.is(Blocks.REDSTONE_TORCH) || state.is(Blocks.REDSTONE_WALL_TORCH)) {
////                                task.finishCurrentState = true;
////                                break loop;
////                            }
////                        }
////                        case REMOVE -> {
////                            //if (state.is(Blocks.PISTON_HEAD)) {
////                            if (state.is(Blocks.PISTON) && state.getValue(PistonBaseBlock.EXTENDED)) {
////                                task.finishCurrentState = true;
////                                break loop;
////                            }
////                        }
////                        case BREAK_BEDROCK -> {
////                            if (state.isAir()) {
////                                if (task.blocks.containsKey(Blocks.SLIME_BLOCK)) {
////                                    if (task.blocks.get(Blocks.SLIME_BLOCK).getFirst().asLong() == pos.asLong()) {
////                                        task.finishCurrentState = true;
////                                        break loop;
////                                    }
////                                }
////                                if (task.blocks.containsKey(Blocks.REDSTONE_TORCH)) {
////                                    if (task.blocks.get(Blocks.REDSTONE_TORCH).getFirst().asLong() == pos.asLong()) {
////                                        task.finishCurrentState = true;
////                                        break loop;
////                                    }
////                                }
////                                if (task.blocks.containsKey(Blocks.REDSTONE_WALL_TORCH)) {
////                                    if (task.blocks.get(Blocks.REDSTONE_WALL_TORCH).getFirst().asLong() == pos.asLong()) {
////                                        task.finishCurrentState = true;
////                                        break loop;
////                                    }
////                                }
////                                if (task.blocks.containsKey(Blocks.PISTON)) {
////                                    if (task.blocks.get(Blocks.PISTON).getFirst().asLong() == pos.asLong()) {
////                                        task.finishCurrentState = true;
////                                        break loop;
////                                    }
////                                }
////                            }
////                        }
////                    };
////                }
////            }
//
//
//        }
    }

}
