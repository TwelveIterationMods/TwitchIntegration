package net.blay09.mods.twitchintegration;

import com.google.common.collect.Maps;
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

    public static void addChannel(TwitchChannel channel) {
        channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);
        channel.loadChannelBadges();
        // TODO TwitchChatIntegration.loadChannelEmotes(channel);
    }

    public static void createDefaultChannelIfNotExists(String username) {
        if (!channels.containsKey(username.toLowerCase(Locale.ENGLISH))) {
            TwitchChannel channel = new TwitchChannel(username);
            channel.setEnabled(true);
            addChannel(channel);

            channel.createOrUpdateChatChannel();
            channel.createDefaultView();
        }
    }

    @Nullable
    public static TwitchChannel getTwitchChannel(String channel) {
        if (channel.charAt(0) == '#') {
            channel = channel.substring(1);
        }

        return channels.get(channel.toLowerCase(Locale.ENGLISH));
    }

    public static void removeTwitchChannel(TwitchChannel channel) {
        channels.remove(channel.getName().toLowerCase(Locale.ENGLISH));

        MinecraftForge.EVENT_BUS.post(new TwitchChannelRemovedEvent(channel));
    }

    public static void removeAllChannels() {
        channels.clear();
    }

    public static void renameTwitchChannel(TwitchChannel twitchChannel, String newName) {
        removeTwitchChannel(twitchChannel);
        twitchChannel.setName(newName);
        addChannel(twitchChannel);
    }
}
