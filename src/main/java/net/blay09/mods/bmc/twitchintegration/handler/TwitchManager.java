package net.blay09.mods.bmc.twitchintegration.handler;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.blay09.javairc.IRCConfiguration;
import net.blay09.javatmi.TMIClient;
import net.blay09.mods.bmc.api.BetterMinecraftChatAPI;
import net.blay09.mods.bmc.api.TokenPair;
import net.blay09.mods.bmc.api.chat.IChatChannel;
import net.blay09.mods.bmc.api.chat.MessageStyle;
import net.blay09.mods.bmc.chat.emotes.twitch.BTTVChannelEmotes;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegrationConfig;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class TwitchManager {

	private final Map<String, TwitchChannel> channels = Maps.newHashMap();
	private final List<TwitchChannel> activeChannels = Lists.newArrayList();
	private TMIClient twitchClient;

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
		TokenPair tokenPair = BetterMinecraftChatAPI.getAuthManager().getToken(TwitchIntegration.MOD_ID);

		if(tokenPair != null && !channels.containsKey(tokenPair.getUsername().toLowerCase())) {
			TwitchChannel channel = new TwitchChannel(tokenPair.getUsername());
			channel.setActive(true);
			if(BetterMinecraftChatAPI.getChatChannel("*", false) != null) {
				channel.setTargetTabName("*");
			}
			channels.put(channel.getName().toLowerCase(), channel);

			IChatChannel twitchTab = BetterMinecraftChatAPI.getChatChannel(channel.getName(), true);
			twitchTab.setOutgoingPrefix("/twitch #" + channel.getName().toLowerCase() + " ");
			if(channel.getTargetTabName().equals(channel.getName())) {
				twitchTab.setDisplayChannel(null);
			} else {
				twitchTab.setDisplayChannel(BetterMinecraftChatAPI.getChatChannel(channel.getTargetTabName(), false));
			}
			BetterMinecraftChatAPI.saveChannels();
		}

		for(TwitchChannel channel : channels.values()) {
			channel.setTargetTab(BetterMinecraftChatAPI.getChatChannel(channel.getTargetTabName(), false));
		}

		if(tokenPair != null) {
			String token = tokenPair.getToken().startsWith("oauth:") ? tokenPair.getToken() : "oauth:" + tokenPair.getToken();
			IRCConfiguration.IRCConfigurationBuilder builder = TMIClient.defaultBuilder().debug(true).nick(tokenPair.getUsername()).password(token);
			builder.port(TwitchIntegrationConfig.port);
			for(TwitchChannel channel : channels.values()) {
				if(channel.isActive()) {
					builder.autoJoinChannel("#" + channel.getName().toLowerCase());
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
			IChatChannel twitchTab = BetterMinecraftChatAPI.getChatChannel(channel.getName(), false);
			if(twitchTab != null) {
				twitchTab.setMessageStyle(channel.isActive() ? MessageStyle.Chat : MessageStyle.Hidden);
			}
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
		TwitchIntegrationConfig.save();
	}

	public void removeTwitchChannel(TwitchChannel channel) {
		IChatChannel twitchTab = BetterMinecraftChatAPI.getChatChannel(channel.getName(), false);
		if(twitchTab != null) {
			BetterMinecraftChatAPI.removeChannel(twitchTab);
		}
		channels.remove(channel.getName().toLowerCase());
		if(activeChannels.remove(channel)) {
			if(twitchClient != null) {
				twitchClient.part("#" + channel.getName().toLowerCase());
			}
		}
		TwitchIntegrationConfig.save();
	}

}
