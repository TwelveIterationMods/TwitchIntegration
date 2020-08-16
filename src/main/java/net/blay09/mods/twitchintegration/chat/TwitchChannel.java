package net.blay09.mods.twitchintegration.chat;

import com.google.gson.JsonObject;
import net.blay09.mods.twitchintegration.util.TwitchAPI;

import javax.annotation.Nullable;
import java.util.Map;

public class TwitchChannel {

    private String name;
    private boolean subscribersOnly;
    private DeletedMessagesMode deletedMessagesMode = DeletedMessagesMode.Replace;
    private boolean enabled = true;
    private int id = -1;
    private Map<String, TwitchBadge> badges;

    public TwitchChannel(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
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

    public DeletedMessagesMode getDeletedMessages() {
        return deletedMessagesMode;
    }

    public void setDeletedMessagesMode(DeletedMessagesMode deletedMessagesMode) {
        this.deletedMessagesMode = deletedMessagesMode;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public JsonObject toJson() {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", name);
        obj.addProperty("subscribersOnly", subscribersOnly);
        obj.addProperty("channelId", id);
        obj.addProperty("deletedMessages", deletedMessagesMode.name());
        obj.addProperty("active", enabled);
        return obj;
    }

    public static TwitchChannel fromJson(JsonObject obj) {
        TwitchChannel channel = new TwitchChannel(obj.get("name").getAsString());
        channel.subscribersOnly = obj.has("subscribersOnly") && obj.get("subscribersOnly").getAsBoolean();
        channel.id = obj.has("channelId") ? obj.get("channelId").getAsInt() : -1;
        channel.deletedMessagesMode = DeletedMessagesMode.fromName(obj.get("deletedMessages").getAsString());
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

}
