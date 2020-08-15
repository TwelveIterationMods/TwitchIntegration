package net.blay09.mods.twitchintegration.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.gui.screen.ConfirmOpenLinkScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TwitchOpenTokenScreen extends ConfirmOpenLinkScreen {

    private boolean hasBrowseErrored;
    private String actualLink;
    private float errorColorFadeTimer;
    private TextFormatting errorColor = TextFormatting.RED;

    public TwitchOpenTokenScreen(BooleanConsumer callback, String actualLink) {
        super(callback, getInfoText(), true);
        this.actualLink = actualLink;
        // TODO this.title = new TranslationTextComponent("twitchchatintegration:gui.openToken.authorize");
    }

    private static String getInfoText() {
        return I18n.format("twitchchatintegration:gui.openToken.requiredPermissions") + "\n" + TextFormatting.GRAY + I18n.format("twitchchatintegration:gui.openToken.logIntoChat") + "\n\n" + TextFormatting.RESET + I18n.format("twitchchatintegration:gui.openToken.openedInBrowser");
    }

    @Override
    public void copyLinkToClipboard() {
        super.copyLinkToClipboard();

        buttons.get(1).setMessage(new TranslationTextComponent("twitchchatintegration:gui.openToken.copied"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        if (hasBrowseErrored) {
            errorColorFadeTimer += partialTicks;
            if (errorColorFadeTimer > 15f) {
                errorColor = errorColor == TextFormatting.RED ? TextFormatting.DARK_RED : TextFormatting.RED;
                errorColorFadeTimer = 0f;
            }
            drawCenteredString(matrixStack, font, errorColor + I18n.format("twitchintegration.gui.browse_failed"), width / 2, height / 2 + 50, 0xFFFFFFFF);
            drawCenteredString(matrixStack, font, errorColor + I18n.format("twitchintegration.gui.browse_failed_hint"), width / 2, height / 2 + 64, 0xFFFFFFFF);
        } else {
            drawCenteredString(matrixStack, font, TextFormatting.RED + I18n.format("twitchintegration.gui.no_leak_pls"), width / 2, height / 2 + 50, 0xFFFFFFFF);
        }
    }

    public void showBrowseErrorHints() {
        hasBrowseErrored = true;
        buttons.get(0).active = false;
    }

}
