package net.blay09.mods.twitchintegration;

import com.google.gson.Gson;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelAddedEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelDisabledEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelEnabledEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelRemovedEvent;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class TwitchChannelManager {

    private static final Logger logger = LogManager.getLogger();

    private transient File file;
    private final Map<String, TwitchChannel> channels = new HashMap<>();

    public Collection<TwitchChannel> getChannels() {
        return channels.values();
    }

    public void joinChannel(String channelName) {
        TwitchChannel channel = getChannel(channelName);
        if (channel == null) {
            channel = new TwitchChannel(channelName);
            addChannel(channel);
        }

        channel.setEnabled(true);
        MinecraftForge.EVENT_BUS.post(new TwitchChannelEnabledEvent(channel));
        save();
    }

    public void leaveChannel(String channelName) {
        final TwitchChannel channel = getChannel(channelName);
        if (channel != null) {
            channel.setEnabled(false);
            MinecraftForge.EVENT_BUS.post(new TwitchChannelDisabledEvent(channel));
            save();
        }
    }

    private void addChannel(TwitchChannel channel) {
        channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);
        MinecraftForge.EVENT_BUS.post(new TwitchChannelAddedEvent(channel));
        save();
    }

    public void removeChannelByName(String channelName) {
        final TwitchChannel removedChannel = channels.remove(channelName.toLowerCase(Locale.ENGLISH));
        MinecraftForge.EVENT_BUS.post(new TwitchChannelRemovedEvent(removedChannel));
        save();
    }

    public void createDefaultChannelIfNotExists(String username) {
        if (!channels.containsKey(username.toLowerCase(Locale.ENGLISH))) {
            TwitchChannel channel = new TwitchChannel(username);
            channel.setEnabled(true);
            addChannel(channel);
        }
    }

    @Nullable
    public TwitchChannel getChannel(String channel) {
        if (channel.charAt(0) == '#') {
            channel = channel.substring(1);
        }

        return channels.get(channel.toLowerCase(Locale.ENGLISH));
    }

    public static TwitchChannelManager load(File file) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            final TwitchChannelManager twitchChannelManager = gson.fromJson(reader, TwitchChannelManager.class);
            twitchChannelManager.file = file;
            return twitchChannelManager;
        } catch (IOException e) {
            final TwitchChannelManager twitchChannelManager = new TwitchChannelManager();
            twitchChannelManager.file = file;
            return twitchChannelManager;
        }
    }

    public void save() {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(this, writer);
        } catch (IOException e) {
            logger.error("Could not save Twitch channel configs: ", e);
        }
    }
}
