# OrbZones

OrbZones is a Hytale server mod for protecting builds and defining named areas
in your world. Select an area in-game, give it a name, and control whether
players can break, place, or damage blocks inside it. Other mods can use its
Java API to query zones and react when players enter or leave.

- [Install the JAR](#installation)
- [Create your first zone](#creating-a-zone)
- [Commands](#commands) and [permissions](#permissions)
- [Use OrbZones in another mod](#use-orbzones-in-another-mod)
- [Build from source](#building-from-source)

## Features

- Persistent named zones containing one or more cuboid regions
- Break, place, and block-damage protection
- Per-action protection settings
- Temporary player-only 3D zone outlines
- Enter and leave callbacks for other Java plugins
- Support for overlapping zones
- Atomic JSON storage

## Installation

### Install the JAR

1. Download `OrbZones-<version>.jar` from the assets on the
   [GitHub Releases page](https://github.com/orbis-sh/OrbZones/releases).
   Choose the JAR asset, rather than a source-code ZIP.
2. Stop your Hytale server.
3. Place the JAR in your server's `mods/` directory. Create the directory if
   it does not exist. When updating, replace the old OrbZones JAR so only one
   version remains installed.
4. Start the server. The server log should contain `Loaded <count> zones`.
5. Join with the `orbzones.admin` permission and run `/zone list` to check
   that OrbZones is available.

```text
your-server/
└── mods/
    └── OrbZones-<version>.jar
```

OrbZones runs on the server; players do not need to install it on their clients.
Use Java 25 and a Hytale server version compatible with your chosen release.
No build tools are needed to install a released JAR.

## Creating a zone

Selection points use the block containing your feet. Both corners are
inclusive.

1. Stand or fly at one corner and run `/zone pos1`.
2. Move to the diagonally opposite corner and run `/zone pos2`.
3. Optionally run `/zone height <blocks>` to set an exact vertical size.
4. Run `/zone create <name>`.

Example:

```text
/zone pos1
/zone pos2
/zone height 10
/zone create StaticBuilding
/zone show StaticBuilding
```

`/zone height` preserves the selected horizontal footprint, starts at the
lower selected Y coordinate, and extends upward. For example, a height of `10`
covers exactly 10 blocks.

Each `/zone pos1`, `/zone pos2`, and `/zone height` command automatically
refreshes the player-only selection outline. It remains visible until the zone
is created. A single selected corner is shown as a one-block marker; the
complete selection is shown as a cuboid.

Include the foundation, roof, and any enclosed air that players must not
modify. Because a player standing on a floor occupies the air block above it,
move below the foundation when selecting the lower corner.

New zones protect breaking, placement, and block damage by default.

Zone names can contain 1–32 letters, numbers, underscores, or hyphens, and are
case-insensitive. For example, `Spawn` and `spawn` refer to the same zone.

To add another cuboid to the same logical zone, select two new corners and run
`/zone create <name>` again with the same name. Regions in one zone share
protection settings and enter/leave callbacks. Regions belonging to the same
zone cannot overlap.

## Commands

| Command | Description |
| --- | --- |
| `/zone pos1` | Set the first selection corner at your feet |
| `/zone pos2` | Set the second selection corner at your feet |
| `/zone height <blocks>` | Set the current selection's exact height from its lower Y coordinate |
| `/zone create <name>` | Create a zone or add a region to an existing zone |
| `/zone delete <name>` | Permanently delete a zone and all of its regions |
| `/zone list` | List configured zone names |
| `/zone info <name>` | Show bounds, world, and protection settings |
| `/zone show <name>` | Display a translucent green 3D box for 30 seconds |
| `/zone protect <name> <action> <true\|false>` | Change a protection setting |

Valid protection actions are `break`, `place`, and `damage`.

```text
/zone protect StaticBuilding break true
/zone protect StaticBuilding place true
/zone protect StaticBuilding damage true
```

The visualization has a green wireframe and low-opacity green fill. It is
client-side, visible only to the player who runs the command, and does not
place blocks.

## Permissions

| Permission | Effect |
| --- | --- |
| `orbzones.admin` | Allows all `/zone` commands |
| `orbzones.bypass` | Allows modifying blocks inside protected zones |

Server operators with the global `*` permission also have access.
The bypass permission also applies to operators with `*`, so test protection
with a player who does not have bypass access.

## Persistence

Zones are stored as `zones.json` in the plugin data directory. Changes made by
commands are written immediately using a temporary file and atomic
replacement.

Stop the server before editing `zones.json` manually. Invalid or incomplete
data prevents OrbZones from loading rather than silently disabling
protection.

## Use OrbZones in another mod

Install the OrbZones JAR on the server alongside your own mod. Use it as a
compile-time dependency in your project, and declare the runtime dependency
so Hytale loads OrbZones before your mod. Keep OrbZones out of your mod's
bundled/shaded JAR.

### 1. Add the compile-time dependency

Use the same OrbZones version for development and on the server. The examples
below use `1.0.0`; replace it with the version you downloaded.

**Maven:** install the downloaded JAR into your local Maven repository:

```sh
mvn install:install-file -Dfile=OrbZones-1.0.0.jar -DgroupId=sh.orbis -DartifactId=OrbZones -Dversion=1.0.0 -Dpackaging=jar
```

Then add this dependency to your mod's `pom.xml`:

```xml
<dependency>
    <groupId>sh.orbis</groupId>
    <artifactId>OrbZones</artifactId>
    <version>1.0.0</version>
    <scope>provided</scope>
</dependency>
```

This uses your local Maven repository; repeat the install step on other build
machines, including CI, that need to compile your mod.

**Gradle (Kotlin DSL):** copy the downloaded JAR into your project's `libs/`
directory and add it to `build.gradle.kts`:

```kotlin
dependencies {
    compileOnly(files("libs/OrbZones-1.0.0.jar"))
}
```

Your project also needs its normal Hytale Server API dependency and Java 25.

### 2. Declare the runtime dependency

Merge this entry into your mod's `manifest.json`, keeping any existing
dependencies:

```json
{
  "Dependencies": {
    "Orbis:OrbZones": "*"
  }
}
```

### 3. Listen for players entering and leaving

Add the following members to your `JavaPlugin` subclass, merging the lifecycle
methods with any existing setup and shutdown logic:

```java
import com.hypixel.hytale.server.core.Message;
import sh.orbis.orbzones.OrbZonesPlugin;
import sh.orbis.orbzones.api.ZoneListener;
import sh.orbis.orbzones.api.ZoneListenerRegistration;
import sh.orbis.orbzones.api.ZoneTransitionEvent;

private ZoneListenerRegistration zoneListener;

@Override
protected void setup() {
    zoneListener = OrbZonesPlugin.get()
            .getZoneManager()
            .registerListener(new ZoneListener() {
                @Override
                public void onEnter(ZoneTransitionEvent event) {
                    event.player().sendMessage(
                            Message.raw("Entered " + event.zone().name()));
                }

                @Override
                public void onLeave(ZoneTransitionEvent event) {
                    event.player().sendMessage(
                            Message.raw("Left " + event.zone().name()));
                }
            });
}

@Override
protected void shutdown() {
    if (zoneListener != null) {
        zoneListener.close();
    }
}
```

Callbacks run on the world thread. Moving between regions of the same logical
zone without leaving the zone does not create another callback. A player can
enter or leave multiple differently named zones at once when zones overlap.
Disconnecting while inside a zone also produces a matching leave callback.

### Querying zones

```java
import sh.orbis.orbzones.OrbZonesPlugin;
import sh.orbis.orbzones.api.BlockPosition;
import sh.orbis.orbzones.api.ZoneAction;

// Inside a method; use your world's name and the block coordinates to check.
var manager = OrbZonesPlugin.get().getZoneManager();
var position = new BlockPosition(100, 64, 100);
var zones = manager.findContaining(worldName, position);
var zone = manager.getByName("StaticBuilding"); // Optional<Zone>
boolean protectsBreaking = manager.isProtected(worldName, position, ZoneAction.BREAK);
```

`findContaining` returns every matching zone. Block modification is denied
when any matching zone protects that action. `Zone.regions()` exposes the
individual cuboids; `Zone.min()` and `Zone.max()` return aggregate bounds
covering every region.

`isProtected` checks the zone settings, not a particular player's bypass
permission. Access the manager after OrbZones has completed setup, such as in
your dependent mod's `setup()` method, rather than in a static initializer.

### Creating and updating zones

Mods can also manage zones directly through `ZoneManager`:

```java
// Inside a method that handles or declares IOException.
var manager = OrbZonesPlugin.get().getZoneManager();
manager.create("Spawn", worldName,
        new BlockPosition(0, 60, 0),
        new BlockPosition(20, 80, 20));
manager.setProtection("Spawn", ZoneAction.PLACE, false);
```

`create` adds a region when the name already exists; that region must be in the
same world and must not overlap another region of that zone. Use
`manager.delete("Spawn")` to remove a zone and all its regions. These changes
are saved immediately and can throw `IOException` if saving fails.
