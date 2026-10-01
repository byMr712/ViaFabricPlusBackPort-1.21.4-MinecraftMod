# ViaFabricPlus BackPort (Fabric 1.21.4)

> **Language:** [Русский](README.md) · English

A specialized build and backport of ViaFabricPlus for **Minecraft 1.21.4 (Fabric)**, allowing players to connect to servers running virtually any Minecraft version — from classic Alpha/Beta releases to the latest versions and snapshots.

![Java 21](https://img.shields.io/badge/Java-21-blue.svg)
![Minecraft](https://img.shields.io/badge/Minecraft-1.21.4-blue.svg)
![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)
![ModMenu](https://img.shields.io/badge/ModMenu-Supported-blue.svg)
![License](https://img.shields.io/badge/License-GPL_3.0-blue.svg)

---

## About the Fork

All rights belong to the original upstream authors — [ViaVersion](https://github.com/ViaVersion) / [FlorianMichael](https://github.com/FlorianMichael) / [RaphiMC](https://github.com/RaphiMC).  
Original repository: [ViaVersion/ViaFabricPlus](https://github.com/ViaVersion/ViaFabricPlus).  

- **Stable 1.21.4 Client:** Unlike upstream ViaFabricPlus which only supports the latest Minecraft release, this fork preserves your stable 1.21.4 client and mod environment while extending protocol compatibility up to **26.3** servers.
- **Smart Version Auto-Detection:** Automatically determines the server's real version upon connection. Works seamlessly even on proxied and protected servers (Velocity, BungeeCord, Cloudflare, and anarchy servers like 6b6t) that disguise or spoof their protocol ID. Features fast ping timeouts without UI freezing and full SRV record resolution.
- **Updated Protocol Libraries:** Bundles up-to-date protocol translation libraries (**ViaVersion**, **ViaBackwards**, **ViaRewind**) for seamless multiplayer connections across all eras of Java Edition.
- **Authentic Mechanics:** Preserves all QoL fixes and legacy physics emulation (Legacy Combat, Movement, hitboxes, and collision fixes).
- **Built-in Compatibility Modules:** Includes pre-packaged `compat` patches for popular mods (Tide, Lithium, Inventory Profiles Next, Fabric API, etc.).
- **Convenient Build Scripts:** Includes `build.bat` for quick compiling.

---

## Supported Server Versions

- **Release:** 1.0.0 — 26.3
- **Beta:** b1.0 — b1.8.1
- **Alpha:** a1.0.15 — a1.2.6
- **Classic:** c0.0.15 — c0.30 (including [CPE](https://wiki.vg/Classic_Protocol_Extension))
- **Snapshots & April Fools:** 3D Shareware, 20w14infinite, Combat Snapshots 8c

---

## Mod Overview

ViaFabricPlus embeds the [ViaVersion](https://github.com/ViaVersion) protocol translators directly into the Fabric client, complemented by numerous gameplay and physics fixes replicating original version behaviors (movement, hitboxes, legacy 1.8/1.12 combat, etc.).

**Core Advantage:** You do not need to upgrade your client or break your curated 1.21.4 modpack just to join servers running newer releases or snapshots up to **26.3**.

---

## Usage

1. Open the ViaFabricPlus protocol version selector from the Title Screen or Multiplayer menu.
2. Select your desired target version manually or leave it on Auto-Detect.
3. Connect directly to the server.

---

## Building

To build the project:

```bat
build.bat
```

or via Gradle:

- **Windows:** `.\gradlew.bat clean build`
- **Linux / macOS:** `./gradlew clean build`

The compiled jar file will be located in `build/libs/`.

---

## Disclaimer & Mod Compatibility

- **Server Rules & Anti-Cheats:** Certain server administrations and anti-cheat plugins may restrict protocol translation mods. Exercise caution on competitive public servers.
- **Content Mod Compatibility:** ViaFabricPlus is designed around standard vanilla network protocols and bundles compatibility modules for known mods. However, complex third-party content mods altering low-level packets or registries may require additional compat patches or temporary disabling in multiplayer.

---

## Credits & License

- **Original Authors:** [ViaVersion](https://github.com/ViaVersion), [FlorianMichael](https://github.com/FlorianMichael), [RaphiMC](https://github.com/RaphiMC)
- **1.21.4 Backport & Maintenance:** [byMr712](https://github.com/byMr712)
- **License:** [GNU General Public License v3.0](LICENSE)
