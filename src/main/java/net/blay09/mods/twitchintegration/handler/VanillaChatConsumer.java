package net.blay09.mods.twitchintegration.handler;

import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.api.ChatConsumer;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class VanillaChatConsumer implements ChatConsumer {

    @Override
    public void onChatMessage(TwitchChannel channel, TwitchMessage message) {
        printChatMessage(new StringTextComponent(message.getMessage()));
    }

    @Override
    public void onSystemMessage(TwitchChannel channel, ITextComponent message) {
        printChatMessage(message);
    }

    @Override
    public void onWhisperMessage(TwitchUser user, TwitchMessage message) {
        printChatMessage(new StringTextComponent(message.getMessage()));
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
