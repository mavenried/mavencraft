package com.mavencraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.fabricmc.loader.api.FabricLoader;

/** Shared setting used by the Fantastic Wings mixin and client controls. */
public final class FlightMultiplierConfig {

    public static final float DEFAULT_MULTIPLIER = 3.15F;
    public static final float MIN_MULTIPLIER = 0.1F;
    public static final float MAX_MULTIPLIER = 10.0F;

    private static final Logger LOGGER = LoggerFactory.getLogger("MavenCraft");
    private static final Path FILE = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("mavencraft-flight.properties");

    private static volatile float multiplier = load();

    private FlightMultiplierConfig() {}

    public static float getMultiplier() {
        return multiplier;
    }

    public static synchronized void setMultiplier(float value) {
        if (!Float.isFinite(value) || value < MIN_MULTIPLIER || value > MAX_MULTIPLIER) {
            throw new IllegalArgumentException("Flight multiplier must be between 0.1 and 10.0");
        }

        multiplier = value;
        save();
    }

    private static float load() {
        if (!Files.isRegularFile(FILE)) return DEFAULT_MULTIPLIER;

        Properties properties = new Properties();
        try (var reader = Files.newBufferedReader(FILE)) {
            properties.load(reader);
            float value = Float.parseFloat(properties.getProperty("multiplier", ""));
            if (Float.isFinite(value) && value >= MIN_MULTIPLIER && value <= MAX_MULTIPLIER) {
                return value;
            }
            LOGGER.warn("Ignoring invalid flight multiplier in {}", FILE);
        } catch (IOException | NumberFormatException e) {
            LOGGER.warn("Could not read flight multiplier from {}", FILE, e);
        }
        return DEFAULT_MULTIPLIER;
    }

    private static void save() {
        Properties properties = new Properties();
        properties.setProperty("multiplier", Float.toString(multiplier));
        try {
            Files.createDirectories(FILE.getParent());
            try (var writer = Files.newBufferedWriter(FILE)) {
                properties.store(writer, "MavenCraft Fantastic Wings flight settings");
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save flight multiplier to {}", FILE, e);
        }
    }
}
