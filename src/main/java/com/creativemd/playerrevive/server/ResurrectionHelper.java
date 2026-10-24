package com.creativemd.playerrevive.server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import com.creativemd.playerrevive.PlayerRevive;
import com.creativemd.playerrevive.api.DamageBledToDeath;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.stats.StatList;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class ResurrectionHelper {

	public static boolean checkTotemOrPotionResurrection(EntityPlayer player, DamageSource source) {
		if (player == null || player.world == null || player.world.isRemote) {
			return false;
		}
		if (source == DamageBledToDeath.bledToDeath) {
			return false;
		}
		if (source != null && PlayerRevive.CONFIG.bypassDamageSources.contains(source.damageType)) {
			return false;
		}

		// 1. Check Totem of Undying (first, matching vanilla order)
		if (PlayerRevive.CONFIG.enableTotemCompatibility && checkTotem(player, source)) {
			return true;
		}

		// 2. Check PotionCore Revival
		if (PlayerRevive.CONFIG.enablePotionCoreRevivalCompatibility && checkPotionCoreRevival(player, source)) {
			return true;
		}

		return false;
	}

	public static boolean checkTotem(EntityPlayer player, DamageSource source) {
		if (source != null && source.canHarmInCreative()) {
			return false;
		}

		ItemStack totemStack = ItemStack.EMPTY;
		for (EnumHand hand : EnumHand.values()) {
			ItemStack held = player.getHeldItem(hand);
			if (!held.isEmpty() && held.getItem() == Items.TOTEM_OF_UNDYING) {
				totemStack = held.copy();
				held.shrink(1);
				break;
			}
		}

		if (!totemStack.isEmpty()) {
			if (player instanceof EntityPlayerMP) {
				EntityPlayerMP mp = (EntityPlayerMP) player;
				mp.addStat(StatList.getObjectUseStats(Items.TOTEM_OF_UNDYING));
				CriteriaTriggers.USED_TOTEM.trigger(mp, totemStack);
			}

			if (Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
				com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, 1.0F);
				com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.onResurrected(player);
			} else {
				player.setHealth(1.0F);
			}

			player.clearActivePotions();
			player.addPotionEffect(new PotionEffect(MobEffects.REGENERATION, 900, 1));
			player.addPotionEffect(new PotionEffect(MobEffects.ABSORPTION, 100, 1));
			player.world.setEntityState(player, (byte) 35);
			return true;
		}

		return false;
	}

	public static boolean checkPotionCoreRevival(EntityPlayer player, DamageSource source) {
		if (source != null && source.canHarmInCreative()) {
			return false;
		}

		Potion revivalPotion = ForgeRegistries.POTIONS.getValue(new ResourceLocation("potioncore", "revival"));
		if (revivalPotion == null) {
			revivalPotion = Potion.getPotionFromResourceLocation("potioncore:revival");
		}
		if (revivalPotion == null || !player.isPotionActive(revivalPotion)) {
			return false;
		}

		try {
			Method isEnabledMethod = revivalPotion.getClass().getMethod("isEnabled");
			if (isEnabledMethod != null) {
				Boolean enabled = (Boolean) isEnabledMethod.invoke(revivalPotion);
				if (enabled != null && !enabled) {
					return false;
				}
			}
		} catch (Throwable ignored) {}

		PotionEffect effect = player.getActivePotionEffect(revivalPotion);
		int level = effect != null ? effect.getAmplifier() + 1 : 1;

		float reviveHealth = 4.0F;
		try {
			Field field = revivalPotion.getClass().getField("reviveHealth");
			reviveHealth = field.getFloat(null);
		} catch (Throwable ignored) {}

		// Consume the potion effect
		player.removePotionEffect(revivalPotion);

		// Restore player health
		float targetHealth = Math.min(player.getMaxHealth(), reviveHealth * (float) level);
		if (Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.setPlayerHealthDirect(player, Math.max(1.0F, targetHealth));
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.onResurrected(player);
		} else {
			player.setHealth(Math.max(1.0F, targetHealth));
		}

		// Play sound
		player.world.playSound(null, player.posX, player.posY, player.posZ, SoundEvents.ITEM_TOTEM_USE, player.getSoundCategory(), 1.0F, 0.6F);

		// Spawn heart particles
		if (player.world instanceof WorldServer) {
			WorldServer ws = (WorldServer) player.world;
			ws.spawnParticle(EnumParticleTypes.HEART, false, player.posX, player.posY + player.getEyeHeight() / 2.0, player.posZ, 16, 0.5, 0.5, 0.5, 0.1);
		}

		// Send PotionCore packet to tracking clients if PotionCore is loaded
		try {
			Class<?> packetClass = Class.forName("com.tmtravlr.potioncore.network.SToCMessage");
			Class<?> potionCoreClass = Class.forName("com.tmtravlr.potioncore.PotionCore");
			Object wrapper = potionCoreClass.getField("networkWrapper").get(null);
			if (wrapper instanceof SimpleNetworkWrapper) {
				PacketBuffer out = new PacketBuffer(Unpooled.buffer());
				out.writeInt(3); // REVIVAL_HEARTS
				out.writeInt(player.getEntityId());
				Object packet = packetClass.getConstructor(ByteBuf.class).newInstance(out);
				((SimpleNetworkWrapper) wrapper).sendToAllAround(
					(IMessage) packet,
					new NetworkRegistry.TargetPoint(player.world.provider.getDimension(), player.posX, player.posY, player.posZ, 16.0)
				);
			}
		} catch (Throwable ignored) {}

		onResurrected(player);
		return true;
	}

	public static void onResurrected(EntityPlayer player) {
		if (Loader.isModLoaded("firstaid") && PlayerRevive.CONFIG.enableFirstAidCompatibility) {
			com.creativemd.playerrevive.compat.firstaid.FirstAidHelper.onResurrected(player);
		}
	}
}
