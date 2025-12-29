# Arsenic

Arsenic is a modular SpigotMC anti-cheat plugin focused on broad version support (1.8 through 1.21.x),
with a scalable core for alerts, reporting, and persistent logs. The project is structured as a
multi-module Maven build so storage, checks, and version adapters can evolve independently.

## Features (current)

- Alert pipeline with console and staff notifications
- Configurable messages.yml formatting
- Persistent player reports (SQLite)
- Inventory GUI report with sortable alert logs
- Command utilities, tab completion, and confirmation prompts

## Build

This project is a multi-module Maven build. Build from the repo root:

```bash
mvn -U -q package
```

The plugin jar is shaded and lives at:

`modules/bukkit/target/arsenic-bukkit-1.0-SNAPSHOT.jar`

## Installation

1. Drop the Bukkit jar into your server's `plugins/` folder.
2. Start the server once to generate configs.
3. Configure `plugins/Arsenic/config.yml` and `plugins/Arsenic/messages.yml`.

## Commands

- `/arsenic testalert` - send a test alert through the pipeline
- `/arsenic report <player>` - open the report GUI or print a report in console
- `/arsenic clearlogs <player>` - clear a player's logs
- `/arsenic clearall` - clear all player data

Alias: `/as`

## Permissions

- `arsenic.command` - access to `/arsenic`
- `arsenic.report` - view reports
- `arsenic.clear.logs` - clear a player's logs
- `arsenic.clear.all` - clear all player data

## Configuration

`plugins/Arsenic/config.yml`

```yml
alerts:
  notify-staff: true
  log-to-console: true
  staff-permission: "arsenic.alerts"
database:
  type: "sqlite"
  sqlite:
    file: "arsenic.db"
```

`plugins/Arsenic/messages.yml` controls all chat output.

## Compatibility

- API target: Spigot 1.8.8 (for broad runtime compatibility)
- Runtime: works on 1.8 through 1.21.x

## Roadmap (next)

- ProtocolLib packet checks
- Per-version NMS adapters for deeper data
- Additional storage backends (SQL, MongoDB)

