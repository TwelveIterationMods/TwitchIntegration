package net.blay09.mods.twitchintegration.util;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.minecraft.client.Minecraft;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import javax.annotation.Nullable;
import java.io.*;

public class CachedAPI {
    private static final Gson gson = new Gson();
    private static final HttpClient httpClient = HttpClients.createDefault();
    private static final long DEFAULT_CACHE_TIME = 86400000L;

    @Nullable
    public static JsonObject loadCachedAPI(String url, String fileName) {
        return loadCachedAPI(new HttpGet(url), fileName, DEFAULT_CACHE_TIME);
    }

    @Nullable
    public static JsonObject loadCachedAPI(HttpGet request, String fileName) {
        return loadCachedAPI(request, fileName, DEFAULT_CACHE_TIME);
    }

    @Nullable
    public static JsonObject loadCachedAPI(HttpGet request, String fileName, long maxCacheTime) {
        return loadCachedAPI(request, new File(getCacheDirectory(), fileName), maxCacheTime);
    }

    @Nullable
    public static JsonObject loadCachedAPI(HttpGet request, File cacheFile, long maxCacheTime) {
        JsonObject result = loadLocal(cacheFile, false, maxCacheTime);
        if (result == null) {
            result = loadRemote(request);
            if (result == null) {
                result = loadLocal(cacheFile, true, maxCacheTime);
            } else {
                try (BufferedWriter writer = new BufferedWriter(new FileWriter(cacheFile))) {
                    gson.toJson(result, writer);
                } catch (IOException e) {
                    ChatTweaks.logger.error("An error occurred trying to cache an API result: ", e);
                }
            }
        }
        return result;
    }

    @Nullable
    private static JsonObject loadLocal(File file, boolean force, long maxCacheTime) {
        if (file.exists() && (force || System.currentTimeMillis() - file.lastModified() < maxCacheTime)) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                return gson.fromJson(reader, JsonObject.class);
            } catch (IOException e) {
                ChatTweaks.logger.error("An error occurred trying to load a cached API result: ", e);
            }
        }
        return null;
    }


    @Nullable
    private static JsonObject loadRemote(HttpGet request) {
        try {
            HttpResponse response = httpClient.execute(request);
            String jsonString = EntityUtils.toString(response.getEntity());
            return gson.fromJson(jsonString, JsonObject.class);
        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            Header authHeader = request.getFirstHeader("Authorization");
            if (authHeader != null) {
                for (String authHeaderPart : authHeader.getValue().split(" ")) {
                    exceptionMessage = exceptionMessage.replace(authHeaderPart, "<secret>");
                }
            }
            ChatTweaks.logger.error("An error occurred trying to load from an API: {}", exceptionMessage);
            return null;
        }
    }

    public static File getCacheDirectory() {
        File file = new File(Minecraft.getMinecraft().mcDataDir, "ChatTweaks/cache/");
        if (!file.exists() && !file.mkdirs()) {
            throw new RuntimeException("Could not create cache directory for Chat Tweaks.");
        }
        return file;
    }

}
