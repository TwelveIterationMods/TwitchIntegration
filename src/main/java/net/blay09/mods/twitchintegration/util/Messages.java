package net.blay09.mods.twitchintegration.util;

import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;

public class Messages {

    public static TranslationTextComponent lang(String key) {
        return lang(key, null);
    }

    public static TranslationTextComponent lang(String key, @Nullable TextFormatting formatting) {
        final TranslationTextComponent text = new TranslationTextComponent(key);
        if (formatting != null) {
            text.mergeStyle(formatting);
        }
        return text;
    }
}
