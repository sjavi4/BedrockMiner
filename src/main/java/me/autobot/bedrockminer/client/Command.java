package me.autobot.bedrockminer.client;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import me.autobot.bedrockminer.Setting;
import me.autobot.bedrockminer.Task;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.commands.arguments.blocks.BlockStateArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.Block;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;
public class Command {
    public Command() {
        ClientCommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess) -> {
            LiteralCommandNode<FabricClientCommandSource> rootNode = literal("bedrockminer")
                    .executes(context -> {
                        final boolean b = Setting.ENABLE = !Setting.ENABLE;
                        context.getSource().sendFeedback(Component.literal("Bedrock Miner is " + (b ? "enabled" : "disabled")));
                        return 1;
                    })
                    .build();

            dispatcher.getRoot().addChild(rootNode);

//            LiteralCommandNode<FabricClientCommandSource> mineModeNode = literal("mode")
//                    .then(literal("single").executes(context -> {
//                        Setting.MINEMODE = Setting.Mode.SINGLE;
//                        context.getSource().sendFeedback(Component.literal("Set Mode to single"));
//                        context.getSource().sendFeedback(Component.literal("Destroy by targeting the block and press use with hand"));
//                        return 1;
//                    }))
//                    .then(literal("area").executes(context -> {
//                        Setting.MINEMODE = Setting.Mode.AREA;
//                        context.getSource().sendFeedback(Component.literal("Set Mode to area"));
//                        context.getSource().sendFeedback(Component.literal("Destroy nearby blocks and hold use with hand"));
//                        return 1;
//                    }))
//                    .build();
            LiteralCommandNode<FabricClientCommandSource> clearTaskNode = literal("clearTask").executes(context -> {
                for (Task t : Task.TASKS.values()) {
                    t.removeTask();
                }
                return 1;
            }).build();

            LiteralCommandNode<FabricClientCommandSource> cleanupDelayNode = literal("cleanupDelay")
                    .then(argument("delay", IntegerArgumentType.integer(1))
                            .suggests((context, builder) -> {
                                builder.suggest(Setting.CLEANUPDELAY);
                                return builder.buildFuture();
                            })
                            .executes(context -> {
                                Setting.CLEANUPDELAY = IntegerArgumentType.getInteger(context, "delay");
                                return 1;
                            }))
                    .build();

            LiteralCommandNode<FabricClientCommandSource> mineBlockListNode = literal("list")
                    .then(literal("add").then(argument("block", BlockStateArgument.block(registryAccess))
                                    .executes(context -> {
                                        BlockInput input = context.getArgument("block", BlockInput.class);
                                        Block block = input.getState().getBlock();
                                        Setting.MINEBLOCKS.add(block);
                                        context.getSource().sendFeedback(Component.literal("Added " + BuiltInRegistries.BLOCK.getKey(block).getPath()));
                                        return 1;
                                    })))
                    .then(literal("del").then(argument("block", BlockStateArgument.block(registryAccess))
                                    .suggests((context, builder) -> {
                                        for (Block b : Setting.MINEBLOCKS) {
                                            builder.suggest(BuiltInRegistries.BLOCK.getKey(b).getPath());
                                        }
                                        return builder.buildFuture();
                                    })
                                    .executes(context -> {
                                        BlockInput input = context.getArgument("block", BlockInput.class);
                                        Block block = input.getState().getBlock();
                                        Setting.MINEBLOCKS.remove(block);
                                        context.getSource().sendFeedback(Component.literal("Removed " + BuiltInRegistries.BLOCK.getKey(block).getPath()));
                                        return 1;
                                    })))
                    .build();






            //rootNode.addChild(mineModeNode);
            rootNode.addChild(mineBlockListNode);
            rootNode.addChild(clearTaskNode);
            rootNode.addChild(cleanupDelayNode);

        }));
    }
}
