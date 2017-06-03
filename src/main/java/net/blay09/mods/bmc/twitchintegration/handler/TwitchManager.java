package net.blay09.mods.bmc.twitchintegration.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonWriter;
import net.blay09.javairc.IRCConfiguration;
import net.blay09.javatmi.TMIClient;
import net.blay09.mods.bmc.ChatManager;
import net.blay09.mods.bmc.ChatTweaks;
import net.blay09.mods.bmc.ChatViewManager;
import net.blay09.mods.bmc.auth.TokenPair;
import net.blay09.mods.bmc.chat.ChatView;
import net.blay09.mods.bmc.chat.emotes.twitch.BTTVChannelEmotes;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegrationConfig;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileInputStream;
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
		channels.put(channel.getName().toLowerCase(), channel);
		new BTTVChannelEmotes(channel.getName());
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

		if(tokenPair != null && !channels.containsKey(tokenPair.getUsername().toLowerCase())) {
			TwitchChannel channel = new TwitchChannel(tokenPair.getUsername());
			channel.setActive(true);
			channels.put(channel.getName().toLowerCase(), channel);

			ChatView twitchView = new ChatView(channel.getName());
			twitchView.setOutgoingPrefix("/twitch #" + channel.getName().toLowerCase(Locale.ENGLISH) + " ");
			ChatViewManager.save();
		}

		if(tokenPair != null) {
			String token = tokenPair.getToken().startsWith("oauth:") ? tokenPair.getToken() : "oauth:" + tokenPair.getToken();
			IRCConfiguration.IRCConfigurationBuilder builder = TMIClient.defaultBuilder().debug(true).nick(tokenPair.getUsername()).password(token);
			builder.port(TwitchIntegrationConfig.port);
			for(TwitchChannel channel : channels.values()) {
				if(channel.isActive()) {
					builder.autoJoinChannel("#" + channel.getName().toLowerCase());
					activeChannels.add(channel);
				}
			}
			twitchClient = new TMIClient(builder.build(), TwitchIntegration.getTwitchChatHandler());
			twitchClient.connect();
		}
	}

	public void disconnect() {
		if(twitchClient != null) {
			twitchClient.disconnect();
			twitchClient = null;
		}
	}

	public boolean isConnected() {
		return twitchClient != null && twitchClient.getIRCConnection().isConnected();
	}

	public boolean isMultiMode() {
		return activeChannels.size() > 1;
	}

	public void updateChannelStates() {
		activeChannels.clear();
		for(TwitchChannel channel : channels.values()) {
			if(channel.isActive()) {
				activeChannels.add(channel);
				if(twitchClient != null) {
					twitchClient.join("#" + channel.getName().toLowerCase());
				}
			} else {
				if(twitchClient != null) {
					twitchClient.part("#" + channel.getName().toLowerCase());
				}
			}
		}
	}

	@Nullable
	public TwitchChannel getTwitchChannel(String channel) {
		return channels.get(channel.charAt(0) == '#' ? channel.substring(1).toLowerCase() : channel.toLowerCase());
	}

	public void addNewChannel(TwitchChannel channel) {
		new BTTVChannelEmotes(channel.getName());
		channels.put(channel.getName().toLowerCase(), channel);
		updateChannelStates();
		saveChannels();
	}

	public void removeTwitchChannel(TwitchChannel channel) {
		ChatManager.removeChannel(channel.getChatChannel());
		channels.remove(channel.getName().toLowerCase());
		if(activeChannels.remove(channel)) {
			if(twitchClient != null) {
				twitchClient.part("#" + channel.getName().toLowerCase());
			}
		}
		saveChannels();
	}

	public void loadChannels() {
		Gson gson = new Gson();
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8)) {
			JsonObject root = gson.fromJson(reader, JsonObject.class);
			JsonArray channels = root.getAsJsonArray("channels");
			for(JsonElement element : channels) {
				JsonObject obj = element.getAsJsonObject();
				TwitchChannel channel = TwitchChannel.fromJson(obj);
				addChannel(channel);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveChannels() {
		JsonObject root = new JsonObject();
		JsonArray channels = new JsonArray();
		for(TwitchChannel channel : this.channels.values()) {
			channels.add(channel.toJson());
		}
		root.add("channels", channels);
		Gson gson = new Gson();
		try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
			writer.setIndent("  ");
			gson.toJson(root, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
