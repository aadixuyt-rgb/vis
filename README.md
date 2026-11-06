# GreenGlow

Client-side Fabric visual mod for **Minecraft Java 1.21.4**.

## Visual effects

- Black Overworld sky with animated green aurora-style ribbons.
- Repeated 3D rectangular outlines around the player, with neon-green edges.
- Animated circular green wavefronts around rendered items in the world, in the local player's hand, and in GUI item slots.
- Original item models, tinting, and Resource Pack textures remain in use.
- No blue tint, player/entity highlighting, names, labels, extra HUD, or GUI.

The effects are a custom client-side rendering approximation. Their final appearance and compatibility with other rendering/resource-pack mods should be checked in Minecraft.

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

The built mod JAR will be in `build/libs/`. GitHub Actions also builds the project on every push and uploads the JAR as an artifact.
