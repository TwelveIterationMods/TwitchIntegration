package net.blay09.mods.twitchintegration.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.TextFormatting;

public class TwitchWaitingForTokenScreen extends Screen {

    protected TwitchWaitingForTokenScreen() {
        super(Messages.lang("gui.awaitingResponse.status"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, font, Messages.format("gui.awaitingResponse.status", TextFormatting.YELLOW + Messages.format("gui.awaitingResponse.awaitingAuthorization")), width / 2, height / 2 - 20, 0xFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.GRAY + Messages.format("gui.awaitingResponse.followInstructions"), width / 2, height / 2 + 10, 0xFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.RED + Messages.format("gui.no_leak_pls"), width / 2, height / 2 + 50, 0xFFFFFFFF);
    }

}
