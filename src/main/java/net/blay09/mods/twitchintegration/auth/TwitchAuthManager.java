package net.blay09.mods.twitchintegration.auth;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;

public class TwitchAuthManager {

    private static final Logger logger = LogManager.getLogger();

    private static TwitchAuthToken authToken;

    public static void setAuthToken(String username, String token, boolean storeToken) {
        authToken = new TwitchAuthToken(username, token);

        if (storeToken) {
            storeToken(authToken);
        }
    }

    public static TwitchAuthToken getAuthToken() {
        return authToken;
    }

    public static void loadStoredToken() {
        File tokenFile = getTokenFile();
        try(DataInputStream in = new DataInputStream(new FileInputStream(tokenFile))) {
            setAuthToken(in.readUTF(), in.readUTF(), false);
        } catch(FileNotFoundException ignored) {
        } catch (IOException e) {
            logger.error("An error occurred when trying to load the stored Twitch token ({})", e.getClass().getSimpleName());
        }
    }

    private static void storeToken(TwitchAuthToken authToken) {
        File tokenFile = getTokenFile();
        try(DataOutputStream out = new DataOutputStream(new FileOutputStream(tokenFile))) {
            out.writeUTF(authToken.getUsername());
            out.writeUTF(authToken.getToken());
        } catch (IOException e) {
            logger.error("An error occurred when trying to store the Twitch token ({})", e.getClass().getSimpleName());
        }
    }

    private static File getTokenFile() {
        final File userHome = new File(System.getProperty("user.home"));
        return new File(userHome, ".tci-auth.dat");
    }
}
