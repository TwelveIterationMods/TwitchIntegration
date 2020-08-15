package net.blay09.mods.twitchintegration;

import com.google.common.collect.Maps;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelAddedEvent;
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

    public static boolean joinChannel(String channelName) {
        if (getChannel(channelName) != null) {
            return false;
        }

        TwitchChannel channel = new TwitchChannel(channelName);
        addChannel(channel);
        return true;
    }

    public static boolean leaveChannel(String channelName) {
        final TwitchChannel channel = getChannel(channelName);
        if (channel != null) {
            removeChannel(channel);
            return true;
        }

        return false;
    }

    public static void addChannel(TwitchChannel channel) {
        channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);

        // TODO channel.loadChannelBadges();
        // TODO TwitchChatIntegration.loadChannelEmotes(channel);

        MinecraftForge.EVENT_BUS.post(new TwitchChannelAddedEvent(channel));
    }

    public static void removeChannel(TwitchChannel channel) {
        channels.remove(channel.getName().toLowerCase(Locale.ENGLISH));

        MinecraftForge.EVENT_BUS.post(new TwitchChannelRemovedEvent(channel));
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

    public static void removeAllChannels() {
        channels.clear();
    }

    public static void renameTwitchChannel(TwitchChannel twitchChannel, String newName) {
        removeChannel(twitchChannel);
        twitchChannel.setName(newName);
        addChannel(twitchChannel);
    }
}
