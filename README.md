# GreenGlow

Client-side Fabric visual mod for **Minecraft Java 1.21.4**.

## Visual effects

- Black Overworld sky with animated green aurora-style ribbons.
- Repeated 3D rectangular outlines around the player, with neon-green edges.
- Animated circular green wavefronts around rendered items and a splash-like ripple after landing.
- Original item models, tinting, and Resource Pack textures remain in use.
- No blue tint, player/entity highlighting, names, labels, extra HUD, or GUI.

The effects are a custom client-side rendering approximation. Their final appearance and compatibility with other rendering/resource-pack mods should be checked in Minecraft.

## Installation

1. Download the latest **GreenGlow** artifact from GitHub Actions.
2. Open the ZIP and copy only `GreenGlow-1.0.0.jar` into your Minecraft `mods` folder.
3. Do not install a file ending in `-sources.jar`; it contains source code and is not the mod.
4. Launch Fabric for Minecraft 1.21.4 with Java 21.

## Development setup

- Java 21
- Gradle (Fabric Loom)
- Minecraft 1.21.4
- Fabric Loader 0.16.10+
- Fabric API 0.119.2+1.21.4

## Build

Run:

```bash
gradle build
```

The built mod JAR will be in `build/libs/`. GitHub Actions builds the project on every push and uploads only the installable mod JAR.
