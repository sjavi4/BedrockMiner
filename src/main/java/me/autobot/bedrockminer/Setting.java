package me.autobot.bedrockminer;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

import java.util.HashSet;
import java.util.Set;

public class Setting {
    public static boolean ENABLE = false;
    public static Mode MINEMODE = Mode.SINGLE;
    public static int CLEANUPDELAY = 2;
    public static Set<Block> MINEBLOCKS = new HashSet<>() {{
        add(Blocks.BEDROCK);
        add(Blocks.END_PORTAL_FRAME);
    }};

    public enum Mode {
        SINGLE, AREA;
    }
}
