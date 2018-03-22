package net.blay09.mods.twitchintegration.gui;

import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.minecraft.client.gui.GuiConfirmOpenLink;
import net.minecraft.client.gui.GuiYesNoCallback;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;

public class GuiTwitchOpenToken extends GuiConfirmOpenLink {

    private boolean hasBrowseErrored;
    private String actualLink;
    private float errorColorFadeTimer;
    private TextFormatting errorColor = TextFormatting.RED;

    public GuiTwitchOpenToken(GuiYesNoCallback callback, int i, String actualLink) {
        super(callback, getInfoText(), i, true);
        this.actualLink = actualLink;
        messageLine1 = I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.authorize");
        disableSecurityWarning();
    }

    private static String getInfoText() {
        return I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.requiredPermissions") + "\n" + TextFormatting.GRAY + I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.logIntoChat") + "\n\n" + TextFormatting.RESET + I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.openedInBrowser");
    }

    @Override
    public void copyLinkToClipboard() {
        setClipboardString(actualLink);

        buttonList.stream().filter(p -> p.id == 2).findAny().ifPresent(p -> p.displayString = I18n.format(TwitchIntegration.MOD_ID + ":gui.openToken.copied"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        if (hasBrowseErrored) {
            errorColorFadeTimer += partialTicks;
            if (errorColorFadeTimer > 15f) {
                errorColor = errorColor == TextFormatting.RED ? TextFormatting.DARK_RED : TextFormatting.RED;
                errorColorFadeTimer = 0f;
            }
            drawCenteredString(fontRenderer, errorColor + I18n.format("twitchintegration.gui.browse_failed"), width / 2, height / 2 + 50, 0xFFFFFFFF);
            drawCenteredString(fontRenderer, errorColor + I18n.format("twitchintegration.gui.browse_failed_hint"), width / 2, height / 2 + 64, 0xFFFFFFFF);
        } else {
            drawCenteredString(fontRenderer, TextFormatting.RED + I18n.format("twitchintegration.gui.no_leak_pls"), width / 2, height / 2 + 50, 0xFFFFFFFF);
        }
    }

    public void enableBrowseErrorHints() {
        hasBrowseErrored = true;
        buttonList.stream().filter(p -> p.id == 0).findAny().ifPresent(p -> p.enabled = false);
    }

}
