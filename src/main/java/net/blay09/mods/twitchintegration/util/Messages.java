package net.blay09.mods.twitchintegration.util;

import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class Messages {

    public static TranslationTextComponent lang(String key, Object... args) {
        return styledLang(key, (Style) null, args);
    }

    public static TranslationTextComponent styledLang(String key, @Nullable TextFormatting formatting, Object... args) {
        return styledLang(key, formatting != null ? Style.EMPTY.applyFormatting(formatting) : null, args);
    }

    public static TranslationTextComponent styledLang(String key, @Nullable Style style, Object... args) {
        final TranslationTextComponent text = new TranslationTextComponent(TwitchChatIntegration.MOD_ID + "." + key, args);
        if (style != null) {
            text.setStyle(style);
        }
        return text;
    }

    public static String format(String key, Object... args) {
        return I18n.format(TwitchChatIntegration.MOD_ID + "." + key, args);
    }

    public static StringTextComponent styledString(String key, @Nullable Style style) {
        final StringTextComponent text = new StringTextComponent(key);
        if (style != null) {
            text.setStyle(style);
        }
        return text;
    }
}
