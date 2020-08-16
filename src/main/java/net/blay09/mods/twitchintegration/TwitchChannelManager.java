package net.blay09.mods.twitchintegration;

import com.google.common.collect.Maps;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelAddedEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelDisabledEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelEnabledEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelRemovedEvent;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.minecraftforge.common.MinecraftForge;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;

public class TwitchChannelManager {

    private static final Map<String, TwitchChannel> channels = Maps.newHashMap();

    public static Collection<TwitchChannel> getChannels() {
        return channels.values();
    }

    public static void joinChannel(String channelName) {
        TwitchChannel channel = getChannel(channelName);
        if (channel == null) {
            channel = new TwitchChannel(channelName);
            addChannel(channel);
        }

        channel.setEnabled(true);
        MinecraftForge.EVENT_BUS.post(new TwitchChannelEnabledEvent(channel));
    }

    public static void leaveChannel(String channelName) {
        final TwitchChannel channel = getChannel(channelName);
        if (channel != null) {
            channel.setEnabled(false);
            MinecraftForge.EVENT_BUS.post(new TwitchChannelDisabledEvent(channel));
        }
    }

    public static void addChannel(TwitchChannel channel) {
        channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);
        MinecraftForge.EVENT_BUS.post(new TwitchChannelAddedEvent(channel));
    }

    public static void removeChannelByName(String channelName) {
        final TwitchChannel removedChannel = channels.remove(channelName.toLowerCase(Locale.ENGLISH));
        MinecraftForge.EVENT_BUS.post(new TwitchChannelRemovedEvent(removedChannel));
    }

    public static void createDefaultChannelIfNotExists(String username) {
        if (!channels.containsKey(username.toLowerCase(Locale.ENGLISH))) {
            TwitchChannel channel = new TwitchChannel(username);
            channel.setEnabled(true);
            addChannel(channel);
        }
    }

    @Nullable
    public static TwitchChannel getChannel(String channel) {
        if (channel.charAt(0) == '#') {
            channel = channel.substring(1);
        }

        return channels.get(channel.toLowerCase(Locale.ENGLISH));
    }
}
