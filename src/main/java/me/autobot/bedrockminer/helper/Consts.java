package me.autobot.bedrockminer.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.chat.ChatListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.HashMap;
import java.util.Map;

public class Consts {

    public static final Minecraft client = Minecraft.getInstance();
    public static Connection connection;
    public static MultiPlayerGameMode gameMode;
    public static ClientLevel level;
    public static LocalPlayer localPlayer;
    public static Player player;
    public static ChatListener chatListener;


    public static Map<Item, ItemCache> itemCacheMap = new HashMap<>();

    public static void sendMsg(String s, boolean b) {
        chatListener.handleSystemMessage(Component.literal(s), b);
    }
}
