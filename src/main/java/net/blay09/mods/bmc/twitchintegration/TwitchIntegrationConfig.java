package net.blay09.mods.bmc.twitchintegration;

import com.google.common.base.Charsets;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchChannel;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class TwitchIntegrationConfig {

	private static File configFile;

	public static boolean useAnonymousLogin;
	public static boolean showWhispers;
	public static String singleMessageFormat = "%u: %m";
	public static String multiMessageFormat = "[%c] %u: %m";
	public static String whisperMessageFormat = "%u \u25b6 %r: %m";
	public static String singleActionFormat = "%u %m";
	public static String multiActionFormat = "[%c] %u %m";
	public static String whisperActionFormat = "%u \u25b6 %r : %m";
	public static int port = 6667;

	public static void load(File configFile) {
		TwitchIntegrationConfig.configFile = configFile;
		Gson gson = new Gson();
		try (InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), Charsets.UTF_8)) {
			TwitchIntegrationConfig.load(gson.fromJson(reader, JsonObject.class), TwitchIntegration.getTwitchManager());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void save() {
		Gson gson = new Gson();
		try (JsonWriter writer = new JsonWriter(new OutputStreamWriter(new FileOutputStream(configFile), Charsets.UTF_8))) {
			writer.setIndent("  ");
			gson.toJson(save(new JsonObject(), TwitchIntegration.getTwitchManager()), writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static void load(JsonObject jsonRoot, TwitchManager twitchManager) {
		JsonObject jsonFormat = jsonRoot.getAsJsonObject("format");
		singleMessageFormat = jsonStringOr(jsonFormat, "singleMessage", singleMessageFormat);
		multiMessageFormat = jsonStringOr(jsonFormat, "multiMessage", multiMessageFormat);
		whisperMessageFormat = jsonStringOr(jsonFormat, "whisperMessage", whisperMessageFormat);
		singleActionFormat = jsonStringOr(jsonFormat, "singleEmote", singleActionFormat);
		multiActionFormat = jsonStringOr(jsonFormat, "multiEmote", multiActionFormat);
		whisperActionFormat = jsonStringOr(jsonFormat, "whisperEmote", whisperActionFormat);
		useAnonymousLogin = jsonRoot.has("anonymousLogin") && jsonRoot.get("anonymousLogin").getAsBoolean();
		showWhispers = jsonRoot.has("showWhispers") && jsonRoot.get("showWhispers").getAsBoolean();
		port = jsonRoot.has("port") ? jsonRoot.get("port").getAsInt() : port;
		JsonArray jsonChannels = jsonRoot.getAsJsonArray("channels");
		for(int i = 0; i < jsonChannels.size(); i++) {
			JsonObject jsonChannel = jsonChannels.get(i).getAsJsonObject();
			TwitchChannel channel = new TwitchChannel(jsonChannel.get("name").getAsString());
			channel.setSubscribersOnly(jsonChannel.has("subscribersOnly") && jsonChannel.get("subscribersOnly").getAsBoolean());
			channel.setId(jsonChannel.has("channelId")? jsonChannel.get("channelId").getAsInt(): -1);
			channel.setDeletedMessages(TwitchChannel.DeletedMessages.fromName(jsonChannel.get("deletedMessages").getAsString()));
			channel.setTargetTabName(jsonStringOr(jsonChannel, "targetTab", channel.getName()));
			channel.setActive(jsonChannel.has("active") && jsonChannel.get("active").getAsBoolean());
			twitchManager.addChannel(channel);
		}
	}

	private static JsonObject save(JsonObject jsonRoot, TwitchManager twitchManager) {
		JsonObject jsonFormat = new JsonObject();
		jsonFormat.addProperty("singleMessage", singleMessageFormat);
		jsonFormat.addProperty("multiMessage", multiMessageFormat);
		jsonFormat.addProperty("whisperMessage", whisperMessageFormat);
		jsonFormat.addProperty("singleEmote", singleActionFormat);
		jsonFormat.addProperty("multiEmote", multiActionFormat);
		jsonFormat.addProperty("whisperEmote", whisperActionFormat);
		jsonRoot.add("format", jsonFormat);
		jsonRoot.addProperty("anonymousLogin", useAnonymousLogin);
		jsonRoot.addProperty("showWhispers", showWhispers);
		jsonRoot.addProperty("port", port);
		JsonArray jsonChannels = new JsonArray();
		for(TwitchChannel channel : twitchManager.getChannels()) {
			JsonObject jsonChannel = new JsonObject();
			jsonChannel.addProperty("name", channel.getName());
			jsonChannel.addProperty("subscribersOnly", channel.isSubscribersOnly());
			jsonChannel.addProperty("channelId", channel.getId());
			jsonChannel.addProperty("deletedMessages", channel.getDeletedMessages().name().toLowerCase());
			jsonChannel.addProperty("targetTab", channel.getTargetTabName());
			jsonChannel.addProperty("active", channel.isActive());
			jsonChannels.add(jsonChannel);
		}
		jsonRoot.add("channels", jsonChannels);
		return jsonRoot;
	}

	private static String jsonStringOr(JsonObject object, String key, String defaultVal) {
		return object.has(key) ? object.get(key).getAsString() : defaultVal;
	}
}
