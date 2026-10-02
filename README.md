# MvndiCapeHider

Hides player capes on Paper and Folia servers — no external dependencies.

Every joining player has their cape removed via Paper's native `PlayerTextures`
profile API, and can toggle their own cape back with `/togglecape`. Works with
vanilla and custom (optifine/cape services) capes since it operates on the
profile the server broadcasts, not on any specific cape source.

Based on [CapeHider](https://github.com/jordoncodes) by onlyjordon, rewritten
to drop the Nicknamer API/PacketEvents dependency and to support Folia.

## Requirements

| | |
|---|---|
| Server | Paper or Folia 1.21.x (Java 21+) |
| Dependencies | none |

## Installation

Drop the jar into `plugins/` and restart. There is no configuration.

## Commands

- `/togglecape` — toggle your own cape (`capehider.togglecape.self`)
- `/togglecape <player>` — toggle another player's cape (`capehider.togglecape.others`)

## Permissions

| Permission | Default | Effect |
|---|---|---|
| `capehider.togglecape` | op | base permission for the command |
| `capehider.togglecape.self` | op | toggle your own cape |
| `capehider.togglecape.others` | op | toggle other players' capes |
| `capehider.bypass` | false | cape is never hidden on join |

## How it works

On join the plugin stores the player's original cape URL, then clears the cape
from their profile one tick later (the small delay lets the client finish the
login sequence). The profile update makes every other client re-spawn the
player without a cape. On quit (or server stop) the stored URL is simply
discarded — textures are re-fetched from the session servers on the next join,
and the hide is re-applied then.

On Folia, profile updates are scheduled on the player's entity scheduler so
they always run on the owning region thread.

## Building

With the Gradle wrapper (JDK 21 required):

```bash
./gradlew build
```

The jar is written to `build/libs/MvndiCapeHider-<version>.jar`.

`runServer` / `runFoliaServer` Gradle tasks (via [run-paper](https://github.com/jpenilla/run-paper)) are available for testing
against a downloaded Paper or Folia server.

## Changelog

- **2.0.1** — Fix `UnsupportedOperationException` crash on Folia when a player
  joins (global Bukkit scheduler replaced with the entity scheduler);
  `/togglecape <player>` now also runs the profile update on the target's
  region thread; Gradle wrapper jar committed so fresh clones can build;
  toolchain kept at Java 21.
- **2.0.0** — Rewritten for Paper 1.21.x using the native `PlayerTextures`
  API; Nicknamer API and PacketEvents dependencies removed; Folia supported.
