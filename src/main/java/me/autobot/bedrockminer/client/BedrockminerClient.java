package me.autobot.bedrockminer.client;

import net.fabricmc.api.ClientModInitializer;


public class BedrockminerClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        new Command();
    }
}
