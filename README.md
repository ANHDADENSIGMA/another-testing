# The Stalker: Six Nights — source 0.4.0

Minecraft Forge 1.20.1, Java 17, Forge 47.2.0.

## Entities
- `stalkersixnights:watcher` — watcher role, slow and watchful.
- `stalkersixnights:runner` — faster pursuer.
- `stalkersixnights:mimic` — deceptive stalker with its own role and voice cue.
- `stalkersixnights:face` — stationary full-image apparition using the supplied transparent artwork.
- `stalkersixnights:stalker_child` — secondary PNG billboard stalkers, not GUI jumpscares; they can watch, wander and sometimes pursue.
- `stalkersixnights:stalker` remains as a legacy compatibility entity.

## Commands (OP level 2)
`/stalker spawn watcher|runner|mimic|face|child`
`/stalker remove`

Vanilla summon IDs also work, e.g. `/summon stalkersixnights:runner`.

## Fog behavior
The ordinary nearby-stalker fog tint was removed. The near-black fog color and shortened fog distance are applied only while a Runner/Mimic/child is actively pursuing a player.

## Audio
Includes the three voice clips supplied by the project owner plus original procedural cinematic chase music and generated effects (heartbeat, footsteps, knock, scratch, breathing, teleport and impact). All are OGG resources registered by `sounds.json` and `StalkerSounds`. Chase music starts only during active pursuit.

## Build
Use Java 17 and run `gradle build` (or the included GitHub Actions workflow). Output is in `build/libs/`.

This source was prepared against Forge 1.20.1-47.2.0. It has not yet been verified in a live Minecraft client; build/runtime testing is still required.
