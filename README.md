# MavenCraft

A Fabric mod for Minecraft 26.3 with client overlays, server-side gameplay features, and optional Fantastic Wings integration:

- **X-Ray / Ore ESP** — highlights ores through walls with configurable colors and scan radius
- **Threat Radar** — a compass-ring HUD that shows nearby hostile mobs as directional chevrons
- **Trajectory Preview** — draws a real-time arc for bows, crossbows, ender pearls, snowballs, and splash/lingering potions
- **Breadcrumb Trail** — leaves connected waypoints along your route so you can find your way back
- **Sprint Flight Multiplier** — boosts horizontal flight acceleration while sprinting with Fantastic Wings
- **Fantastic Wings persistence** — restores saved wings after death or rejoining; drinking bat blood clears the saved type
- **Creeper explosion modes** — choose vanilla explosions, no block damage, or no damage at all

> **Personal-use mod.** Use responsibly and only on servers where client-side mods are permitted.

---

## Requirements

| Dependency    | Version        |
| ------------- | -------------- |
| Minecraft     | `26.3.x`       |
| Fabric Loader | `≥ 0.20.1`     |
| Fabric API    | `0.165.2+26.3` |
| Java          | `25`           |

---

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft `26.3.x`.
2. Drop `mavencraft-<version>.jar` into your `mods/` folder.
3. Launch the game — MavenCraft config files are created in your config directory as needed.
4. To use the sprint flight multiplier, also install Fantastic Wings for Minecraft `26.3.x`.

---

## Features

### Mod Menu Settings

Install [Mod Menu](https://modrinth.com/mod/modmenu) to open MavenCraft's settings screen from the Mods menu. It provides toggles for X-ray, radar, projectile preview, breadcrumbs, and individual ores, plus sliders for scan radius and the sprint flight multiplier. The scan-radius slider ranges from 16 to 256 blocks in 16-block steps, and the ore list scrolls.

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

### Breadcrumb Trail

Leaves small connected waypoints along your path. Markers are placed farther apart at higher speeds and closer together when your direction changes, making the trail useful on foot and while flying. The trail holds up to 1,024 markers and clears when disabled or when changing worlds.

| Command                            | Description                  |
| ---------------------------------- | ---------------------------- |
| `/mavencraft breadcrumbs enable`   | Start recording a trail      |
| `/mavencraft breadcrumbs disable`  | Disable and clear the trail  |
| `/mavencraft breadcrumbs toggle`   | Toggle the breadcrumb trail  |
| `/mavencraft breadcrumbs clear`    | Clear the current trail      |

**Keybind:** `B` — toggle breadcrumbs (rebindable in Controls settings, category _MavenCraft_). Disabling clears the current trail.

---

### Sprint Flight Multiplier

When Fantastic Wings is installed, sprinting with forward input increases horizontal flight acceleration. The multiplier leaves the mod's vertical acceleration unchanged, preserving its pitch-based lift.

MavenCraft also saves the current wing type and restores it after death or rejoining. Drinking Fantastic Wings bat blood clears the saved type. If wings are removed by another means, MavenCraft restores them after a two-second grace period.

### Creeper explosion mode

Operator commands, saved per world:

| Command | Behavior |
| --- | --- |
| `/mavencraft creepers vanilla` | Normal block and entity damage |
| `/mavencraft creepers no-block-damage` | Entity damage without terrain damage (default) |
| `/mavencraft creepers no-damage` | Creepers explode without damaging blocks or entities |
| `/mavencraft creepers status` | Show the current mode |

The client command forwards these options to the server. From a server console, use `/mavencraft-server creepers <mode>` with `vanilla`, `no-block-damage`, `no-damage`, or `status`.

| Command | Description |
| ------- | ----------- |
| `/mavencraft flight multiplier` | Show the current multiplier |
| `/mavencraft flight multiplier <0.1-10.0>` | Set the multiplier |

The default is `3.15`. The Mod Menu slider ranges from `0.10` to `10.00`. Settings persist to `<config-dir>/mavencraft-flight.properties`.

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
