package com.creativemd.playerrevive.server;

import com.creativemd.creativecore.common.gui.mc.ContainerSub;
import com.creativemd.creativecore.common.gui.opener.GuiHandler;
import com.creativemd.playerrevive.CapaReviveProvider;
import com.creativemd.playerrevive.PlayerRevive;
import com.creativemd.playerrevive.api.DamageBledToDeath;
import com.creativemd.playerrevive.api.IRevival;
import com.creativemd.playerrevive.packet.ReviveUpdatePacket;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.server.FMLServerHandler;

public class ReviveEventServer {
	
	public static boolean isClient() {
		try {
			return FMLCommonHandler.instance().getSide().isClient();
		} catch (Throwable t) {
			return false;
		}
	}
	
	public static boolean isReviveActive() {
		if (PlayerRevive.CONFIG != null && PlayerRevive.CONFIG.enableInSingleplayer)
			return true;
		if (isClient())
			return !isSinglePlayer();
		return true;
	}
	
	@SideOnly(Side.CLIENT)
	private static boolean isSinglePlayer() {
		return Minecraft.getMinecraft().isSingleplayer() && !Minecraft.getMinecraft().getIntegratedServer().getPublic();
	}
	
	public static MinecraftServer getMinecraftServer() {
		MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance();
		if (server != null)
			return server;
		if (isClient())
			return getMinecraftServerClient();
		return FMLServerHandler.instance().getServer();
	}
	
	@SideOnly(Side.CLIENT)
	private static MinecraftServer getMinecraftServerClient() {
		return Minecraft.getMinecraft().getIntegratedServer();
	}
	
	//private static MinecraftServer server = FMLServerHandler.instance().getServer();
	
	@SubscribeEvent
	public void playerTick(PlayerTickEvent event) {
		if (event.phase == Phase.START && event.side == Side.SERVER && isReviveActive()) {
			EntityPlayer player = event.player;
			if (player.isDead)
				return;
			IRevival revive = PlayerReviveServer.getRevival(player);
			
			if (!revive.isHealty()) {
				revive.tick();
				
				player.motionX = 0;
				player.motionZ = 0;
				if (player.motionY > 0)
					player.motionY = 0;
				player.velocityChanged = true;
				
				if (player instanceof EntityPlayerMP && !(player.openContainer instanceof ContainerSub)) {
					GuiHandler.openGui("plrevive", new NBTTagCompound(), (EntityPlayerMP) player);
				}
				
				if (player.ticksExisted % 20 == 0)
					PlayerReviveServer.sendUpdatePacket(player);
				
				player.getFoodStats().setFoodLevel(PlayerRevive.CONFIG.playerFoodAfter);
				if (net.minecraftforge.fml.common.Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
					com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, (float) PlayerRevive.CONFIG.playerHealthAfter);
				} else {
					player.setHealth(PlayerRevive.CONFIG.playerHealthAfter);
				}
				player.capabilities.disableDamage = true;
				player.setEntityInvulnerable(true);
				
				if (revive.isRevived())
					PlayerReviveServer.revive(player);
				else if (revive.isDead())
					PlayerReviveServer.kill(player);
			}
		}
	}
	
	@SubscribeEvent
	public void playerLeave(PlayerLoggedOutEvent event) {
		IRevival revive = PlayerReviveServer.getRevival(event.player);
		if (!revive.isHealty())
			PlayerReviveServer.kill(event.player);
		if (!event.player.world.isRemote)
			PlayerReviveServer.removePlayerAsHelper(event.player);
	}
	
	/* @SubscribeEvent
	 * public void playerJoin(PlayerLoggedInEvent event)
	 * {
	 * if(!isReviveActive())
	 * return ;
	 * Revival revive = PlayerReviveServer.playerRevivals.get(EntityPlayer.getUUID(event.player.getGameProfile()));
	 * if(revive != null)
	 * PacketHandler.sendPacketToPlayer(new PlayerRevivalPacket(revive), (EntityPlayerMP) event.player);
	 * } */
	
	@SubscribeEvent(priority = EventPriority.HIGH)
	public void playerInteract(PlayerInteractEvent.EntityInteract event) {
		if (!PlayerReviveServer.isPlayerBleeding(event.getEntityPlayer()) && event.getTarget() instanceof EntityPlayer && !event.getEntityLiving().world.isRemote) {
			EntityPlayer player = (EntityPlayer) event.getTarget();
			IRevival revive = PlayerReviveServer.getRevival(player);
			if (!revive.isHealty()) {
				NBTTagCompound nbt = new NBTTagCompound();
				nbt.setString("uuid", EntityPlayer.getUUID(player.getGameProfile()).toString());
				revive.getRevivingPlayers().add(event.getEntityPlayer());
				GuiHandler.openGui("plreviver", nbt, event.getEntityPlayer());
				event.setCanceled(true);
			}
		}
	}
	
	public static boolean isDamageFromAnotherEntity(DamageSource source, EntityPlayer player) {
		if (source == null)
			return false;
		
		String type = source.getDamageType();
		if ("starve".equals(type) || "cactus".equals(type) || "magic".equals(type) || "wither".equals(type)
				|| "inFire".equals(type) || "onFire".equals(type) || "lava".equals(type) || "hotFloor".equals(type)
				|| "fall".equals(type) || "drown".equals(type) || "inWall".equals(type) || "cramming".equals(type)
				|| "flyIntoWall".equals(type) || "outOfWorld".equals(type) || "generic".equals(type)
				|| "lightningBolt".equals(type)) {
			return false;
		}
		
		Entity trueSource = source.getTrueSource();
		if (trueSource != null) {
			return trueSource != player;
		}
		
		Entity immediateSource = source.getImmediateSource();
		if (immediateSource != null) {
			return immediateSource != player;
		}
		
		return false;
	}
	
	@SubscribeEvent
	public void playerDamage(LivingHurtEvent event) {
		if (event.getEntityLiving() instanceof EntityPlayer) {
			EntityPlayer player = (EntityPlayer) event.getEntityLiving();
			IRevival revive = PlayerReviveServer.getRevival(player);
			if (!revive.isHealty() && ((event.getSource() != DamageBledToDeath.bledToDeath && !event.getSource().canHarmInCreative() && !PlayerRevive.CONFIG.bypassDamageSources.contains(event.getSource().damageType)) || revive.isDead())) {
				event.setCanceled(true);
				return;
			}
			
			if (!event.isCanceled() && !player.world.isRemote && event.getAmount() > 0) {
				EntityPlayer bleedingPlayer = PlayerReviveServer.getBleedingPlayerForHelper(player);
				if (bleedingPlayer != null) {
					boolean isFromEntity = isDamageFromAnotherEntity(event.getSource(), player);
					
					boolean applyMultiplier = !PlayerRevive.CONFIG.multiplyOnlyEntityDamage || isFromEntity;
					if (applyMultiplier) {
						float min = PlayerRevive.CONFIG.damageMultiplierMin;
						float max = PlayerRevive.CONFIG.damageMultiplierMax;
						if (max < min) {
							float temp = min;
							min = max;
							max = temp;
						}
						float multiplier = min;
						if (max > min) {
							multiplier = min + (float) (Math.random() * (max - min));
						}
						if (multiplier > 1.0F) {
							event.setAmount(event.getAmount() * multiplier);
						}
					}
					
					if (PlayerRevive.CONFIG.resetProgressOnDamage && isFromEntity) {
						IRevival bleedingRevival = PlayerReviveServer.getRevival(bleedingPlayer);
						if (bleedingRevival instanceof com.creativemd.playerrevive.Revival) {
							((com.creativemd.playerrevive.Revival) bleedingRevival).resetProgress();
							PlayerReviveServer.sendUpdatePacket(bleedingPlayer);
							PlayerReviveServer.forceSyncContainers(bleedingPlayer, player);
						}
					}
				}
			}
		}
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void playerDied(LivingDeathEvent event) {
		if (event.isCanceled())
			return;
		if (event.getEntityLiving() instanceof EntityPlayer && isReviveActive() && !event.getEntityLiving().world.isRemote && event.getSource() != DamageBledToDeath.bledToDeath && !event.getSource().canHarmInCreative() && !PlayerRevive.CONFIG.bypassDamageSources.contains(event.getSource().damageType)) {
			EntityPlayer player = (EntityPlayer) event.getEntityLiving();
			IRevival revive = PlayerReviveServer.getRevival(player);
			
			if (revive.isDead()) {
				revive.stopBleeding();
				return;
			}
			
			if (PlayerReviveServer.checkTotemOrPotionResurrection(player, event.getSource())) {
				event.setCanceled(true);
				return;
			}
			
			PlayerReviveServer.startBleeding(player, event.getSource());
			player.capabilities.disableDamage = true;
			player.setEntityInvulnerable(true);
			
			if (player.isRiding())
				player.dismountRidingEntity();
			
			event.setCanceled(true);
			
			if (net.minecraftforge.fml.common.Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
				com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setWaitingForHelp(player, true);
				com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.ensureCriticalPartsAlive(player, 1.0F);
				com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, (float) PlayerRevive.CONFIG.playerHealthAfter);
				com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.syncDamageModel(player);
			} else {
				player.setHealth((float) PlayerRevive.CONFIG.playerHealthAfter);
			}
			player.getFoodStats().setFoodLevel(1);
			
			if (player instanceof EntityPlayerMP) {
				GuiHandler.openGui("plrevive", new NBTTagCompound(), (EntityPlayerMP) player);
			}
			PlayerReviveServer.sendUpdatePacket(player);
			
			if (!PlayerRevive.CONFIG.disableBleedingMessage)
				player.getServer().getPlayerList().sendMessage(new TextComponentTranslation("playerrevive.chat.bleeding", player.getDisplayNameString()));
		}
	}
	
	@SubscribeEvent
	public void onStartTracking(net.minecraftforge.event.entity.player.PlayerEvent.StartTracking event) {
		if (event.getTarget() instanceof EntityPlayer && event.getEntityPlayer() instanceof EntityPlayerMP) {
			EntityPlayer target = (EntityPlayer) event.getTarget();
			IRevival revive = PlayerReviveServer.getRevival(target);
			if (revive != null && !revive.isHealty()) {
				ReviveUpdatePacket packet = new ReviveUpdatePacket(target);
				if (PlayerRevive.NETWORK != null) {
					PlayerRevive.NETWORK.sendTo(packet, (EntityPlayerMP) event.getEntityPlayer());
				}
			}
		}
	}
	
	@SubscribeEvent
	public void attachCapability(AttachCapabilitiesEvent<Entity> event) {
		if (event.getObject() instanceof EntityPlayer) {
			event.addCapability(new ResourceLocation(PlayerRevive.modid, "revive"), new CapaReviveProvider());
		}
	}
	
}
