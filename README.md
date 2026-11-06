# GreenGlow

Client-side Fabric mod project for **Minecraft Java 1.21.4**.

## Planned visual features

- Repeated, equal-sized 3D rectangular outlines distributed through the world.
- Green outlines with a soft neon glow; transparent interiors.
- Green glow on the held sword and thumbs-up/hand visual shown in the reference.
- No labels, icons, extra HUD, or GUI.

## Development setup

- Java 21
- Gradle (Fabric Loom)
- Minecraft 1.21.4
- Fabric Loader 0.16.10+
- Fabric API 0.119.2+1.21.4

## Build

After opening the project in IntelliJ IDEA (or another Gradle IDE), run:

```bash
./gradlew build
```

On Windows:

```bat
gradlew.bat build
```

The built mod JAR will be in `build/libs/`.

## Note

This is the upload-ready source project scaffold. The rendering/effect implementation still needs to be added and tested in Minecraft before it can produce a working mod JAR.
