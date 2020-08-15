package net.blay09.mods.twitchintegration.util;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.twitchintegration.auth.TokenReceiver;
import net.blay09.mods.twitchintegration.auth.TwitchAuthManager;
import net.blay09.mods.twitchintegration.chat.TwitchBadge;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.gui.screen.TwitchWaitingForUsernameScreen;
import net.minecraft.client.Minecraft;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class TwitchAPI {

    public static final String CLIENT_ID = "10napoemcms7qf9j0dxf28ndl2ltc3";
    public static final String OAUTH_REDIRECT_URI = "http://localhost:" + TokenReceiver.PORT + "/token/";
    public static final String SCOPES = "chat:read+chat:edit";
    private static final String API_BASE_URL = "https://api.twitch.tv/helix/";
    private static final String TWITCH_AUTHORIZE = "https://id.twitch.tv/oauth2/authorize?response_type=token&client_id={{CLIENT_ID}}&redirect_uri={{REDIRECT_URI}}&scope={{SCOPES}}&force_verify=true";

    private static TokenReceiver tokenReceiver;
    private static Map<String, TwitchBadge> defaultBadges;

    public static int loadChannelId(String name) {
        try {
            HttpGet request = new HttpGet(API_BASE_URL + "users?login=" + name);
            request.addHeader("Client-ID", CLIENT_ID);
            JsonObject object = CachedAPI.loadCachedAPI(request, "twitch_" + name + "_data");
            if (object != null && object.has("data")) {
                JsonArray array = object.getAsJsonArray("data");
                if (array.size() > 0) {
                    JsonObject userObject = array.get(0).getAsJsonObject();
                    if (userObject.has("_id")) {
                        return (userObject.get("_id").getAsInt());
                    }
                }
            }
        } catch (Exception e) {
            TwitchChatIntegration.logger.error("Unexpected error when retrieving channel id for {}: ", name, e);
        }
        return -1;
    }

    public static Map<String, TwitchBadge> loadGlobalBadges() {
        Map<String, TwitchBadge> result = Maps.newHashMap();
        try {
            JsonObject object = CachedAPI.loadCachedAPI("https://badges.twitch.tv/v1/badges/global/display", "twitch_badges");
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
                        /* TODO try {
                            badge = new TwitchBadge(ImageLoader.loadImage(new URI(imageUri), "twitch_" + badgeName + "_" + slashVal), ITooltipProvider.EMPTY);
                        } catch (IOException | URISyntaxException e) {
                            TwitchChatIntegration.logger.error("Could not load global chat badge {}: ", badgeName, e);
                        }*/
                        result.put(badgeName + "/" + version.getKey(), badge);
                    }
                }
            }
        } catch (Exception e) {
            TwitchChatIntegration.logger.error("Unexpected error when loading global chat badges: ", e);
        }
        return result;
    }

    public static Map<String, TwitchBadge> loadChannelSpecificBadges(TwitchChannel channel) {
        Map<String, TwitchBadge> result = Maps.newHashMap();
        try {
            JsonObject object = CachedAPI.loadCachedAPI("https://badges.twitch.tv/v1/badges/channels/" + channel.getId() + "/display", "twitch_" + channel.getName() + "_badges");
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
                        /* TODO try {
                            badge = new TwitchBadge(ImageLoader.loadImage(new URI(imageUri), "twitch_" + channel.getName() + "_" + badgeName + "_" + slashVal), ITooltipProvider.EMPTY);
                        } catch (IOException | URISyntaxException e) {
                            TwitchChatIntegration.logger.error("Could not load chat badge {} for channel {}: ", badgeName, channel.getName(), e);
                        }*/
                        result.put(badgeName + "/" + version.getKey(), badge);
                    }
                }
            }
        } catch (Exception e) {
            TwitchChatIntegration.logger.error("Unexpected error when loading chat badges for channel {}: ", channel.getName(), e);
        }
        return result;
    }

    public static String getAuthenticationURL() {
        return TWITCH_AUTHORIZE.replace("{{CLIENT_ID}}", CLIENT_ID).replace("{{REDIRECT_URI}}", OAUTH_REDIRECT_URI).replace("{{SCOPES}}", SCOPES);
    }

    public static boolean listenForToken(final Runnable callback) {
        if (tokenReceiver == null) {
            tokenReceiver = new TokenReceiver() {
                @Override
                public void onTokenReceived(final String token) {
                    Minecraft.getInstance().enqueue(() -> {
                        Minecraft.getInstance().displayGuiScreen(new TwitchWaitingForUsernameScreen());
                        requestUsername(token, callback);
                    });
                }
            };
            tokenReceiver.start();
        }

        String uri = getAuthenticationURL();
        try {
            Class<?> desktopClass = Class.forName("java.awt.Desktop");
            Object desktop = desktopClass.getMethod("getDesktop").invoke(null);

            desktopClass.getMethod("browse", URI.class).invoke(desktop, new URI(uri));
            return true;
        } catch (NoSuchMethodException | IllegalAccessException | ClassNotFoundException | InvocationTargetException | URISyntaxException e) {
            TwitchChatIntegration.logger.error("Could not open your browser - please copy the link into your browser manually: {}", uri);
            return false;
        }
    }

    public static void requestUsername(final String token, final Runnable callback) {
        new Thread(() -> {
            try {
                Gson gson = new Gson();
                HttpClient httpClient = HttpClients.createDefault();
                HttpGet request = new HttpGet(API_BASE_URL + "users");
                request.addHeader("Authorization", "Bearer " + token);
                request.addHeader("Client-ID", CLIENT_ID);
                HttpResponse response = httpClient.execute(request);
                String jsonString = EntityUtils.toString(response.getEntity());
                JsonObject root = gson.fromJson(jsonString, JsonObject.class);
                String username = root.getAsJsonArray("data").get(0).getAsJsonObject().get("login").getAsString();
                TwitchAuthManager.setAuthToken(username, token, TwitchIntegrationConfig.CLIENT.doNotStoreToken.get());
                Minecraft.getInstance().enqueue(callback);
            } catch (Exception e) {
                String exceptionMessage = e.getMessage().replace(token, "<token>");
                TwitchChatIntegration.logger.error("Failed to retrieve your username from the token. Please try again. {}", exceptionMessage);
            }
        }).start();
    }

    public static Map<String, TwitchBadge> getDefaultBadges() {
        if (defaultBadges == null) {
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
