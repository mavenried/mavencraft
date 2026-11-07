package com.mavencraft;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MavenCraftMod implements ModInitializer {

    public static final String MOD_ID = "mavencraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        CreeperBlockDamage.register();
        WingsPersistence.register();
        CommandRegistrationCallback.EVENT.register(CreeperBlockDamage::registerCommand);
        LOGGER.info("MavenCraft loaded.");
    }
}
