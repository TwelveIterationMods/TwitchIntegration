package net.blay09.mods.twitchintegration.api.event;

import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.minecraftforge.eventbus.api.Event;

public class TwitchChannelRemovedEvent extends Event {
    private final TwitchChannel twitchChannel;

    public TwitchChannelRemovedEvent(TwitchChannel twitchChannel) {
        this.twitchChannel = twitchChannel;
    }

    public TwitchChannel getTwitchChannel() {
        return twitchChannel;
    }
}
