package com.creativemd.playerrevive.client;

import com.creativemd.playerrevive.packet.ReviveUpdatePacket;
import com.creativemd.playerrevive.server.PlayerReviveServer;

import net.minecraft.client.Minecraft;
import net.minecraftforge.common.MinecraftForge;

public class PlayerReviveClient extends PlayerReviveServer {
	
	@Override
	public void loadSide() {
		MinecraftForge.EVENT_BUS.register(new ReviveEventClient());
	}
	
	@Override
	public void handleClientPacket(ReviveUpdatePacket message) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			message.executeClient(Minecraft.getMinecraft().player);
		});
	}
	
}
