package net.blay09.mods.twitchintegration.gui.screen;

import com.mojang.blaze3d.matrix.MatrixStack;
import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TwitchWaitingForTokenScreen extends Screen {

    protected TwitchWaitingForTokenScreen() {
        super(new TranslationTextComponent("twitchchatintegration:gui.awaitingResponse.status"));
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        super.render(matrixStack, mouseX, mouseY, partialTicks);
        drawCenteredString(matrixStack, font, I18n.format(TwitchChatIntegration.MOD_ID + ":gui.awaitingResponse.status", TextFormatting.YELLOW + I18n.format(TwitchChatIntegration.MOD_ID + ":gui.awaitingResponse.awaitingAuthorization")), width / 2, height / 2 - 20, 0xFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.GRAY + I18n.format(TwitchChatIntegration.MOD_ID + ":gui.awaitingResponse.followInstructions"), width / 2, height / 2 + 10, 0xFFFFFF);
        drawCenteredString(matrixStack, font, TextFormatting.RED + I18n.format("twitchintegration.gui.no_leak_pls"), width / 2, height / 2 + 50, 0xFFFFFFFF);
    }

}
