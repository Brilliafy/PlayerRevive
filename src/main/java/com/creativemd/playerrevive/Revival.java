package com.creativemd.playerrevive;

import java.util.ArrayList;
import java.util.List;

import com.creativemd.playerrevive.api.CombatTrackerClone;
import com.creativemd.playerrevive.api.DamageBledToDeath;
import com.creativemd.playerrevive.api.IRevival;
import com.creativemd.playerrevive.server.PlayerReviveServer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.DamageSource;

public class Revival implements IRevival {
	
	private boolean healty = true;
	
	private float progress;
	private int timeLeft;
	
	private DamageSource lastSource;
	private CombatTrackerClone trackerClone;
	
	public final ArrayList<EntityPlayer> revivingPlayers = new ArrayList<>();
	
	public Revival() {
		this.progress = 0;
		timeLeft = PlayerRevive.CONFIG.playerReviveSurviveTime;
	}
	
	@Override
	public void resetProgress() {
		this.progress = 0;
	}
	
	@Override
	public void tick() {
		for (int i = revivingPlayers.size() - 1; i >= 0; i--) {
			EntityPlayer helper = revivingPlayers.get(i);
			if (helper == null || helper.isDead || helper.getHealth() <= 0.0F) {
				revivingPlayers.remove(i);
			}
		}
		
		if (revivingPlayers.isEmpty()) {
			timeLeft--;
			progress = 0;
		} else {
			if (!PlayerRevive.CONFIG.stopTimerWhileReviving) {
				timeLeft--;
			}
			progress += revivingPlayers.size();
		}
		
		for (int i = 0; i < revivingPlayers.size(); i++) {
			EntityPlayer helper = revivingPlayers.get(i);
			if (helper != null && helper.getFoodStats().getFoodLevel() > 1 && PlayerRevive.CONFIG.exhaustion > 0) {
				helper.addExhaustion(PlayerRevive.CONFIG.exhaustion);
			}
		}
	}
	
	@Override
	public List<EntityPlayer> getRevivingPlayers() {
		return revivingPlayers;
	}
	
	@Override
	public boolean isHealty() {
		return healty;
	}
	
	@Override
	public void stopBleeding() {
		this.timeLeft = PlayerRevive.CONFIG.playerReviveSurviveTime;
		this.progress = 0;
		this.healty = true;
		this.lastSource = null;
		this.trackerClone = null;
	}
	
	@Override
	public void startBleeding(EntityPlayer player, DamageSource source) {
		if (player != null && PlayerReviveServer.checkTotemOrPotionResurrection(player, source))
			return;
		this.healty = false;
		this.progress = 0;
		this.timeLeft = PlayerRevive.CONFIG.playerReviveSurviveTime;
		this.lastSource = source;
		if (player != null) {
			this.trackerClone = new CombatTrackerClone(player.getCombatTracker());
			if (!player.world.isRemote) {
				player.capabilities.disableDamage = true;
				player.setEntityInvulnerable(true);
				if (net.minecraftforge.fml.common.Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
					com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setWaitingForHelp(player, true);
					com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.ensureCriticalPartsAlive(player, 1.0F);
					com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, Math.max(1.0F, (float) PlayerRevive.CONFIG.playerHealthAfter));
					com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.syncDamageModel(player);
				}
				PlayerReviveServer.sendUpdatePacket(player);
			}
		}
	}
	
	@Override
	public float getProgress() {
		return progress;
	}
	
	@Override
	public boolean isRevived() {
		return progress >= PlayerRevive.CONFIG.playerReviveTime;
	}
	
	@Override
	public boolean isDead() {
		return timeLeft <= 0;
	}
	
	@Override
	public int getTimeLeft() {
		return timeLeft;
	}
	
	@Override
	public void kill() {
		timeLeft = 0;
		progress = 0;
		healty = true;
		lastSource = null;
		trackerClone = null;
	}
	
	public void writeToNBT(NBTTagCompound nbt) {
		nbt.setInteger("timeLeft", timeLeft);
		nbt.setFloat("progress", progress);
		nbt.setBoolean("healty", healty);
	}
	
	public void readFromNBT(NBTTagCompound nbt) {
		timeLeft = nbt.getInteger("timeLeft");
		progress = nbt.getFloat("progress");
		healty = nbt.getBoolean("healty");
	}
	
	@Override
	public NBTTagCompound serializeNBT() {
		NBTTagCompound nbt = new NBTTagCompound();
		writeToNBT(nbt);
		return nbt;
	}
	
	@Override
	public void deserializeNBT(NBTTagCompound nbt) {
		readFromNBT(nbt);
	}
	
	@Override
	public DamageSource getSource() {
		if (lastSource != null)
			return lastSource;
		return DamageBledToDeath.bledToDeath;
	}
	
	@Override
	public CombatTrackerClone getTrackerClone() {
		return trackerClone;
	}
}
