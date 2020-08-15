package net.blay09.mods.twitchintegration.auth;

public class TwitchAuthToken {
    private final String username;
    private final String token;

    public TwitchAuthToken(String username, String token) {
        this.username = username;
        this.token = token;
    }

    public String getUsername() {
        return username;
    }

    public String getToken() {
        return token;
    }

    public String getTmiPassword() {
        return token.startsWith("oauth:") ? token : "oauth:" + token;
    }
}
