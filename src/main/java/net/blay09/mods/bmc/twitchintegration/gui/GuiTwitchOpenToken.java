package net.blay09.mods.bmc.twitchintegration.gui;

import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class GuiTwitchOpenToken extends GuiConfirmOpenLink {
	public GuiTwitchOpenToken(GuiYesNoCallback callback, int i) {
		super(callback, I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.requiredPermissions") + "\n" + TextFormatting.GRAY + I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.logIntoChat") + "\n\n" + TextFormatting.RESET + I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.openedInBrowser"), i, true);
		messageLine1 = I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.authorize");
		disableSecurityWarning();
	}

	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		super.drawScreen(mouseX, mouseY, partialTicks);
		drawCenteredString(fontRenderer, TextFormatting.RED + "Please do not show your browser on stream during authorization.", width / 2, height / 2 + 50, 0xFFFFFFFF); // TODO i18n
	}
}
