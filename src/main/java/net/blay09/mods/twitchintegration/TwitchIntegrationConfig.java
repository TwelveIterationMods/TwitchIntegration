package net.blay09.mods.twitchintegration;

import com.google.common.collect.Lists;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class TwitchIntegrationConfig {

    public static class Client {

        public final ForgeConfigSpec.BooleanValue useAnonymousLogin;
        public final ForgeConfigSpec.BooleanValue showWhispers;
        public final ForgeConfigSpec.BooleanValue disableUserColors;
        public final ForgeConfigSpec.BooleanValue disableNameBadges;
        public final ForgeConfigSpec.BooleanValue doNotStoreToken;
        public final ForgeConfigSpec.IntValue port;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> userBlacklist;

        Client(ForgeConfigSpec.Builder builder) {
            useAnonymousLogin = builder
                    .comment("If you login anonymously you can read chat, but you will not be able to type to Twitch chat from within Minecraft.")
                    .translation("config.twitchchatintegration.useAnonymousLogin")
                    .define("useAnonymousLogin", false);

            showWhispers = builder
                    .translation("config.twitchchatintegration.showWhispers")
                    .define("showWhispers", false);

            disableUserColors = builder
                    .translation("config.twitchchatintegration.disableUserColors")
                    .define("disableUserColors", false);

            disableNameBadges = builder
                    .translation("config.twitchchatintegration.disableNameBadges")
                    .define("disableNameBadges", false);

            doNotStoreToken = builder
                    .comment("Set this if you're on a public computer or concerned about security. You will have to re-authenticate every time you start Minecraft.")
                    .translation("config.twitchchatintegration.doNotStoreToken")
                    .define("doNotStoreToken", false);

            port = builder
                    .translation("config.twitchchatintegration.port")
                    .defineInRange("port", 6667, Integer.MIN_VALUE, Integer.MAX_VALUE);

            userBlacklist = builder
                    .comment("Messages by these users will not display in chat. Useful to hide bots for example.")
                    .translation("config.twitchchatintegration.userBlacklist")
                    .defineList("userBlacklist", Lists.newArrayList(), it -> it instanceof String);
        }

    }

    static final ForgeConfigSpec clientSpec;
    public static final TwitchIntegrationConfig.Client CLIENT;

    static {
        final Pair<TwitchIntegrationConfig.Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(TwitchIntegrationConfig.Client::new);
        clientSpec = specPair.getRight();
        CLIENT = specPair.getLeft();
    }

}
