package net.blay09.mods.twitchintegration.compat.chattweaks;

import net.blay09.mods.chattweaks.api.ChatChannel;
import net.blay09.mods.chattweaks.core.ChatManager;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelRemovedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ChatTweaksAddon {

    @SubscribeEvent
    public void onTwitchChannelRemoved(TwitchChannelRemovedEvent event) {
        final ChatChannel chatChannel = event.getTwitchChannel().getChatChannel();
        if (chatChannel != null) {
            ChatManager.removeChatChannel(chatChannel.getName());
        }
    }

}
