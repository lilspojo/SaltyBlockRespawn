# SaltyBlockRespawn

[![GitHub release (latest by date)](https://img.shields.io/github/v/release/lilspojo/SaltyBlockRespawn?style=flat-square)](https://github.com/lilspojo/SaltyBlockRespawn/releases)
[![GitHub issues](https://img.shields.io/github/issues/lilspojo/SaltyBlockRespawn?style=flat-square)](https://github.com/lilspojo/SaltyBlockRespawn/issues)
[![License](https://img.shields.io/github/license/lilspojo/SaltyBlockRespawn?style=flat-square)](LICENSE)

---

## ❓ What is SaltyBlockRespawn?
Simple! SaltyBlockRespawn is a plugin which respawns broken blocks after a set amount of time, with instant temporary block replacements, as well as WorldGuard region support!

## 🚀 Features

* **Configurable Block Respawning:** Configure different block types, immediate replacements, block respawn delays, and more!
* **Crash Protection:** If your server ever crashes and a block has not yet respawned, they will be automatically recovered on startup!
* **Block Protection:** Prevent mining of blocks which aren't respawnable, configurable per region!
* **Block Data Support:** Block respawns take block data into account, so block state, orientation, and custom data will be saved!
* **Creative Bypass:** Toggle block respawn and protection while in creative mode in the config to easily work on the map!
* **High Performance:** Using async tasks wherever possible, performance never takes a hit, even with hundreds of simultaneous respawns!

## 🧩 Dependencies

* WorldEdit
* WorldGuard
* **This plugin requires Java 21 to run.**

## ⚙️ Configuration Files
* [config.yml](https://github.com/lilspojo/SaltyBlockRespawn/blob/master/src/main/resources/config.yml)
* [example_region.yml](https://github.com/lilspojo/SaltyBlockRespawn/blob/master/src/main/resources/regions/example_region.yml)
* [lang.yml](https://github.com/lilspojo/SaltyBlockRespawn/blob/master/src/main/resources/lang.yml)

## 📖 Commands

* **/saltyblockrespawn reload:** Reload all configuration files.

## ⚖️ Permissions

* **saltyblockrespawn.reload:** Permission to perform the reload command.

## ❓ More About The Plugin

SaltyBlockRespawn was originally created for my own Minecraft server: Salty Universe. For a long time we used Skript to handle block respawns, but always knew a real plugin would be best, but no good options existed, all being outdated or just plain not good. So, I took matters into my own hands, and created SaltyBlockRespawn. Realizing the gap in the market, I decided I'd release the plugin for public use as well!
