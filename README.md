# mc-extraction

Minecraft-themed extraction game server built with Java and the Minestom server framework.

This repository contains a multiplayer game prototype centered around themed extraction gameplay, procedural mine shaft generation, lobby flow, party systems, and server-side world logic. The project is structured as a Maven multi-module build and includes a submodule dependency for the `stomui` UI library.

## Project overview

`mc-extraction` is designed as a Java-based Minestom server application. It creates a custom game loop for a lobby, matchmaking, party management, and in-game extraction mechanics. Core systems include:

- procedural mine shaft generation and segment loading
- lobby instance setup and world scaffolding
- party and matchmaking logic
- custom commands for gameplay and admin controls
- loot tables and mob spawning behavior
- server-side GUI and shop-style interfaces
- event-driven block handling and game state management

## Repository structure

```text
mc-extraction/
├── .gitignore
├── .gitmodules
├── LICENSE
├── README.md
├── pom.xml
├── mineshaft-core/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── ca/maximilian/mineshaft/
│           │       ├── command/
│           │       ├── core/
│           │       ├── lobby/
│           │       ├── worldgen/
│           │       ├── exeptions/
│           │       └── ...
│           └── resources/
│               └── mineshaft/
└── stomui/
```

## Modules

### `mineshaft-core`
The main game project. This contains the actual server implementation, including:

- `Mineshaft.java` — application bootstrap and server initialization
- `command/` — gameplay and admin commands (`ExtractionCommands`, party commands, game mode tools)
- `core/` — core runtime logic, chunk handling, player state, GUI, item/shop systems, block handlers, and event handling
- `lobby/` — lobby creation, matchmaking, invites, party coordination, and lobby instance handling
- `worldgen/` — mine shaft generation, segment logic, clustering, and WFC-style procedural generation
- `core/loot/` and `core/mob/` — loot tables and enemy spawn entries
- `resources/mineshaft/` — generated layout files and NBT schematics used by the game world

### `stomui`
A linked submodule dependency used by the main project. It is configured through `.gitmodules` and should be initialized when first checking out the repository.

## Features

- Minestom-based Minecraft server foundation
- custom lobby and instance management
- extraction gameplay with world generation
- player and party management
- command-driven game flow
- custom block handlers for gameplay interactions
- loot generation and mob balancing
- server-side GUI and in-game shop views

## Requirements

Before building and running the project, ensure you have:

- Java 25
- Maven 3.9+
- Git with submodules enabled

The application specifically checks for ZGC support at runtime and will refuse to start unless a compatible JVM is used. This is enforced in `Mineshaft.java`.

## Getting started

### 1. Clone the repository

```bash
git clone https://github.com/Maximilian-school/mc-extraction.git
cd mc-extraction
```

### 2. Initialize submodules

```bash
git submodule update --init --recursive
```

### 3. Build the project

```bash
mvn clean install
```

This will build the multi-module project and install the dependencies required by the application.

## Running the server

From the repository root:

```bash
mvn -pl mineshaft-core exec:java
```

This launches the server entry point defined in the `mineshaft-core` module.

You may also run it directly from the module folder:

```bash
cd mineshaft-core
mvn exec:java
```

## Runtime notes

The application starts a Minestom server bound to:

- host: `0.0.0.0`
- port: `25565`

It also uses Java runtime flags related to ZGC/Enhanced Class Redefinition. If you are running this locally, make sure your JDK matches the project's expectations.

## Development notes

The project is heavily structured around object-oriented game systems and modular server logic. Common development areas include:

- `command/` for multiplayer and server commands
- `worldgen/` for generated map layouts and obstacle generation
- `core/handler/` for game interaction rules such as block actions and validation
- `core/loot/` and `core/mob/` for rewards and enemy generation
- `lobby/` for the main waiting and matchmaking flow

## Licensing

This project is licensed under the MIT License. See [LICENSE](LICENSE) for the full license text.

## Contributing

Contributions are welcome. If you add new gameplay systems, worldgen logic, or commands, please keep the project organized and consistent with the current module structure.
