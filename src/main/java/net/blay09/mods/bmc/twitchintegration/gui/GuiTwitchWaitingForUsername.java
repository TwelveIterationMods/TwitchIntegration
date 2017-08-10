package net.blay09.mods.bmc.twitchintegration.gui;

import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class GuiTwitchWaitingForUsername extends GuiScreen {

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		drawCenteredString(this.mc.fontRenderer, I18n.format("twitchintegration:gui.awaitingResponse.status", TextFormatting.YELLOW + I18n.format("twitchintegration:gui.awaitingResponse.requestingUsername")), width / 2, height / 2 - 20, 0xFFFFFF);
		drawCenteredString(mc.fontRenderer, TextFormatting.GRAY + I18n.format("twitchintegration:gui.awaitingResponse.pleaseWait"), width / 2, height / 2 + 10, 0xFFFFFF);
	}

}
