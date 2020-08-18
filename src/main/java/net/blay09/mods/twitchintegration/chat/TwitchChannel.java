package net.blay09.mods.twitchintegration.chat;

public class TwitchChannel {

    private String name;
    private boolean subscribersOnly;
    private DeletedMessagesMode deletedMessagesMode = DeletedMessagesMode.Replace;
    private boolean enabled = true;
    private int channelId = -1;

    public TwitchChannel(String name) {
        this.name = name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean hasChannelId() {
        return channelId != -1;
    }

    public int getChannelId() {
        return channelId;
    }

    public void setChannelId(int channelId) {
        this.channelId = channelId;
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

    public DeletedMessagesMode getDeletedMessagesMode() {
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

}
