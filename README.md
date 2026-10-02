# mc-extraction

A Java project built around the Minestom game server framework for Minecraft world extraction / server-side tooling.

## Overview

This repository contains the following modules:

- `mineshaft-core` – core server logic and game logic
- `stomui` – UI-related submodule dependency

## Requirements

- Java 25
- Maven

## Build

```bash
mvn clean install
```

## Run

```bash
mvn -pl mineshaft-core exec:java
```

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.
