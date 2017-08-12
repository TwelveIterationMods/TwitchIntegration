package net.blay09.mods.bmc.twitchintegration.gui;

import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class GuiTwitchWaitingForToken extends GuiScreen {

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		drawCenteredString(mc.fontRenderer, I18n.format(TwitchIntegration.MOD_ID + ":gui.awaitingResponse.status", TextFormatting.YELLOW + I18n.format(TwitchIntegration.MOD_ID + ":gui.awaitingResponse.awaitingAuthorization")), width / 2, height / 2 - 20, 0xFFFFFF);
		drawCenteredString(mc.fontRenderer, TextFormatting.GRAY + I18n.format(TwitchIntegration.MOD_ID + ":gui.awaitingResponse.followInstructions"), width / 2, height / 2 + 10, 0xFFFFFF);
		drawCenteredString(fontRenderer, TextFormatting.RED + I18n.format("twitchintegration.gui.no_leak_pls"), width / 2, height / 2 + 50, 0xFFFFFFFF);
	}

}
