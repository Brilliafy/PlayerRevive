package com.creativemd.playerrevive.compat.firstaid;

import java.lang.reflect.Field;

import ichttt.mods.firstaid.FirstAid;
import ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem;
import ichttt.mods.firstaid.api.damagesystem.AbstractDamageablePart;
import ichttt.mods.firstaid.api.damagesystem.AbstractPlayerDamageModel;
import ichttt.mods.firstaid.common.DataManagerWrapper;
import ichttt.mods.firstaid.common.network.MessageSyncDamageModel;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.datasync.DataParameter;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.relauncher.ReflectionHelper;

public class FirstAidHelper {

	private static DataParameter<Float> healthParam = null;
	private static boolean healthParamLookup = false;

	public static boolean isFirstAidLoaded() {
		return Loader.isModLoaded("firstaid");
	}

	@SuppressWarnings("unchecked")
	public static DataParameter<Float> getHealthParam() {
		if (!healthParamLookup) {
			healthParamLookup = true;
			try {
				Field f = ReflectionHelper.findField(EntityLivingBase.class, "HEALTH", "field_184632_c");
				f.setAccessible(true);
				healthParam = (DataParameter<Float>) f.get(null);
			} catch (Throwable ignored) {
				healthParam = null;
			}
		}
		return healthParam;
	}

	public static AbstractPlayerDamageModel getDamageModel(EntityPlayer player) {
		if (!isFirstAidLoaded() || player == null) {
			return null;
		}
		try {
			if (CapabilityExtendedHealthSystem.INSTANCE != null && player.hasCapability(CapabilityExtendedHealthSystem.INSTANCE, null)) {
				return player.getCapability(CapabilityExtendedHealthSystem.INSTANCE, null);
			}
		} catch (Throwable ignored) {}
		return null;
	}

	public static void setWaitingForHelp(EntityPlayer player, boolean waiting) {
		AbstractPlayerDamageModel model = getDamageModel(player);
		if (model != null) {
			setWaitingForHelp(model, waiting);
			if (!waiting) {
				try {
					model.stopWaitingForHelp(player);
				} catch (Throwable ignored) {}
			}
		}
	}

	public static void setWaitingForHelp(AbstractPlayerDamageModel model, boolean waiting) {
		if (model == null)
			return;
		try {
			Field field = model.getClass().getDeclaredField("waitingForHelp");
			field.setAccessible(true);
			field.setBoolean(model, waiting);
		} catch (Throwable t) {
			try {
				Field field = AbstractPlayerDamageModel.class.getDeclaredField("waitingForHelp");
				field.setAccessible(true);
				field.setBoolean(model, waiting);
			} catch (Throwable ignored) {}
		}
	}

	public static void ensureCriticalPartsAlive(EntityPlayer player, float minHealth) {
		AbstractPlayerDamageModel model = getDamageModel(player);
		if (model != null) {
			boolean changed = false;
			boolean hasCritical = false;
			for (AbstractDamageablePart part : model) {
				if (part.canCauseDeath) {
					hasCritical = true;
					if (part.currentHealth < minHealth) {
						part.currentHealth = minHealth;
						changed = true;
					}
				}
			}
			if (!hasCritical) {
				for (AbstractDamageablePart part : model) {
					if (part.currentHealth < minHealth) {
						part.currentHealth = minHealth;
						changed = true;
						break;
					}
				}
			}
			if (changed) {
				syncDamageModel(player, model);
			}
		}
	}

	public static void revivePlayer(EntityPlayer player) {
		AbstractPlayerDamageModel model = getDamageModel(player);
		if (model != null) {
			model.revivePlayer(player);
			try {
				model.stopWaitingForHelp(player);
			} catch (Throwable ignored) {}
			setWaitingForHelp(model, false);
			ensureCriticalPartsAlive(player, 1.0F);
			syncDamageModel(player, model);
		}
	}

	public static void onResurrected(EntityPlayer player) {
		revivePlayer(player);
	}

	public static void syncDamageModel(EntityPlayer player) {
		AbstractPlayerDamageModel model = getDamageModel(player);
		if (model != null) {
			syncDamageModel(player, model);
		}
	}

	public static void syncDamageModel(EntityPlayer player, AbstractPlayerDamageModel model) {
		if (player != null && !player.world.isRemote && player instanceof EntityPlayerMP) {
			try {
				FirstAid.NETWORKING.sendTo(new MessageSyncDamageModel(model, true), (EntityPlayerMP) player);
			} catch (Throwable ignored) {}
		}
	}

	public static void toggleTracking(EntityPlayer player, boolean enabled) {
		if (player != null && player.getDataManager() instanceof DataManagerWrapper) {
			try {
				((DataManagerWrapper) player.getDataManager()).toggleTracking(enabled);
			} catch (Throwable ignored) {}
		}
	}

	public static void setPlayerHealthDirect(EntityPlayer player, float health) {
		if (player == null)
			return;
		if (player.getDataManager() instanceof DataManagerWrapper) {
			DataManagerWrapper wrapper = (DataManagerWrapper) player.getDataManager();
			DataParameter<Float> param = getHealthParam();
			if (param != null) {
				wrapper.set_impl(param, Float.valueOf(health));
				return;
			}
		}
		player.setHealth(health);
	}
}
