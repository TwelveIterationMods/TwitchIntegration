package net.blay09.mods.twitchintegration.handler;

public class ChannelUser {
    private final String channel;
    private final String username;

    public ChannelUser(String channel, String username) {
        this.channel = channel;
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChannelUser that = (ChannelUser) o;
        return channel.equals(that.channel) && username.equals(that.username);
    }

    @Override
    public int hashCode() {
        int result = channel.hashCode();
        result = 31 * result + username.hashCode();
        return result;
    }

    public String getChannel() {
        return channel;
    }

    public String getUsername() {
        return username;
    }
}
