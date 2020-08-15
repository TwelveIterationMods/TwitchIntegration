package net.blay09.mods.twitchintegration.compat.vanilla;

import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.api.ChatConsumer;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.handler.ChannelUser;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.Style;

public class VanillaChatConsumer implements ChatConsumer {

    @Override
    public void onChatMessage(TwitchChannel channel, TwitchUser user, TwitchMessage message) {
        Style style = null;
        if (user.getColor() != null) {
            final Color color = Color.func_240745_a_(user.getColor());
            style = Style.EMPTY.setColor(color);
        }

        if (message.isAction()) {
            printChatMessage(Messages.styledLang("chat.action", style, user.getDisplayName(), message.getMessage()));
        } else {
            final StringTextComponent displayName = Messages.styledString(user.getDisplayName(), style);
            printChatMessage(Messages.lang("chat.message", displayName, message.getMessage()));
        }
    }

    @Override
    public void onSystemMessage(TwitchChannel channel, ITextComponent message) {
        printChatMessage(message);
    }

    @Override
    public void onWhisperMessage(TwitchUser user, TwitchMessage message) {
        Style style = null;
        if (user.getColor() != null) {
            final Color color = Color.func_240745_a_(user.getColor());
            style = Style.EMPTY.setColor(color);
        }

        if (message.isAction()) {
            printChatMessage(Messages.styledLang("chat.whisper.action", style, user.getDisplayName(), message.getMessage()));
        } else {
            final StringTextComponent displayName = Messages.styledString(user.getDisplayName(), style);
            printChatMessage(Messages.lang("chat.whisper.message", displayName, message.getMessage()));
        }
    }

    @Override
    public void clearChat(TwitchChannel channel) {
        // TODO
    }

    @Override
    public void purgeUserMessages(TwitchChannel channel, ChannelUser user) {
        // TODO
    }

    private void printChatMessage(ITextComponent textComponent) {
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessage(textComponent);
    }

}
