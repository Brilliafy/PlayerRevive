package com.creativemd.playerrevive.packet;

import java.util.UUID;

import com.creativemd.creativecore.common.gui.mc.ContainerSub;
import com.creativemd.creativecore.common.gui.opener.GuiHandler;
import com.creativemd.creativecore.common.packet.CreativeCorePacket;
import com.creativemd.playerrevive.PlayerRevive;
import com.creativemd.playerrevive.api.IRevival;
import com.creativemd.playerrevive.server.PlayerReviveServer;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ReviveUpdatePacket extends CreativeCorePacket implements IMessage {
	
	public EntityPlayer player;
	public UUID uuid;
	public NBTTagCompound nbt;
	
	public ReviveUpdatePacket(EntityPlayer player) {
		this.player = player;
		if (player != null) {
			this.uuid = player.getUniqueID();
			IRevival rev = PlayerReviveServer.getRevival(player);
			if (rev != null) {
				this.nbt = rev.serializeNBT();
			} else {
				this.nbt = new NBTTagCompound();
			}
		}
	}
	
	public ReviveUpdatePacket(UUID uuid, NBTTagCompound nbt) {
		this.uuid = uuid;
		this.nbt = nbt;
	}
	
	public ReviveUpdatePacket() {
		
	}
	
	@Override
	public void writeBytes(ByteBuf buf) {
		if (this.nbt == null && player != null) {
			IRevival rev = PlayerReviveServer.getRevival(player);
			if (rev != null) {
				this.nbt = rev.serializeNBT();
			} else {
				this.nbt = new NBTTagCompound();
			}
		}
		if (this.uuid == null && player != null) {
			this.uuid = player.getUniqueID();
		}
		writeNBT(buf, this.nbt != null ? this.nbt : new NBTTagCompound());
		writeString(buf, this.uuid != null ? this.uuid.toString() : "");
	}
	
	@Override
	public void readBytes(ByteBuf buf) {
		nbt = readNBT(buf);
		String str = readString(buf);
		if (str != null && !str.isEmpty()) {
			uuid = UUID.fromString(str);
		}
	}
	
	public void toBytes(ByteBuf buf) {
		writeBytes(buf);
	}
	
	public void fromBytes(ByteBuf buf) {
		readBytes(buf);
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public void executeClient(EntityPlayer player) {
		if (uuid == null || nbt == null)
			return;
		Minecraft mc = Minecraft.getMinecraft();
		EntityPlayer member = null;
		if (player != null && uuid.equals(player.getUniqueID())) {
			member = player;
		} else if (mc.player != null && uuid.equals(mc.player.getUniqueID())) {
			member = mc.player;
		} else if (mc.world != null) {
			member = mc.world.getPlayerEntityByUUID(uuid);
		}
		
		if (member != null) {
			IRevival rev = PlayerReviveServer.getRevival(member);
			if (rev != null) {
				rev.deserializeNBT(nbt);
			}
			
			if (member == mc.player && rev != null && !rev.isHealty()) {
				if (!(mc.player.openContainer instanceof ContainerSub)) {
					GuiHandler.openGui("plrevive", new NBTTagCompound());
				}
			}
		}
	}
	
	@Override
	public void executeServer(EntityPlayer player) {
		
	}
	
	public static class Handler implements IMessageHandler<ReviveUpdatePacket, IMessage> {
		
		@Override
		public IMessage onMessage(ReviveUpdatePacket message, MessageContext ctx) {
			if (ctx.side.isClient()) {
				PlayerRevive.proxy.handleClientPacket(message);
			}
			return null;
		}
	}
	
}
