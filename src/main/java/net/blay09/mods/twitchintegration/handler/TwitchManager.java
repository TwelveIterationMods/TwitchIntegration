package net.blay09.mods.twitchintegration.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.blay09.javairc.IRCConfiguration;
import net.blay09.javatmi.TMIClient;
import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.chattweaks.ChatManager;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatViewManager;
import net.blay09.mods.chattweaks.auth.TokenPair;
import net.blay09.mods.chattweaks.chat.ChatView;

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

	private final File configFile;
	private final Map<String, TwitchChannel> channels = Maps.newHashMap();
	private final List<TwitchChannel> activeChannels = Lists.newArrayList();
	private TMIClient twitchClient;

	public TwitchManager(File configFile) {
		this.configFile = configFile;
		loadChannels();
	}

	public void addChannel(TwitchChannel channel) {
		channels.put(channel.getName().toLowerCase(Locale.ENGLISH), channel);
		channel.loadChannelBadges();
		TwitchIntegration.loadChannelEmotes(channel);
	}

	public Collection<TwitchChannel> getChannels() {
		return channels.values();
	}

	@Nullable
	public TMIClient getClient() {
		return twitchClient;
	}

	public void connect() {
		TokenPair tokenPair = ChatTweaks.getAuthManager().getToken(TwitchIntegration.MOD_ID);

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
			IRCConfiguration.IRCConfigurationBuilder builder = TMIClient.defaultBuilder().debug(false);
			if (tokenPair != null && !TwitchIntegrationConfig.useAnonymousLogin) {
				String token = tokenPair.getToken().startsWith("oauth:") ? tokenPair.getToken() : "oauth:" + tokenPair.getToken();
				builder.nick(tokenPair.getUsername()).password(token);
			} else {
				builder.nick(getAnonymousUsername());
			}
			builder.port(TwitchIntegrationConfig.port);
			for (TwitchChannel channel : channels.values()) {
				if (channel.isActive()) {
					builder.autoJoinChannel("#" + channel.getName().toLowerCase(Locale.ENGLISH));
					activeChannels.add(channel);
				}
			}
			twitchClient = new TMIClient(builder.build(), TwitchIntegration.getTwitchChatHandler());
			twitchClient.connect();
		}
	}

	private static String getAnonymousUsername() {
		return "justinfan" + (int) (Math.floor(Math.random() * 80000.0D + 1000.0D));
	}

	public void disconnect() {
		if (twitchClient != null) {
			twitchClient.disconnect();
			activeChannels.clear();
			twitchClient = null;
		}
	}

	public boolean isConnected() {
		return twitchClient != null && twitchClient.getIRCConnection().isConnected();
	}

	public void updateChannelStates() {
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
	public TwitchChannel getTwitchChannel(String channel) {
		return channels.get(channel.charAt(0) == '#' ? channel.substring(1).toLowerCase(Locale.ENGLISH) : channel.toLowerCase(Locale.ENGLISH));
	}

	public void removeTwitchChannel(TwitchChannel channel) {
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

	public void loadChannels() {
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
			TwitchIntegration.logger.error("Could not load Twitch channel configurations: ", e);
		}
	}

	public void saveChannels() {
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
			TwitchIntegration.logger.error("Could not save Twitch channel configurations: ", e);
		}
	}

	public TwitchChannel[] createDefaults() {
		TokenPair tokenPair = ChatTweaks.getAuthManager().getToken(TwitchIntegration.MOD_ID);
		if (tokenPair != null) {
			TwitchChannel defaultChannel = new TwitchChannel(tokenPair.getUsername());
			defaultChannel.setActive(true);
			return new TwitchChannel[]{defaultChannel};
		}
		return new TwitchChannel[0];
	}

	public void removeAllChannels() {
		channels.clear();
	}

	public void renameTwitchChannel(TwitchChannel twitchChannel, String newName) {
		removeTwitchChannel(twitchChannel);
		twitchChannel.setName(newName);
		addChannel(twitchChannel);
	}
}
