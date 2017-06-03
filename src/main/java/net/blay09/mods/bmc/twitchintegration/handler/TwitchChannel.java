package net.blay09.mods.bmc.twitchintegration.handler;

import com.google.gson.JsonObject;
import net.blay09.mods.bmc.ChatManager;
import net.blay09.mods.bmc.chat.ChatChannel;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.minecraft.util.ResourceLocation;

import java.util.Locale;

public class TwitchChannel {

	public enum DeletedMessages {
		SHOW,
		STRIKETHROUGH,
		REPLACE,
		HIDE;

		public static DeletedMessages fromName(String name) {
			try {
				return valueOf(name.toUpperCase());
			} catch (IllegalArgumentException e) {
				return HIDE;
			}
		}
	}

	private final String name;
	private ChatChannel chatChannel;
	private boolean subscribersOnly;
	private DeletedMessages deletedMessages = DeletedMessages.SHOW;
	private boolean active;
	private int id = -1;

	public TwitchChannel(String name) {
		this.name = name;
		chatChannel = new ChatChannel(name, "Twitch Chat for '" + name + "'", new ResourceLocation(TwitchIntegration.MOD_ID, "icon.png"));
		ChatManager.addChatChannel(chatChannel);
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public boolean isSubscribersOnly() {
		return subscribersOnly;
	}

	public void setSubscribersOnly(boolean subscribersOnly) {
		this.subscribersOnly = subscribersOnly;
	}

	public DeletedMessages getDeletedMessages() {
		return deletedMessages;
	}

	public void setDeletedMessages(DeletedMessages deletedMessages) {
		this.deletedMessages = deletedMessages;
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
		chatChannel.setEnabled(active);
	}

	public ChatChannel getChatChannel() {
		return chatChannel;
	}

	public JsonObject toJson() {
		JsonObject obj = new JsonObject();
		obj.addProperty("name", name);
		obj.addProperty("subscribersOnly", subscribersOnly);
		obj.addProperty("channelId", id);
		obj.addProperty("deletedMessages", deletedMessages.name().toLowerCase(Locale.ENGLISH));
		obj.addProperty("active", active);
		return obj;
	}

	public static TwitchChannel fromJson(JsonObject obj) {
		TwitchChannel channel = new TwitchChannel(obj.get("name").getAsString());
		channel.subscribersOnly = obj.has("subscribersOnly") && obj.get("subscribersOnly").getAsBoolean();
		channel.id = obj.has("channelId") ? obj.get("channelId").getAsInt() : -1;
		channel.deletedMessages = DeletedMessages.fromName(obj.get("deletedMessages").getAsString());
		channel.active = obj.has("active") && obj.get("active").getAsBoolean();
		return channel;
	}
}
