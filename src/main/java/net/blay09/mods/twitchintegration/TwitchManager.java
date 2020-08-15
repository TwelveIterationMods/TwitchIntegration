package net.blay09.mods.twitchintegration;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.blay09.javairc.IRCConfiguration;
import net.blay09.javatmi.TMIClient;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TwitchManager {

	private static final Map<String, TwitchChannel> channels = Maps.newHashMap();
	private static final List<TwitchChannel> activeChannels = Lists.newArrayList();
	private static TMIClient twitchClient;

	public static void load(File configDir) {
		File configFile = new File(configDir, "twitch_channels.json")
		this.configFile = configFile;
		loadChannels();
	}

	public static void addChannel(TwitchChannel channel) {
		channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);
		channel.loadChannelBadges();
		TwitchChatIntegration.loadChannelEmotes(channel);
	}

	public static Collection<TwitchChannel> getChannels() {
		return channels.values();
	}

	@Nullable
	public static TMIClient getClient() {
		return twitchClient;
	}

	public static void connect() {
		TokenPair tokenPair = ChatTweaks.getAuthManager().getToken(TwitchChatIntegration.MOD_ID);

		if (tokenPair != null && !channels.containsKey(tokenPair.getUsername().toLowerCase(Locale.ENGLISH))) {
			TwitchChannel[] defaultChannels = createDefaults();
			if (defaultChannels.length > 0) {
				TwitchChannel channel = defaultChannels[0];
				channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);
				saveChannels();

				channel.createOrUpdateChatChannel();
				channel.createDefaultView();
			}
		}

		if(tokenPair != null || TwitchIntegrationConfig.useAnonymousLogin) {
			IRCConfiguration config = TMIClient.defaultConfig();
			config.setEncoding(StandardCharsets.UTF_8);
			if (tokenPair != null && !TwitchIntegrationConfig.useAnonymousLogin) {
				String token = tokenPair.getToken().startsWith("oauth:") ? tokenPair.getToken() : "oauth:" + tokenPair.getToken();
				config.setNick(tokenPair.getUsername());
				config.setPassword(token);
			} else {
				config.setNick(getAnonymousUsername());
			}
			config.setPort(TwitchIntegrationConfig.port);
			for (TwitchChannel channel : channels.values()) {
				if (channel.isActive()) {
					config.getAutoJoinChannels().add("#" + channel.getName().toLowerCase(Locale.ENGLISH));
					activeChannels.add(channel);
				}
			}
			twitchClient = new TMIClient(config, TwitchChatIntegration.getTwitchChatHandler());
			twitchClient.connect();
		}
	}

	private static String getAnonymousUsername() {
		return "justinfan" + (int) (Math.floor(Math.random() * 80000.0D + 1000.0D));
	}

	public static void disconnect() {
		if (twitchClient != null) {
			twitchClient.disconnect();
			activeChannels.clear();
			twitchClient = null;
		}
	}

	public static boolean isConnected() {
		return twitchClient != null && twitchClient.getIRCConnection().isConnected();
	}

	public static void updateChannelStates() {
		// Leave channels if they were removed
		for (TwitchChannel channel : activeChannels) {
			if (!channels.containsKey(channel.getName().toLowerCase(Locale.ENGLISH))) {
				ChatView chatView = ChatViewManager.getChatView(channel.getName());
				if (chatView != null && chatView.getChannels().size() == 1 && chatView.getChannels().contains(channel.getChatChannel())) {
					ChatViewManager.removeChatView(chatView);
					ChatViewManager.save();
				}
				if (twitchClient != null) {
					twitchClient.part("#" + channel.getName().toLowerCase(Locale.ENGLISH));
				}
			}
		}

		activeChannels.clear();

		for (TwitchChannel channel : channels.values()) {
			if (channel.isActive()) {
				activeChannels.add(channel);
				if (twitchClient != null) {
					twitchClient.join("#" + channel.getName().toLowerCase(Locale.ENGLISH));
				}
			} else {
				if (twitchClient != null) {
					twitchClient.part("#" + channel.getName().toLowerCase(Locale.ENGLISH));
				}
			}
		}
	}

	@Nullable
	public static TwitchChannel getTwitchChannel(String channel) {
		return channels.get(channel.charAt(0) == '#' ? channel.substring(1).toLowerCase(Locale.ENGLISH) : channel.toLowerCase(Locale.ENGLISH));
	}

	public static void removeTwitchChannel(TwitchChannel channel) {
		if(channel.getChatChannel() != null) {
			ChatManager.removeChatChannel(channel.getChatChannel().getName());
		}
		channels.remove(channel.getName().toLowerCase(Locale.ENGLISH));
		if (activeChannels.remove(channel)) {
			if (twitchClient != null) {
				twitchClient.part("#" + channel.getName().toLowerCase(Locale.ENGLISH));
			}
		}
	}

	public static void loadChannels() {
		Gson gson = new Gson();
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
			JsonObject root = gson.fromJson(reader, JsonObject.class);
			JsonArray channels = root.getAsJsonArray("channels");
			for (JsonElement element : channels) {
				JsonObject obj = element.getAsJsonObject();
				TwitchChannel channel = TwitchChannel.fromJson(obj);
				channel.createOrUpdateChatChannel();
				addChannel(channel);
			}
		} catch (FileNotFoundException ignored) {
		} catch (Exception e) {
			TwitchChatIntegration.logger.error("Could not load Twitch channel configurations: ", e);
		}
	}

	public static void saveChannels() {
		JsonObject root = new JsonObject();
		JsonArray channels = new JsonArray();
		for (TwitchChannel channel : this.channels.values()) {
			channels.add(channel.toJson());
		}
		root.add("channels", channels);
		Gson gson = new Gson();
		try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
			writer.setIndent("  ");
			gson.toJson(root, writer);
		} catch (IOException e) {
			TwitchChatIntegration.logger.error("Could not save Twitch channel configurations: ", e);
		}
	}

	public static TwitchChannel[] createDefaults() {
		TokenPair tokenPair = ChatTweaks.getAuthManager().getToken(TwitchChatIntegration.MOD_ID);
		if (tokenPair != null) {
			TwitchChannel defaultChannel = new TwitchChannel(tokenPair.getUsername());
			defaultChannel.setActive(true);
			return new TwitchChannel[]{defaultChannel};
		}
		return new TwitchChannel[0];
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
