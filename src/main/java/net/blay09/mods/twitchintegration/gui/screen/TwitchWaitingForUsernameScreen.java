package net.blay09.mods.twitchintegration.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.text.TextFormatting;

public class TwitchWaitingForUsernameScreen extends Screen {

    public TwitchWaitingForUsernameScreen() {
        super(Messages.lang("gui.awaitingResponse.pleaseWait"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        final FontRenderer font = Minecraft.getInstance().fontRenderer;
        drawCenteredString(matrixStack, font, Messages.format("gui.awaitingResponse.status", TextFormatting.YELLOW + Messages.format("gui.awaitingResponse.requestingUsername")), width / 2, height / 2 - 20, 0xFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.GRAY + Messages.format("gui.awaitingResponse.pleaseWait"), width / 2, height / 2 + 10, 0xFFFFFF);
    }

}
