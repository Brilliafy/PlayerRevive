package com.creativemd.playerrevive.compat.firstaid;

import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.api.event.FirstAidLivingDamageEvent;

import com.creativemd.playerrevive.PlayerRevive;
import com.creativemd.playerrevive.api.DamageBledToDeath;
import com.creativemd.playerrevive.api.IRevival;
import com.creativemd.playerrevive.server.PlayerReviveServer;
import com.creativemd.playerrevive.server.ReviveEventServer;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class FirstAidHandler {

	public static void init() {
		MinecraftForge.EVENT_BUS.register(new FirstAidHandler());
	}

	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onFirstAidLivingDamage(FirstAidLivingDamageEvent event) {
		if (!PlayerRevive.CONFIG.enableFirstAidCompatibility) {
			return;
		}

		EntityPlayer player = event.getEntityPlayer();
		if (player == null || player.world == null || player.world.isRemote) {
			return;
		}

		DamageSource source = event.getSource();
		if (source == DamageBledToDeath.bledToDeath || (source != null && (source.canHarmInCreative() || PlayerRevive.CONFIG.bypassDamageSources.contains(source.damageType)))) {
			return;
		}

		IRevival revival = PlayerReviveServer.getRevival(player);
		if (revival != null && !revival.isHealty()) {
			event.setCanceled(true);
			return;
		}

		AbstractPlayerDamageModel after = event.getAfterDamage();
		boolean isCritical = false;
		if (after != null) {
			if (after.isDead(player)) {
				isCritical = true;
			} else {
				for (AbstractDamageablePart part : after) {
					if (part.canCauseDeath && part.currentHealth <= 0.0F) {
						isCritical = true;
						break;
					}
				}
			}
		}
		if (player.getHealth() <= 0.0F) {
			isCritical = true;
		}

		if (!isCritical) {
			return;
		}

		// 1. Check Totem of Undying or PotionCore Revival
		if (PlayerReviveServer.checkTotemOrPotionResurrection(player, source)) {
			event.setCanceled(true);
			return;
		}

		// 2. Check if revive is active
		if (!ReviveEventServer.isReviveActive()) {
			return;
		}

		// 3. Down player and cancel FirstAid event so killPlayer is bypassed
		event.setCanceled(true);

		PlayerReviveServer.startBleeding(player, source);
		player.capabilities.disableDamage = true;
		player.setEntityInvulnerable(true);

		if (player.isRiding()) {
			player.dismountRidingEntity();
		}

		player.getFoodStats().setFoodLevel(1);

		FirstAidHelper.setWaitingForHelp(player, true);
		FirstAidHelper.ensureCriticalPartsAlive(player, 1.0F);
		FirstAidHelper.setPlayerHealthDirect(player, (float) PlayerRevive.CONFIG.playerHealthAfter);
		FirstAidHelper.syncDamageModel(player);
		PlayerReviveServer.sendUpdatePacket(player);

		MinecraftServer server = player.getServer();
		if (server != null) {
			server.addScheduledTask(() -> {
				if (player.isDead)
					return;
				IRevival rev = PlayerReviveServer.getRevival(player);
				if (rev == null || rev.isHealty() || rev.isDead())
					return;
				FirstAidHelper.setWaitingForHelp(player, true);
				FirstAidHelper.ensureCriticalPartsAlive(player, 1.0F);
				FirstAidHelper.setPlayerHealthDirect(player, (float) PlayerRevive.CONFIG.playerHealthAfter);
				FirstAidHelper.syncDamageModel(player);
				if (player instanceof net.minecraft.entity.player.EntityPlayerMP && !(((net.minecraft.entity.player.EntityPlayerMP) player).openContainer instanceof com.creativemd.creativecore.common.gui.mc.ContainerSub)) {
					com.creativemd.creativecore.common.gui.opener.GuiHandler.openGui("plrevive", new net.minecraft.nbt.NBTTagCompound(), (net.minecraft.entity.player.EntityPlayerMP) player);
				}
				PlayerReviveServer.sendUpdatePacket(player);
			});
		}

		if (!PlayerRevive.CONFIG.disableBleedingMessage && player.getServer() != null) {
			player.getServer().getPlayerList().sendMessage(
				new TextComponentTranslation("playerrevive.chat.bleeding", player.getDisplayNameString())
			);
		}
	}
}
