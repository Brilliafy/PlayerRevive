package com.creativemd.playerrevive.server;

import java.io.IOException;
import java.util.Iterator;

import com.creativemd.creativecore.common.gui.opener.GuiHandler;
import com.creativemd.creativecore.common.packet.PacketHandler;
import com.creativemd.playerrevive.PlayerRevive;
import com.creativemd.playerrevive.api.CombatTrackerClone;
import com.creativemd.playerrevive.api.IRevival;
import com.creativemd.playerrevive.api.capability.CapaRevive;
import com.creativemd.playerrevive.api.event.PlayerKilledEvent;
import com.creativemd.playerrevive.api.event.PlayerRevivedEvent;
import com.creativemd.playerrevive.packet.ReviveUpdatePacket;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import com.creativemd.creativecore.common.gui.container.SubContainer;
import com.creativemd.creativecore.common.gui.mc.ContainerSub;
import com.creativemd.playerrevive.gui.SubContainerRevive;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.common.MinecraftForge;

public class PlayerReviveServer {
	
	//private static MinecraftServer server = FMLServerHandler.instance().getServer();
	
	public static boolean isPlayerBleeding(EntityPlayer player) {
		return !player.getCapability(CapaRevive.reviveCapa, null).isHealty();
	}
	
	public static void sendUpdatePacket(EntityPlayer player) {
		if (player == null || player.world == null)
			return;
		ReviveUpdatePacket packet = new ReviveUpdatePacket(player);
		if (PlayerRevive.NETWORK != null) {
			try {
				PlayerRevive.NETWORK.sendToAllTracking(packet, player);
				if (player instanceof EntityPlayerMP) {
					PlayerRevive.NETWORK.sendTo(packet, (EntityPlayerMP) player);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			if (player instanceof EntityPlayerMP) {
				PacketHandler.sendPacketToTrackingPlayers(packet, (EntityPlayerMP) player);
				PacketHandler.sendPacketToPlayer(packet, (EntityPlayerMP) player);
			}
		} catch (Exception ignored) {}
	}
	
	public static boolean checkTotemOrPotionResurrection(EntityPlayer player, DamageSource source) {
		return ResurrectionHelper.checkTotemOrPotionResurrection(player, source);
	}
	
	public static void startBleeding(EntityPlayer player, DamageSource source) {
		if (checkTotemOrPotionResurrection(player, source))
			return;
		getRevival(player).startBleeding(player, source);
		if (player instanceof EntityPlayerMP) {
			GuiHandler.openGui("plrevive", new net.minecraft.nbt.NBTTagCompound(), (EntityPlayerMP) player);
		}
		sendUpdatePacket(player);
	}
	
	private static void resetPlayer(EntityPlayer player, IRevival revive) {
		player.capabilities.disableDamage = player.capabilities.isCreativeMode;
		player.setEntityInvulnerable(false);
		
		for (int i = 0; i < revive.getRevivingPlayers().size(); i++) {
			revive.getRevivingPlayers().get(i).closeScreen();
		}
		if (player instanceof EntityPlayerMP) {
			player.closeScreen();
		}
	}
	
	public static void revive(EntityPlayer player) {
		IRevival revive = getRevival(player);
		MinecraftForge.EVENT_BUS.post(new PlayerRevivedEvent(player, revive));
		revive.stopBleeding();
		resetPlayer(player, revive);
		
		if (net.minecraftforge.fml.common.Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.revivePlayer(player);
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, (float) PlayerRevive.CONFIG.playerHealthAfter);
		} else {
			player.setHealth(PlayerRevive.CONFIG.playerHealthAfter);
		}
		
		if (!PlayerRevive.CONFIG.disableSounds)
			player.world.playSound(null, player.getPosition(), PlayerRevive.revivedSound, SoundCategory.PLAYERS, 1, 1);
		
		sendUpdatePacket(player);
	}
	
	public static void kill(EntityPlayer player) {
		IRevival revive = getRevival(player);
		MinecraftForge.EVENT_BUS.post(new PlayerKilledEvent(player, revive));
		DamageSource source = revive.getSource();
		CombatTrackerClone trackerClone = revive.getTrackerClone();
		if (trackerClone != null)
			trackerClone.overwriteTracker(player.getCombatTracker());
		revive.kill();
		resetPlayer(player, revive);
		if (net.minecraftforge.fml.common.Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setWaitingForHelp(player, false);
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, 0.0F);
		} else {
			player.setHealth(0.0F);
		}
		player.onDeath(source);
		
		if (!PlayerRevive.CONFIG.disableSounds)
			player.world.playSound(null, player.getPosition(), PlayerRevive.deathSound, SoundCategory.PLAYERS, 1, 1);
		
		if (PlayerRevive.CONFIG.banPlayerAfterDeath) {
			GameProfile profile = null;
			profile = player.getGameProfile();
			player.getServer().getPlayerList().getBannedPlayers().addEntry(new UserListBansEntry(player.getGameProfile()));
			try {
				player.getServer().getPlayerList().getBannedPlayers().writeChanges();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		sendUpdatePacket(player);
	}
	
	public static IRevival getRevival(EntityPlayer player) {
		return player.getCapability(CapaRevive.reviveCapa, null);
	}
	
	public static void removePlayerAsHelper(EntityPlayer player) {
		MinecraftServer server = ReviveEventServer.getMinecraftServer();
		if (server == null && player != null)
			server = player.getServer();
		if (server != null) {
			for (Iterator<EntityPlayerMP> iterator = server.getPlayerList().getPlayers().iterator(); iterator.hasNext();) {
				EntityPlayerMP member = iterator.next();
				IRevival revive = getRevival(member);
				revive.getRevivingPlayers().remove(player);
			}
		}
	}
	
	public static EntityPlayer getBleedingPlayerForHelper(EntityPlayer helper) {
		if (helper == null || helper.world == null || helper.world.isRemote)
			return null;
		MinecraftServer server = ReviveEventServer.getMinecraftServer();
		if (server == null)
			server = helper.getServer();
		if (server == null)
			return null;
		UUID helperUuid = helper.getUniqueID();
		for (EntityPlayerMP member : server.getPlayerList().getPlayers()) {
			IRevival revive = getRevival(member);
			if (revive != null && !revive.isHealty()) {
				for (EntityPlayer reviving : revive.getRevivingPlayers()) {
					if (reviving != null && reviving.getUniqueID().equals(helperUuid)) {
						return member;
					}
				}
			}
		}
		return null;
	}
	
	public static void syncGui(EntityPlayer player) {
		if (player != null && player.openContainer instanceof ContainerSub) {
			for (SubContainer sub : ((ContainerSub) player.openContainer).layers) {
				if (sub instanceof SubContainerRevive) {
					((SubContainerRevive) sub).forceSync();
				}
			}
		}
	}
	
	public static void forceSyncContainers(EntityPlayer bleedingPlayer, EntityPlayer helper) {
		syncGui(bleedingPlayer);
		if (helper != null && helper != bleedingPlayer) {
			syncGui(helper);
		}
	}
	
	public void loadSide() {
		
	}
	
	public void handleClientPacket(ReviveUpdatePacket message) {
		
	}
}
