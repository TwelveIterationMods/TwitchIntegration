package net.blay09.mods.bmc.twitchintegration.handler;

import com.google.gson.JsonObject;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.util.TwitchAPI;
import net.blay09.mods.chattweaks.ChatManager;
import net.blay09.mods.chattweaks.chat.ChatChannel;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;

public class TwitchChannel {

	public enum DeletedMessages {
		Show,
		Strikethrough,
		Replace,
		Hide;

		public static DeletedMessages fromName(String name) {
			try {
				return valueOf(name);
			} catch (IllegalArgumentException e) {
				return Replace;
			}
		}
	}

	private String name;
	private ChatChannel chatChannel;
	private boolean subscribersOnly;
	private DeletedMessages deletedMessages = DeletedMessages.Replace;
	private boolean active = true;
	private int id = -1;
	private Map<String, TwitchBadge> badges;

	public TwitchChannel(String name) {
		setName(name);
	}

	public void setName(String name) {
		if(this.name != null) {
			ChatManager.removeChatChannel(this.name);
		}
		this.name = name;
		chatChannel = ChatManager.getChatChannel(name);
		if(chatChannel == null) {
			chatChannel = new ChatChannel(name, "Twitch Chat for '" + name + "'", new ResourceLocation(TwitchIntegration.MOD_ID, "icon.png"));
			ChatManager.addChatChannel(chatChannel);
		}
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
		obj.addProperty("deletedMessages", deletedMessages.name());
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

	public boolean loadChannelId() {
		if (id == -1) {
			id = TwitchAPI.loadChannelId(name);
			return id != -1;
		}
		return true;
	}

	public void loadChannelBadges() {
		if (!loadChannelId()) {
			return;
		}
		badges = TwitchAPI.loadChannelBadges(this);
		badges.putAll(TwitchAPI.loadChannelSpecificBadges(this));
	}

	@Nullable
	public TwitchBadge getBadge(String key, int version) {
		if(badges == null) {
			loadChannelBadges();
		}
		TwitchBadge badge = badges.get(key + "/" + version);
		if(badge == null) {
			badge = badges.get(key);
		}
		return badge;
	}
}
