package net.blay09.mods.twitchintegration.gui;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.twitchintegration.TwitchManager;
import net.blay09.mods.twitchintegration.auth.TwitchAuthManager;
import net.blay09.mods.twitchintegration.auth.TwitchAuthToken;
import net.blay09.mods.twitchintegration.util.Messages;
import net.blay09.mods.twitchintegration.util.TwitchAPI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.button.Button;
import net.minecraft.client.gui.widget.button.CheckboxButton;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import java.util.Objects;

public class TwitchAuthenticationScreen extends Screen {

    private static final ResourceLocation twitchLogo = new ResourceLocation(TwitchChatIntegration.MOD_ID, "twitch_logo.png");

    private final Screen parentScreen;

    private Button btnGetToken;
    private PasswordFieldWidget tokenTextField;
    private Button btnConnect;

    public TwitchAuthenticationScreen(Screen parentScreen) {
        super(new TranslationTextComponent("twitchchatintegration:gui.authentication.generateToken"));
        this.parentScreen = parentScreen;
    }

    @Override
    public void init() {
        super.init();

        btnGetToken = new Button(width / 2 - 100, height / 2 - 25, 200, 20, Messages.lang("gui.authentication.generateToken", TextFormatting.GREEN), button -> {
            Minecraft.getInstance().displayGuiScreen(new TwitchOpenTokenScreen(success -> {
                final Minecraft mc = Minecraft.getInstance();
                if (success) {
                    if (TwitchAPI.listenForToken(() -> mc.displayGuiScreen(new TwitchAuthenticationScreen(parentScreen)))) {
                        mc.displayGuiScreen(new TwitchWaitingForTokenScreen());
                    } else {
                        if (mc.currentScreen instanceof TwitchOpenTokenScreen) {
                            ((TwitchOpenTokenScreen) mc.currentScreen).showBrowseErrorHints();
                        }
                    }
                } else {
                    mc.displayGuiScreen(this);
                }
            }, TwitchAPI.getAuthenticationURL()));
        });
        addButton(btnGetToken);

        tokenTextField = new PasswordFieldWidget(font, width / 2 - 100, height / 2 + 20, 200, 15, new StringTextComponent(""));
        TwitchAuthToken tokenPair = TwitchAuthManager.getAuthToken();
        if (tokenPair != null) {
            tokenTextField.setText(tokenPair.getToken());
        }
        tokenTextField.setEnabled(!TwitchIntegrationConfig.CLIENT.useAnonymousLogin.get());

        CheckboxButton chkAnonymous = new CheckboxButton(width / 2 - 100, height / 2 + 45, 16, 16, Messages.lang("gui.authentication.anonymousLogin"), TwitchIntegrationConfig.CLIENT.useAnonymousLogin.get()) {
            @Override
            public void onPress() {
                super.onPress();
                TwitchIntegrationConfig.CLIENT.useAnonymousLogin.set(isChecked());
            }
        };
        addButton(chkAnonymous);

        btnConnect = new Button(width / 2, height / 2 + 65, 100, 20, new TranslationTextComponent("twitchchatintegration:gui.authentication.connect"), button -> {
            TwitchAuthToken authToken = TwitchAuthManager.getAuthToken();
            if (!TwitchIntegrationConfig.CLIENT.useAnonymousLogin.get() && (authToken == null || !authToken.getToken().equals(tokenTextField.getText()) || authToken.getUsername() == null)) {
                Minecraft.getInstance().displayGuiScreen(new TwitchWaitingForUsernameScreen());
                TwitchAPI.requestUsername(tokenTextField.getText(), TwitchManager::connect);
            } else {
                if (TwitchManager.isConnected()) {
                    TwitchManager.disconnect();
                } else {
                    TwitchManager.connect();
                }
                Minecraft.getInstance().displayGuiScreen(parentScreen);
            }
        });

        if (TwitchManager.isConnected()) {
            btnConnect.setMessage(new TranslationTextComponent(TwitchChatIntegration.MOD_ID + ":gui.authentication.disconnect"));
        }

        addButton(btnConnect);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256) {
            Objects.requireNonNull(getMinecraft().player).closeScreen();
        }

        return tokenTextField.keyPressed(keyCode, scanCode, modifiers) || this.tokenTextField.canWrite() || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(MatrixStack matrixStack, int mouseX, int mouseY, float partialTicks) {
        final int windowHalfWidth = 150;
        final int windowHalfHeight = 100;
        fill(matrixStack, width / 2 - windowHalfWidth, height / 2 - windowHalfHeight, width / 2 + windowHalfWidth, height / 2 + windowHalfHeight, 0xDD000000);
        hLine(matrixStack, width / 2 - windowHalfWidth - 1, width / 2 + windowHalfWidth, height / 2 - windowHalfHeight - 1, 0xFFFFFFFF);
        hLine(matrixStack, width / 2 - windowHalfWidth - 1, width / 2 + windowHalfWidth, height / 2 + windowHalfHeight, 0xFFFFFFFF);
        vLine(matrixStack, width / 2 - windowHalfWidth - 1, height / 2 - windowHalfHeight - 1, height / 2 + windowHalfHeight, 0xFFFFFFFF);
        vLine(matrixStack, width / 2 + windowHalfWidth, height / 2 - windowHalfHeight - 1, height / 2 + windowHalfHeight, 0xFFFFFFFF);

        super.render(matrixStack, mouseX, mouseY, partialTicks);

        RenderSystem.color4f(1f, 1f, 1f, 1f);
        Minecraft.getInstance().getTextureManager().bindTexture(twitchLogo);
        blit(matrixStack, width / 2 - 64, height / 2 - 80, 0, 0, 128, 43, 128, 43);
        drawString(matrixStack, font, I18n.format(TwitchChatIntegration.MOD_ID + ":gui.authentication.chatToken"), width / 2 - 100, height / 2 + 5, 0xFFFFFF);
        tokenTextField.render(matrixStack, mouseX, mouseY, partialTicks);
    }

}
