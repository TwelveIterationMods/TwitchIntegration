package net.blay09.mods.twitchintegration.api;

import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.handler.ChannelUser;
import net.minecraft.util.text.ITextComponent;

public interface ChatConsumer {
    void onChatMessage(TwitchChannel channel, TwitchUser user, TwitchMessage message);
    void onSystemMessage(TwitchChannel channel, ITextComponent message);
    void onWhisperMessage(TwitchUser user, TwitchMessage message);

    void clearChat(TwitchChannel channel);

    void purgeUserMessages(TwitchChannel channel, ChannelUser user);
}
