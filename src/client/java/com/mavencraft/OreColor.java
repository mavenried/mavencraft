package com.mavencraft;

/**
 * Represents a highlight entry: a Minecraft block identifier and the ARGB
 * color to use when rendering its highlight box.
 */
public record OreColor(String blockId, int argb) {

    /** Convenience constructor accepting separate r, g, b, a components (0-255). */
    public static OreColor of(String blockId, int r, int g, int b, int a) {
        int argb = (a << 24) | (r << 16) | (g << 8) | b;
        return new OreColor(blockId, argb);
    }

    public float red()   { return ((argb >> 16) & 0xFF) / 255f; }
    public float green() { return ((argb >> 8)  & 0xFF) / 255f; }
    public float blue()  { return (argb          & 0xFF) / 255f; }
    public float alpha() { return ((argb >> 24) & 0xFF) / 255f; }
}
