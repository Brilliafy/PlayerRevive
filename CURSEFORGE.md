# PlayerRevive: First Aid Edition (1.12.2)

**PlayerRevive: First Aid Edition** is a drop-in, modpack-optimized fork of [CreativeMD's PlayerRevive](https://www.curseforge.com/minecraft/mc-mods/playerrevive) for **Minecraft 1.12.2**.

It solves long-standing modpack incompatibilities most notably fixing the notorious **First Aid** instant death/limb desync issue and **Totem of Undying** bypasses while introducing high-stakes, tactical revival mechanics for multiplayer co-op.

---

### ⚠️ Credits & Open Source Notice
This project is an open-source continuation and enhancement distributed under the **GNU General Public License v3 (GPL-3.0)**.
* **Original Creator:** **[CreativeMD](https://www.curseforge.com/members/creativemd)**
* **Original Mod:** [PlayerRevive on CurseForge](https://www.curseforge.com/minecraft/mc-mods/playerrevive)
* **Original GitHub Repository:** [CreativeMD/PlayerRevive](https://github.com/CreativeMD/PlayerRevive)
* **Fork Source Code:** [GitHub Repository](https://github.com/Brilliafy/PlayerRevive)

*Huge thanks and full credit to CreativeMD for creating the original mod and its visual framework.*

---

### 🌟 Why Choose This Fork?

If you are running a 1.12.2 modpack with **First Aid**, **PotionCore**, or hardcore survival mechanics, the upstream version of PlayerRevive often causes players to die instantly when their head or torso drops to 0 HP, bypassing the bleedout state completely.

This fork addresses these pain points with zero compromises:

#### 🩺 Seamless First Aid Compatibility
* Downed players with 0 HP on head or body are safely caught by PlayerRevive before First Aid calls its internal death trigger.
* Critical parts are preserved at $\ge 1.0\text{ HP}$ while bleeding.
* When revived, First Aid's damage model is updated and automatically synchronized to the client with `MessageSyncDamageModel`.

#### 🛡️ Totem of Undying & PotionCore Support
* **Totem of Undying**: Holding a Totem in either hand takes priority over bleeding. When you take fatal damage, your Totem pops normally with vanilla sound, animations, and absorption/regeneration effects instead of forcing you into a bleedout.
* **PotionCore Revival**: Players under the effect of PotionCore's Revival potion will consume the effect and heal immediately without downing.

#### ⚔️ Tactical, High-Stakes Reviving
* **Helper Vulnerability**: Players actively reviving a teammate take increased damage (configurable, default 1.65x – 3.0x). Teammates must provide cover or clear mobs first.
* **Hit Interruption**: If the reviving player is damaged by an enemy, the revive progress resets back to 0.
* **Countdown Pausing**: While a teammate is actively reviving you, the bleedout countdown timer pauses, ensuring clutch rescues are rewarded.
* **Rebalanced Timing**: Default revive duration set to 10 seconds (200 ticks) and bleedout survival time set to 2 minutes (2400 ticks).

#### 🔒 Movement & Physics Fixes
* Downed players cannot slide, bunny-hop, or jump away while bleeding out (`motionX = 0`, `motionZ = 0`, `motionY <= 0`).
* Pressing `ESC` or the Inventory key while bleeding out will not accidentally close the revive GUI.

#### 📶 Dual-Channel Synchronization
* Employs Forge's `SimpleNetworkWrapper` channel alongside CreativeCore to eliminate packet loss and ghost bleedout states on multiplayer servers.
* Automatically syncs bleeding state to players entering render distance (`PlayerEvent.StartTracking`).

#### 👤 Singleplayer Toggle
* Want to test configs, companion mods, or mechanics in singleplayer? Set `enableInSingleplayer = true` in `config/playerrevive.cfg`.

---

### 📋 Requirements & Installation

1. **Minecraft Forge 1.12.2** (Build `14.23.5.2847` or newer recommended).
2. **[CreativeCore](https://www.curseforge.com/minecraft/mc-mods/creativecore)** (Version `1.10.71` or newer for 1.12.2).
3. *(Optional)* **[First Aid](https://www.curseforge.com/minecraft/mc-mods/first-aid)** and/or **[Potion Core](https://www.curseforge.com/minecraft/mc-mods/potion-core)**.

Simply drop `PlayerRevive-FirstAidEdition-1.2.41.jar` into your `mods/` directory. It acts as a drop-in replacement for the original PlayerRevive.

---

### ⚙️ Default Configuration (`config/playerrevive.cfg`)

```hocon
# General Settings
playerReviveTime=200                  # Revive time in ticks (200 = 10s)
playerReviveSurviveTime=2400          # Bleedout survive time (2400 = 120s)
exhaustion=0.04                       # Exhaustion added to helper per tick

# Combat Mechanics
damageMultiplierWhileReviving=3.0     # Damage multiplier on helpers
damageMultiplierMin=1.65              # Min random multiplier
damageMultiplierMax=3.0               # Max random multiplier
multiplyOnlyEntityDamage=true         # Only multiply mob/player damage
resetProgressOnDamage=true            # Reset progress when helper is hit
stopTimerWhileReviving=true           # Pause timer during active revive

# Mod Integrations
enableFirstAidCompatibility=true      # First Aid limb death prevention
enableTotemCompatibility=true         # Vanilla Totem priority
enablePotionCoreRevivalCompatibility=true # PotionCore Revival priority
enableInSingleplayer=true             # Singleplayer support toggle
```

---

### 📦 Modpack Policy
You are free to include this mod in any public or private modpack on CurseForge, Modrinth, FTB, or Technic under the terms of the **GNU GPLv3**. No separate permission is required.
