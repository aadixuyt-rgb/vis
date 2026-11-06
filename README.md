# GreenGlow

Client-side Fabric mod project for **Minecraft Java 1.21.4**.

## Visual effect

- Repeated 3D rectangular outlines distributed around the player.
- Neon-green edges with a subtle green outer pass.
- Transparent interiors.
- No blue tint, player highlighting, names, labels, extra HUD, or GUI.

The current implementation is an initial rendering pass. Check it in Minecraft and adjust the spacing, size, and glow strength based on the result.

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
