package net.blay09.mods.twitchintegration.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.gui.DialogTexts;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TwitchOpenTokenScreen extends Screen {

    private final BooleanConsumer callback;
    private final String link;

    private boolean hasBrowseErrored;
    private TextFormatting errorColor = TextFormatting.RED;
    private float errorColorFadeTimer;
    private Button copyLinkButton;
    private Button openLinkButton;

    public TwitchOpenTokenScreen(BooleanConsumer callback, String link) {
        super(Messages.lang("gui.openToken.authorize"));
        this.callback = callback;
        this.link = link;
    }

    @Override
    protected void init() {
        super.init();

        openLinkButton = addButton(new Button(this.width / 2 - 50 - 105, this.height / 6 + 96, 100, 20, new TranslationTextComponent("chat.link.open"), (button) -> {
            callback.accept(true);
        }));

        copyLinkButton = addButton(new Button(this.width / 2 - 50, this.height / 6 + 96, 100, 20, new TranslationTextComponent("chat.copy"), (button) -> {
            copyLinkToClipboard();
        }));

        addButton(new Button(this.width / 2 - 50 + 105, this.height / 6 + 96, 100, 20, DialogTexts.field_240633_d_, (button) -> {
            callback.accept(false);
        }));
    }

    private void copyLinkToClipboard() {
        getMinecraft().keyboardListener.setClipboardString(link);

        copyLinkButton.setMessage(Messages.lang("gui.openToken.copied"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);

        drawCenteredString(matrixStack, font, title, width / 2, height / 2 - 70, 0xFFFFFFFF);

        if (hasBrowseErrored) {
            errorColorFadeTimer += partialTicks;
            if (errorColorFadeTimer > 15f) {
                errorColor = errorColor == TextFormatting.RED ? TextFormatting.DARK_RED : TextFormatting.RED;
                errorColorFadeTimer = 0f;
            }
            drawCenteredString(matrixStack, font, errorColor + Messages.format("gui.browse_failed"), width / 2, height / 2 + 50, 0xFFFFFFFF);
            drawCenteredString(matrixStack, font, errorColor + Messages.format("gui.browse_failed_hint"), width / 2, height / 2 + 64, 0xFFFFFFFF);
        } else {
            drawCenteredString(matrixStack, font, TextFormatting.RED + Messages.format("gui.no_leak_pls"), width / 2, height / 2 + 50, 0xFFFFFFFF);
        }

        drawCenteredString(matrixStack, font, Messages.format("gui.openToken.requiredPermissions"), width / 2, height / 2 - 47, 0xFFFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.GRAY + Messages.format("gui.openToken.logIntoChat"), width / 2, height / 2 - 35, 0xFFFFFFFF);
        drawCenteredString(matrixStack, font, Messages.format("gui.openToken.openedInBrowser"), width / 2, height / 2 - 15, 0xFFFFFFFF);
    }

    public void showBrowseErrorHints() {
        hasBrowseErrored = true;
        openLinkButton.active = false;
    }

}
