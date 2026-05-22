package com.mavencraft;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import net.fabricmc.loader.api.FabricLoader;

public class MavenCraftState {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("mavencraft.json");

    public boolean enabled = true;

    public int radius = 64;

    public Set<String> ores = new HashSet<>();

    public static MavenCraftState load() {

        try {

            if (!Files.exists(FILE)) {
                return new MavenCraftState();
            }

            return GSON.fromJson(Files.readString(FILE), MavenCraftState.class);

        } catch (Exception e) {

            e.printStackTrace();

            return new MavenCraftState();
        }
    }

    public void save() {

        try {

            Files.writeString(FILE, GSON.toJson(this));

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}
