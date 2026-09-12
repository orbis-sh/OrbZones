# OrbZones

OrbZones is a Hytale server plugin for persistent cuboid zones, block
protection, temporary 3D visualization, and Java enter/leave callbacks.

## Features

- Persistent named zones containing one or more cuboid regions
- Break, place, and block-damage protection
- Per-action protection settings
- Temporary player-only 3D zone outlines
- Enter and leave callbacks for other Java plugins
- Support for overlapping zones
- Atomic JSON storage

## Installation

Build and copy the plugin into the server's `mods` directory:

```powershell
Set-Location .\Modding\OrbZones
..\build-mod.ps1
```

Restart the server after installing or updating the JAR. OrbZones requires
Java 25 and the Hytale Server API used by this project.

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

## Persistence

Zones are stored as `zones.json` in the plugin data directory. Changes made by
commands are written immediately using a temporary file and atomic
replacement.

Stop the server before editing `zones.json` manually. Invalid or incomplete
data prevents OrbZones from loading rather than silently disabling
protection.

## Java API

Install the OrbZones artifact into the local Maven repository:

```powershell
mvn --batch-mode -DskipTests install
```

Add it to a consumer plugin:

```xml
<dependency>
    <groupId>sh.orbis</groupId>
    <artifactId>OrbZones</artifactId>
    <version>1.0-SNAPSHOT</version>
    <scope>provided</scope>
</dependency>
```

Declare the runtime plugin dependency in the consumer's `manifest.json`:

```json
{
  "Dependencies": {
    "Orbis:OrbZones": "*"
  }
}
```

### Enter and leave callbacks

```java
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
                    String player = event.player().getUsername();
                    String zone = event.zone().name();
                }

                @Override
                public void onLeave(ZoneTransitionEvent event) {
                    String player = event.player().getUsername();
                    String zone = event.zone().name();
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
var zones = OrbZonesPlugin.get()
        .getZoneManager()
        .findContaining(worldName, blockPosition);

var zone = OrbZonesPlugin.get()
        .getZoneManager()
        .getByName("StaticBuilding");
```

`findContaining` returns every matching zone. Block modification is denied
when any matching zone protects that action. `Zone.regions()` exposes the
individual cuboids; `Zone.min()` and `Zone.max()` return aggregate bounds
covering every region.
