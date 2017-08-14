package net.blay09.mods.twitchintegration.util;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.twitchintegration.gui.GuiTwitchWaitingForUsername;
import net.blay09.mods.twitchintegration.handler.TwitchBadge;
import net.blay09.mods.twitchintegration.handler.TwitchChannel;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.balyware.CachedAPI;
import net.blay09.mods.chattweaks.image.ITooltipProvider;
import net.blay09.mods.chattweaks.image.renderable.ImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

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
	private static Map<String, TwitchBadge> defaultBadges;

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

	public static Map<String, TwitchBadge> loadGlobalBadges() {
		Map<String, TwitchBadge> result = Maps.newHashMap();
		try {
			JsonObject object = CachedAPI.loadCachedAPI("https://badges.twitch.tv/v1/badges/global/display", "twitch_badges", null);
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
							badge = new TwitchBadge(ImageLoader.loadImage(new URI(imageUri), "twitch_" + badgeName + "_" + slashVal), ITooltipProvider.EMPTY);
						} catch (IOException | URISyntaxException e) {
							TwitchIntegration.logger.error("Could not load global chat badge {}: ", badgeName, e);
						}
						result.put(badgeName + "/" + version.getKey(), badge);
					}
				}
			}
		} catch (Exception e) {
			TwitchIntegration.logger.error("Unexpected error when loading global chat badges: ", e);
		}
		return result;
	}

	public static Map<String, TwitchBadge> loadChannelSpecificBadges(TwitchChannel channel) {
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
			TwitchIntegration.logger.error("Could not open your browser - please copy the link into your browser manually.", e);
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
						ChatTweaks.getAuthManager().storeToken(TwitchIntegration.MOD_ID, username, token, TwitchIntegrationConfig.doNotStoreToken);
						Minecraft.getMinecraft().addScheduledTask(callback);
					} catch (Exception e) {
						TwitchIntegration.logger.error("Failed to retrieve your username from the Twitch token. Please try again.", e);
					}
				}
			} catch (IOException e) {
				TwitchIntegration.logger.error("Failed to retrieve your username from the token. Please try again.", e);
			}
		}).start();
	}

	public static Map<String, TwitchBadge> getDefaultBadges() {
		if(defaultBadges == null) {
			defaultBadges = loadGlobalBadges();
//			defaultBadges = Maps.newHashMap();
//			defaultBadges.put("staff", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_staff.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("admin", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_admin.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("global_mod", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_global_mod.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("broadcaster", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_broadcaster.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("moderator", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_moderator.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("partner", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_partner.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("premium", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_premium.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("turbo", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_turbo.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/1", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits1.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/100", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits100.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/1000", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits1000.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/5000", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits5000.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/10000", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits10000.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/25000", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits25000.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/50000", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits50000.png")), ITooltipProvider.EMPTY));
//			defaultBadges.put("bits/75000", new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits75000.png")), ITooltipProvider.EMPTY));
//			for (int i = 100000; i <= 1000000; i += 100000) {
//				defaultBadges.put("bits/" + i, new TwitchBadge(ImageLoader.loadImage(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_bits" + i + ".png")), ITooltipProvider.EMPTY));
//			}
		}
		return Maps.newHashMap(defaultBadges);
	}
}
