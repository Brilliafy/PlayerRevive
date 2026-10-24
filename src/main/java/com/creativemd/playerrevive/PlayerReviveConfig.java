package com.creativemd.playerrevive;

import java.util.Arrays;
import java.util.List;

import com.creativemd.creativecore.common.config.api.CreativeConfig;
import com.creativemd.creativecore.common.config.sync.ConfigSynchronization;

public class PlayerReviveConfig {
	
	@CreativeConfig(type = ConfigSynchronization.CLIENT)
	@CreativeConfig.DecimalRange(min = 0, max = 2)
	public float volumeModifier = 1;
	
	@CreativeConfig
	public int playerReviveTime = 200;
	@CreativeConfig
	public int playerReviveSurviveTime = 2400;
	
	@CreativeConfig
	public float damageMultiplierWhileReviving = 3.0F;
	@CreativeConfig
	public float damageMultiplierMin = 1.65F;
	@CreativeConfig
	public float damageMultiplierMax = 3.0F;
	@CreativeConfig
	public boolean multiplyOnlyEntityDamage = true;
	@CreativeConfig
	public boolean resetProgressOnDamage = true;
	@CreativeConfig
	public boolean stopTimerWhileReviving = true;
	
	@CreativeConfig
	@CreativeConfig.IntRange(min = 1, max = 20)
	public int playerHealthAfter = 2;
	@CreativeConfig
	@CreativeConfig.IntRange(min = 1, max = 20)
	public int playerFoodAfter = 6;
	
	@CreativeConfig
	public boolean banPlayerAfterDeath = false;
	
	@CreativeConfig
	public float exhaustion = 0.04F;
	
	@CreativeConfig(type = ConfigSynchronization.CLIENT)
	public boolean disableMusic = false;
	@CreativeConfig
	public boolean disableSounds = false;
	
	@CreativeConfig
	public boolean disableBleedingMessage = false;
	@CreativeConfig(type = ConfigSynchronization.CLIENT)
	public boolean particleBeacon = false;
	
	@CreativeConfig
	public boolean disableGiveUp;
	@CreativeConfig
	public boolean disableDisconnect;
	
	@CreativeConfig
	public boolean allowCommandsWhileBleeding = false;
	
	@CreativeConfig
	public boolean enableTotemCompatibility = true;
	@CreativeConfig
	public boolean enablePotionCoreRevivalCompatibility = true;
	@CreativeConfig
	public boolean enableFirstAidCompatibility = true;
	@CreativeConfig
	public boolean enableInSingleplayer = true;
	
	@CreativeConfig
	public List<String> bypassDamageSources = Arrays.asList("outOfWorld", "gorgon", "death.attack.sgcraft:transient", "death.attack.sgcraft:iris");
	
}
