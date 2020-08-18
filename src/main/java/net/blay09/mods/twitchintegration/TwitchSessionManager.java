package net.blay09.mods.twitchintegration;

import com.google.common.collect.Lists;
import net.blay09.javairc.IRCConfiguration;
import net.blay09.javatmi.TMIClient;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelDisabledEvent;
import net.blay09.mods.twitchintegration.api.event.TwitchChannelEnabledEvent;
import net.blay09.mods.twitchintegration.auth.TwitchAuthManager;
import net.blay09.mods.twitchintegration.auth.TwitchAuthToken;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.handler.TwitchChatHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

@Mod.EventBusSubscriber(modid = TwitchChatIntegration.MOD_ID, value = Dist.CLIENT)
public class TwitchSessionManager {

    private static final Path channelConfigPath = FMLPaths.CONFIGDIR.get().resolve("twitchchatintegration_channels.json");

    private static final TwitchChatHandler twitchChatHandler = new TwitchChatHandler();
    private static TwitchChannelManager twitchChannelManager;
    private static final List<TwitchChannel> joinedChannels = Lists.newArrayList();
    private static TMIClient twitchClient;

    public static void init() {
        twitchChannelManager = TwitchChannelManager.load(channelConfigPath.toFile());
    }

    public static void connect() {
        TwitchAuthToken authToken = TwitchAuthManager.getAuthToken();

        if (authToken != null) {
            twitchChannelManager.createDefaultChannelIfNotExists(authToken.getUsername());
        }

        final boolean anonymousLogin = TwitchIntegrationConfig.CLIENT.useAnonymousLogin.get();
        if (authToken != null || anonymousLogin) {
            IRCConfiguration config = TMIClient.defaultConfig();
            config.setEncoding(StandardCharsets.UTF_8);
            if (authToken != null && !anonymousLogin) {
                config.setNick(authToken.getUsername());
                config.setPassword(authToken.getTmiPassword());
            } else {
                config.setNick(getAnonymousUsername());
            }
            config.setPort(TwitchIntegrationConfig.CLIENT.port.get());
            for (TwitchChannel channel : twitchChannelManager.getChannels()) {
                if (channel.isEnabled()) {
                    joinedChannels.add(channel);
                    config.getAutoJoinChannels().add("#" + channel.getName().toLowerCase(Locale.ENGLISH));
                }
            }
            twitchClient = new TMIClient(config, twitchChatHandler);
            twitchClient.connect();
        }
    }

    @Nullable
    public static TMIClient getClient() {
        return twitchClient;
    }

    private static String getAnonymousUsername() {
        return "justinfan" + (int) (Math.floor(Math.random() * 80000.0D + 1000.0D));
    }

    public static void disconnect() {
        if (twitchClient != null) {
            twitchClient.disconnect();
            twitchClient = null;
        }
    }

    public static boolean isConnected() {
        return twitchClient != null && twitchClient.getIRCConnection().isConnected();
    }

    public static TwitchChatHandler getChatHandler() {
        return twitchChatHandler;
    }

    public static TwitchChannelManager getChannelManager() {
        return twitchChannelManager;
    }

    public static List<TwitchChannel> getJoinedChannels() {
        return joinedChannels;
    }

    @SubscribeEvent
    public static void onTwitchChannelEnabled(TwitchChannelEnabledEvent event) {
        final TwitchChannel channel = event.getTwitchChannel();
        if (!joinedChannels.contains(channel)) {
            if (twitchClient != null) {
                twitchClient.join("#" + channel.getName().toLowerCase(Locale.ENGLISH));
                joinedChannels.add(channel);
            }
        }
    }

    @SubscribeEvent
    public static void onTwitchChannelDisabled(TwitchChannelDisabledEvent event) {
        final TwitchChannel channel = event.getTwitchChannel();
        if (joinedChannels.remove(channel)) {
            if (twitchClient != null) {
                twitchClient.part("#" + channel.getName().toLowerCase(Locale.ENGLISH));
            }
        }
    }
}
