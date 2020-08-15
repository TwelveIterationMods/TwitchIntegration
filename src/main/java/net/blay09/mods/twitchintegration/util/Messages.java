package net.blay09.mods.twitchintegration.util;

import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class Messages {

    public static TranslationTextComponent lang(String key, Object... args) {
        return styledLang(key, null, args);
    }

    public static TranslationTextComponent styledLang(String key, @Nullable TextFormatting formatting, Object... args) {
        final TranslationTextComponent text = new TranslationTextComponent(TwitchChatIntegration.MOD_ID + "." + key, args);
        if (formatting != null) {
            text.mergeStyle(formatting);
        }
        return text;
    }

    public static String format(String key) {
        return I18n.format(TwitchChatIntegration.MOD_ID + "." + key);
    }
}
