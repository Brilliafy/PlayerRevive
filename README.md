# PlayerRevive: First Aid Edition (Minecraft 1.12.2)

[![Minecraft 1.12.2](https://img.shields.io/badge/Minecraft-1.12.2-blue.svg)](https://minecraft.net/)
[![Forge](https://img.shields.io/badge/Forge-14.23.5.2847+-orange.svg)](https://files.minecraftforge.net/)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-green.svg)](https://www.gnu.org/licenses/gpl-3.0)
[![Requires CreativeCore](https://img.shields.io/badge/Requires-CreativeCore-red.svg)](https://curseforge.com/minecraft/mc-mods/creativecore)

**PlayerRevive: First Aid Edition** is an enhanced, modpack-optimized fork of [CreativeMD's PlayerRevive](https://github.com/CreativeMD/PlayerRevive) for Minecraft 1.12.2.

Designed specifically for challenging multiplayer environments and hardcore modpacks (such as RLCraft, Dregora, and Rebirth of the Night), this edition resolves critical mod conflicts (such as **First Aid** limb death and **Totem of Undying** bypasses) while transforming reviving into a high-stakes, tactical team interaction.

---

## 📜 Credits & Attribution

This mod is an open-source fork released under the **GNU General Public License v3 (GPL-3.0)**.
* **Original Author & Maintainer:** [CreativeMD](https://github.com/CreativeMD)
* **Original Repository:** [CreativeMD/PlayerRevive](https://github.com/CreativeMD/PlayerRevive)
* **Original CurseForge Project:** [PlayerRevive on CurseForge](https://www.curseforge.com/minecraft/mc-mods/playerrevive)

All original assets, networking foundations, and core GUI logic were created by CreativeMD. This fork builds upon that foundation to provide modern modpack compatibility and combat balancing.

---

## ⚔️ Key Features & Improvements

### 1. 🩺 Full First Aid Mod Compatibility
In vanilla 1.12.2 and older versions of PlayerRevive, using the **First Aid** mod resulted in severe issues: fatal damage to the head or torso caused an unblockable internal kill that completely bypassed the bleed-out state, or resulted in ghost health desynchronization.
* Intercepts `FirstAidLivingDamageEvent` before lethal damage resolves.
* Sets First Aid's `waitingForHelp` flag and locks critical body parts at $\ge 1.0\text{ HP}$, cleanly transferring the player into the PlayerRevive bleed state.
* Automatically triggers `FirstAidHelper.revivePlayer(...)` and sends a `MessageSyncDamageModel` packet upon successful revival so client and server limb health are always in lockstep.

### 2. 🛡️ Native Totem of Undying & PotionCore Support
* **Totem of Undying**: Vanilla Totems in either hand (main-hand or off-hand) now take precedence over bleeding. If you take fatal damage while holding a Totem, it triggers standard vanilla totem effects, particles, advancements, and sounds instead of downing you.
* **PotionCore Revival Effect**: If you have the PotionCore *Revival* potion active, taking fatal damage consumes the buff, restores your health based on the amplifier, plays the revival effect, and skips the bleedout state entirely.

### 3. 🎯 Tactical, High-Stakes Reviving
Upstream PlayerRevive allowed helpers to revive teammates with virtually zero combat risk. This fork introduces tactical mechanics:
* **Helper Vulnerability (Damage Multiplier)**: When reviving a fallen player, helpers take increased damage (`damageMultiplierMin: 1.65x` to `damageMultiplierMax: 3.0x`, default `3.0x`). Teammates must clear the area or provide cover.
* **Progress Interruption on Hit**: If a helper takes entity damage while reviving, revival progress is reset to 0 (`resetProgressOnDamage = true`).
* **Timer Pausing**: While helpers are actively reviving the fallen player, the bleedout countdown timer pauses (`stopTimerWhileReviving = true`), rewarding coordinated rescue attempts.
* **Fair Exhaustion**: Helper exhaustion was re-tuned from `0.5F` to `0.04F`, preventing helpers from dropping to zero hunger instantly during a revive.

### 4. 🔒 Movement Lock & Exploit Prevention
* Downed players have horizontal and upward momentum locked (`motionX = 0`, `motionZ = 0`, `motionY <= 0`). Players cannot bunny-hop, slide, or escape while bleeding out.
* The revive GUI intercepts `ESC` and the Inventory key so downed players cannot accidentally dismiss their revive prompt.
* If a helper disconnects, dies, or closes their screen, they are safely cleaned up from the active revivers list and progress resets cleanly.

### 5. 📡 Robust Networking & Synchronization
* Added a dedicated Forge `SimpleNetworkWrapper` channel (`playerrevive_net`) alongside CreativeCore's packet handler to ensure reliable delivery across congested networks.
* Listens to Forge `PlayerEvent.StartTracking` events so players entering chunk render distance immediately receive the bleeding status of fallen allies.
* Increases GUI sync frequency from every 10 ticks to every 2 ticks, producing a smooth, responsive progress bar.

### 6. 👤 Singleplayer Testing & Support
* Added `enableInSingleplayer = true` in config. Upstream forcefully disabled revive features in singleplayer worlds; with this option enabled, players can test revive interactions in singleplayer environments.

---

## 📊 Comparison: Upstream vs. First Aid Edition

| Feature | CreativeMD / PlayerRevive (Upstream) | PlayerRevive: First Aid Edition (This Fork) |
| :--- | :--- | :--- |
| **First Aid Compatibility** | ❌ Fatal head/body injury causes instant death or HP desync | ✅ Fully integrated with health models & sync packets |
| **Totem of Undying** | ⚠️ Can be ignored or trigger incorrectly after downing | ✅ Checked first; triggers vanilla totem mechanics |
| **PotionCore Revival Potion** | ❌ Ignored | ✅ Consumed to resurrect player before bleeding |
| **Default Revive Time** | 5 seconds (100 ticks) | **10 seconds (200 ticks)** |
| **Default Bleedout Time** | 60 seconds (1200 ticks) | **120 seconds (2400 ticks)** |
| **Timer During Revive** | Continues ticking down | **Pauses countdown while being revived** |
| **Helper Vulnerability** | Standard damage (1.0x) | **Amplified damage (1.65x – 3.0x)** |
| **Interruption on Hit** | None; helper continues | **Resets revive progress to 0 on entity damage** |
| **Helper Exhaustion** | `0.5F` per tick (rapid hunger drain) | **`0.04F` per tick (balanced)** |
| **Downed Movement** | Free drifting / knockback | **Fully locked horizontal/upward velocity** |
| **Singleplayer Toggle** | Hardcoded off in singleplayer | **Configurable (`enableInSingleplayer`)** |
| **Networking** | CreativeCore packet channel only | **Dual channel (Forge SimpleNetwork + CreativeCore)** |
| **Tracking Sync** | Requires manual update | **Hooks `PlayerEvent.StartTracking`** |

---

## ⚙️ Configuration Reference

The configuration file is located at `config/playerrevive.cfg`:

```hocon
# General Settings
playerReviveTime=200                  # Ticks needed to revive (200 = 10s)
playerReviveSurviveTime=2400          # Bleedout survive time in ticks (2400 = 120s)
exhaustion=0.04                       # Hunger exhaustion added to helper per tick

# Tactical Combat Mechanics
damageMultiplierWhileReviving=3.0     # Base damage multiplier on helpers
damageMultiplierMin=1.65              # Minimum random damage multiplier
damageMultiplierMax=3.0               # Maximum random damage multiplier
multiplyOnlyEntityDamage=true         # Only multiply damage from mobs/players (not fire/fall)
resetProgressOnDamage=true            # Reset revive progress to 0 if helper takes hit
stopTimerWhileReviving=true           # Pause bleedout countdown while being revived

# Mod Compatibilities
enableFirstAidCompatibility=true      # Enable First Aid limb damage integration
enableTotemCompatibility=true         # Check Totem of Undying before downing
enablePotionCoreRevivalCompatibility=true # Support PotionCore Revival potion
enableInSingleplayer=true             # Allow revive mechanics in singleplayer

# Damage Source Bypass
bypassDamageSources=[
    "outOfWorld",
    "gorgon",
    "death.attack.sgcraft:transient",
    "death.attack.sgcraft:iris"
]
```

---

## 🔨 Building from Source

This project uses **ForgeGradle 2.3** and requires **Java 8 (JDK 1.8)**.

1. Clone the repository:
   ```bash
   git clone https://github.com/Brilliafy/PlayerRevive.git
   cd PlayerRevive
   ```
2. Build the reobfuscated mod jar:
   ```bash
   ./gradlew clean build
   ```
3. The compiled jar will be located in:
   ```
   build/libs/PlayerRevive-FirstAidEdition-1.2.41.jar
   ```

*Note: Dependencies (CreativeCore, First Aid, and PotionCore) are automatically resolved through [CurseMaven](https://cursemaven.com/). No local jar installations are required.*

---

## 📄 License

This project is licensed under the **GNU General Public License v3.0**. See the [LICENSE](LICENSE) file for the full license text.
