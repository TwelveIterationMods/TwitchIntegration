package net.blay09.mods.twitchintegration.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TwitchWaitingForUsernameScreen extends Screen {

    public TwitchWaitingForUsernameScreen() {
        super(new TranslationTextComponent("twitchintegration:gui.awaitingResponse.pleaseWait"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        final FontRenderer font = Minecraft.getInstance().fontRenderer;
        drawCenteredString(matrixStack, font, I18n.format("twitchintegration:gui.awaitingResponse.status", TextFormatting.YELLOW + I18n.format("twitchintegration:gui.awaitingResponse.requestingUsername")), width / 2, height / 2 - 20, 0xFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.GRAY + I18n.format("twitchintegration:gui.awaitingResponse.pleaseWait"), width / 2, height / 2 + 10, 0xFFFFFF);
    }

}
