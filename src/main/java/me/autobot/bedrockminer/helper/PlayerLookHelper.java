package me.autobot.bedrockminer.helper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public class PlayerLookHelper {
    private static boolean modifyYaw = false;
    private static boolean modifyPitch = false;
    private static float yaw = 0F;
    private static float pitch = 0F;

    public static float onModifyLookYaw(float yaw) {
        return modifyYaw ? PlayerLookHelper.yaw : yaw;
    }

    public static float onModifyLookPitch(float pitch) {
        return modifyPitch ? PlayerLookHelper.pitch : pitch;
    }

    private static ServerboundMovePlayerPacket getLookPacket(LocalPlayer player) {
        var yaw = modifyYaw ? PlayerLookHelper.yaw : player.getViewYRot(1f);
        var pitch = modifyPitch ? PlayerLookHelper.pitch : player.getXRot();
        return new ServerboundMovePlayerPacket.Rot(yaw, pitch, player.onGround(), player.horizontalCollision);
    }

    public static void set(float yaw, float pitch) {
        PlayerLookHelper.modifyYaw = true;
        PlayerLookHelper.yaw = yaw;
        PlayerLookHelper.modifyPitch = true;
        PlayerLookHelper.pitch = pitch;
    }

    public static void set(Direction facing) {
        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientPacketListener networkHandler = client.getConnection();
        float yaw = switch (facing) {
            case SOUTH -> 180F;
            case EAST -> 90F;
            case NORTH -> 0F;
            case WEST -> -90F;
            default -> player == null ? 0F : player.getViewYRot(1f);
        };
        float pitch = switch (facing) {
            case UP -> 90F;
            case DOWN -> -90F;
            default -> 0F;
        };
        set(yaw, pitch);
        if (networkHandler != null && player != null) {
            networkHandler.send(getLookPacket(player));
        }
    }

    public static void reset() {
        modifyYaw = false;
        yaw = 0F;
        modifyPitch = false;
        pitch = 0F;

        Minecraft client = Minecraft.getInstance();
        LocalPlayer player = client.player;
        ClientPacketListener networkHandler = client.getConnection();
        if (networkHandler != null && player != null) {
            networkHandler.send(getLookPacket(player));
        }
    }

    public static boolean isModify() {
        return modifyYaw || modifyPitch;
    }
}
