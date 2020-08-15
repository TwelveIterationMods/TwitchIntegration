package net.blay09.mods.twitchintegration.config;

public class TwitchChannelsConfig {

    /*
    public static void load(File configDir) {
        File configFile = new File(configDir, "twitch_channels.json");
        loadChannels();
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
	}*/

    /*
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
     */

}
