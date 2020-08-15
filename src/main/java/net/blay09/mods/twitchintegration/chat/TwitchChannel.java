package net.blay09.mods.twitchintegration.chat;

import com.google.gson.JsonObject;
import net.blay09.mods.chattweaks.api.ChatChannel;
import net.blay09.mods.chattweaks.api.ChatView;
import net.blay09.mods.chattweaks.core.ChatChannelImpl;
import net.blay09.mods.chattweaks.core.ChatManager;
import net.blay09.mods.chattweaks.core.ChatViewImpl;
import net.blay09.mods.chattweaks.core.ChatViewManager;
import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.blay09.mods.twitchintegration.util.TwitchAPI;
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
    private boolean enabled = true;
    private int id = -1;
    private Map<String, TwitchBadge> badges;

    public TwitchChannel(String name) {
        this.name = name;
    }

    public void createOrUpdateChatChannel() {
        if (chatChannel != null && !chatChannel.getName().equalsIgnoreCase(name)) {
            ChatManager.removeChatChannel(chatChannel.getName());
        }
        chatChannel = ChatManager.getChatChannel(name);
        if (chatChannel == null) {
            chatChannel = new ChatChannelImpl(name, "Twitch Chat for '" + name + "'", new ResourceLocation(TwitchChatIntegration.MOD_ID, "icon.png"), null);
            ChatManager.addChatChannel(chatChannel);
        }
    }

    public void setName(String name) {
        this.name = name;
        createOrUpdateChatChannel();
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (chatChannel != null) {
            // TODO chatChannel.setEnabled(active);
        }
    }

    @Nullable
    public ChatChannel getChatChannel() {
        return chatChannel;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("subscribersOnly", subscribersOnly);
        obj.addProperty("channelId", id);
        obj.addProperty("deletedMessages", deletedMessages.name());
        obj.addProperty("active", enabled);
        return obj;
    }

    public static TwitchChannel fromJson(JsonObject obj) {
        TwitchChannel channel = new TwitchChannel(obj.get("name").getAsString());
        channel.subscribersOnly = obj.has("subscribersOnly") && obj.get("subscribersOnly").getAsBoolean();
        channel.id = obj.has("channelId") ? obj.get("channelId").getAsInt() : -1;
        channel.deletedMessages = DeletedMessages.fromName(obj.get("deletedMessages").getAsString());
        channel.enabled = obj.has("active") && obj.get("active").getAsBoolean();
        return channel;
    }

    public void loadChannelBadges() {
        if (badges == null) {
            new Thread(() -> {
                if (id == -1) {
                    id = TwitchAPI.loadChannelId(name);
                    if (id == -1) {
                        return;
                    }
                }
                Map<String, TwitchBadge> badges = TwitchAPI.getDefaultBadges();
                badges.putAll(TwitchAPI.loadChannelSpecificBadges(this));

                this.badges = badges;
            }).start();
        }
    }

    @Nullable
    public TwitchBadge getBadge(String key, int version) {
        if (badges == null) {
            return null;
        }
        TwitchBadge badge = badges.get(key + "/" + version);
        if (badge == null) {
            badge = badges.get(key);
        }
        return badge;
    }

    public void createDefaultView() {
        if (chatChannel != null) {
            ChatView twitchView = new ChatViewImpl(name);
            twitchView.addChannel(chatChannel.getName());
            twitchView.setOutgoingPrefix("/twitch #" + name.toLowerCase(Locale.ENGLISH) + " ");
            ChatViewManager.registerView(twitchView);
        }
    }
}
