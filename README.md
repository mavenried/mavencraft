# MavenCraft

A client-side Fabric mod for Minecraft 1.26.x that bundles three utility features:

- **X-Ray / Ore ESP** — highlights ores through walls with configurable colors and scan radius
- **Threat Radar** — a compass-ring HUD that shows nearby hostile mobs as directional chevrons
- **Trajectory Preview** — draws a real-time arc for bows, crossbows, ender pearls, snowballs, and splash/lingering potions

> **Personal-use mod.** Use responsibly and only on servers where client-side mods are permitted.

---

## Requirements

| Dependency    | Version        |
| ------------- | -------------- |
| Minecraft     | `26.1.x`       |
| Fabric Loader | `≥ 0.18.0`     |
| Fabric API    | `0.144.3+26.1` |
| Java          | `25`           |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft `26.1.x`.
2. Drop `mavencraft-<version>.jar` into your `mods/` folder.
3. Launch the game — a `mavencraft.json` config file is created in your config directory on first run.

---

## Features

### X-Ray / Ore ESP

Draws wireframe boxes around ores visible within a configurable radius.

| Command                            | Description                              |
| ---------------------------------- | ---------------------------------------- |
| `/mavencraft xray toggle`          | Toggle the X-ray overlay on/off          |
| `/mavencraft xray enable <id>`     | Show a specific ore (e.g. `diamond`)     |
| `/mavencraft xray disable <id>`    | Hide a specific ore                      |
| `/mavencraft xray only <id>`       | Show only that ore, hide everything else |
| `/mavencraft xray all`             | Show all configured ores                 |
| `/mavencraft xray none`            | Hide all ores                            |
| `/mavencraft xray radius <16‑256>` | Set scan radius in blocks                |

**Keybind:** `Y` — toggle X-ray (rebindable in Controls settings, category _MavenCraft_).

Ore colors and the list of tracked blocks are configured in `MavenCraftConfig.java`. To add a modded ore, append an `OreColor.of(...)` entry to `ORE_COLORS`.

Settings (enabled state, radius, active ore set) persist to `<config-dir>/mavencraft.json`.

---

### Threat Radar

A circular HUD centered on screen. Hostile mobs within `range` blocks are shown as red chevrons pointing toward their direction; chevrons scale and fade with distance.

| Command                     | Description           |
| --------------------------- | --------------------- |
| `/mavencraft radar enable`  | Enable the radar HUD  |
| `/mavencraft radar disable` | Disable the radar HUD |

Tunable constants in `RadarManager`:

| Field        | Default | Description                |
| ------------ | ------- | -------------------------- |
| `range`      | `48.0`  | Detection radius in blocks |
| `maxThreats` | `5`     | Maximum chevrons drawn     |
| `smoothing`  | `0.18`  | Angle lerp factor per tick |

---

### Trajectory Preview

When the player holds a supported item, a white line traces the predicted arc and a red dot marks the impact point.

**Supported items:** Bow, Crossbow, Snowball, Ender Pearl, Splash/Lingering Potion, Experience Bottle.

| Command                   | Description                |
| ------------------------- | -------------------------- |
| `/mavencraft aim enable`  | Enable trajectory preview  |
| `/mavencraft aim disable` | Disable trajectory preview |

Bow arcs account for charge level. Crossbow arcs include player momentum. Other thrown items use a fixed initial speed.

---

## Building from Source

```bash
./gradlew build
```

The output jar is placed in `build/libs/`.

To run in a development environment:

```bash
./gradlew runClient
```

---

## Configuration File

`<minecraft-dir>/config/mavencraft.json` is written automatically and can be edited by hand:

```json
{
  "enabled": true,
  "radius": 64,
  "ores": ["minecraft:diamond_ore", "minecraft:deepslate_diamond_ore"]
}
```

---
