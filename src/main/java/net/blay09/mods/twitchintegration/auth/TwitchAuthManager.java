package net.blay09.mods.twitchintegration.auth;

public class TwitchAuthManager {

    private static TwitchAuthToken authToken;

    public static void setAuthToken(String username, String token, boolean storeToken) {
        authToken = new TwitchAuthToken(username, token);

        if(storeToken) {
            // TODO store token
        }
    }

    public static TwitchAuthToken getAuthToken() {
        return authToken;
    }
}
