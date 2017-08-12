package net.blay09.mods.bmc.twitchintegration.util;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.gui.GuiTwitchWaitingForUsername;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchBadge;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchChannel;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.balyware.CachedAPI;
import net.blay09.mods.chattweaks.image.ITooltipProvider;
import net.blay09.mods.chattweaks.image.renderable.ImageLoader;
import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

public class TwitchAPI {

	public static final String CLIENT_ID = "gdhi94otnk7c7746syjv7gkr6bizq4w";
	public static final String OAUTH_REDIRECT_URI = "http://localhost:" + TokenReceiver.PORT + "/token/";
	private static final String API_BASE_URL = "https://api.twitch.tv/kraken?api_version=3&client_id={{CLIENT_ID}}&oauth_token={{ACCESS_TOKEN}}";
	private static final String TWITCH_AUTHORIZE = "https://api.twitch.tv/kraken/oauth2/authorize?response_type=token&client_id={{CLIENT_ID}}&redirect_uri={{REDIRECT_URI}}&scope=chat_login&force_verify=true";

	private static TokenReceiver tokenReceiver;

	public static int loadChannelId(String name) {
		try {
			JsonObject object = CachedAPI.loadCachedAPI("https://api.twitch.tv/kraken/users?login=" + name + "&client_id=" + CLIENT_ID, "twitch_" + name + "_data", "application/vnd.twitchtv.v5+json");
			if (object != null && object.has("users")) {
				JsonArray array = object.getAsJsonArray("users");
				if (array.size() > 0) {
					JsonObject userObject = array.get(0).getAsJsonObject();
					if (userObject.has("_id")) {
						return (userObject.get("_id").getAsInt());
					}
				}
			}
		} catch (Exception e) {
			TwitchIntegration.logger.error("Unexpected error when retrieving channel id for {}: ", name, e);
		}
		return -1;
	}

	public static Map<String, TwitchBadge> loadChannelBadges(TwitchChannel channel) {
		Map<String, TwitchBadge> result = Maps.newHashMap();
		try {
			JsonObject object = CachedAPI.loadCachedAPI("https://badges.twitch.tv/v1/badges/channels/" + channel.getId() + "/display", "twitch_" + channel.getName() + "_badges", null);
			if (object != null && object.has("badge_sets")) {
				JsonObject badgeSets = object.getAsJsonObject("badge_sets");
				for (Map.Entry<String, JsonElement> entry : badgeSets.entrySet()) {
					String badgeName = entry.getKey();
					JsonObject badgeObject = entry.getValue().getAsJsonObject();
					JsonObject badgeVersions = badgeObject.getAsJsonObject("versions");
					for (Map.Entry<String, JsonElement> version : badgeVersions.entrySet()) {
						String slashVal = version.getKey();
						String imageUri = version.getValue().getAsJsonObject().get("image_url_1x").getAsString();
						TwitchBadge badge = null;
						try {
							badge = new TwitchBadge(ImageLoader.loadImage(new URI(imageUri), "twitch_" + channel.getName() + "_" + badgeName + "_" + slashVal), ITooltipProvider.EMPTY);
						} catch (IOException | URISyntaxException e) {
							TwitchIntegration.logger.error("Could not load chat badge {} for channel {}: ", badgeName, channel.getName(), e);
						}
						result.put(badgeName + "/" + version.getKey(), badge);
					}
				}
			}
		} catch (Exception e) {
			TwitchIntegration.logger.error("Unexpected error when loading chat badges for channel {}: ", channel.getName(), e);
		}
		return result;
	}

	public static Map<String, TwitchBadge> loadChannelSpecificBadges(TwitchChannel channel) {
		Map<String, TwitchBadge> result = Maps.newHashMap();
		try {
			JsonObject object = CachedAPI.loadCachedAPI("https://api.twitch.tv/kraken/chat/" + channel.getId() + "/badges?client_id=" + CLIENT_ID, "twitch_" + channel.getName() + "_badges", "application/vnd.twitchtv.v5+json");
			if (object != null) {
				for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
					if (entry.getValue().isJsonNull()) {
						continue;
					}
					JsonObject badgeObject = entry.getValue().getAsJsonObject();
					try {
						TwitchBadge badge = new TwitchBadge(ImageLoader.loadImage(new URI(badgeObject.get("image").getAsString()), channel.getName() + "_" + entry.getKey()), ITooltipProvider.EMPTY);
						result.put(entry.getKey(), badge);
					} catch (IOException | URISyntaxException e) {
						TwitchIntegration.logger.error("Could not load chat badge {} for channel {}: ", entry.getKey(), channel.getName(), e);
					}
				}
			}
		} catch (Exception e) {
			TwitchIntegration.logger.error("Unexpected error when loading chat badges for channel {}: ", channel.getName(), e);
		}
		return result;
	}

	public static void listenForToken(final Runnable callback) {
		if (tokenReceiver == null) {
			tokenReceiver = new TokenReceiver() {
				@Override
				public void onTokenReceived(final String token) {
					Minecraft.getMinecraft().addScheduledTask(() -> {
						Minecraft.getMinecraft().displayGuiScreen(new GuiTwitchWaitingForUsername());
						requestUsername(token, callback);
					});
				}
			};
			tokenReceiver.start();
		}
		try {
			Class<?> desktopClass = Class.forName("java.awt.Desktop");
			Object desktop = desktopClass.getMethod("getDesktop").invoke(null);
			desktopClass.getMethod("browse", URI.class).invoke(desktop, new URI(TWITCH_AUTHORIZE.replace("{{CLIENT_ID}}", CLIENT_ID).replace("{{REDIRECT_URI}}", OAUTH_REDIRECT_URI)));
		} catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | URISyntaxException e) {
			e.printStackTrace();
		}
	}

	public static void requestUsername(final String token, final Runnable callback) {
		new Thread(() -> {
			try {
				URL apiURL = new URL(API_BASE_URL.replace("{{CLIENT_ID}}", CLIENT_ID).replace("{{ACCESS_TOKEN}}", token));
				try (InputStreamReader reader = new InputStreamReader(apiURL.openStream())) {
					try {
						Gson gson = new Gson();
						JsonObject root = gson.fromJson(reader, JsonObject.class);
						String username = root.getAsJsonObject("token").get("user_name").getAsString();
						ChatTweaks.getAuthManager().storeToken(TwitchIntegration.MOD_ID, username, token);
						Minecraft.getMinecraft().addScheduledTask(callback);
					} catch (JsonParseException e) {
						e.printStackTrace();
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();
	}

}
