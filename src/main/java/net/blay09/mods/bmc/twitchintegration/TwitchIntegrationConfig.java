package net.blay09.mods.bmc.twitchintegration;

import net.minecraftforge.common.config.Config;

@Config(modid = TwitchIntegration.MOD_ID, type = Config.Type.INSTANCE)
@Config.LangKey("twitchintegration.config")
public class TwitchIntegrationConfig {

	public static Format format = new Format();
	public static boolean useAnonymousLogin = false;
	public static boolean showWhispers = false;
	public static int port = 6667;

	public static class Format {
		public static String singleMessageFormat = "%u: %m";
		public static String multiMessageFormat = "[%c] %u: %m";
		public static String whisperMessageFormat = "%u \u25b6 %r: %m";
		public static String singleActionFormat = "%u %m";
		public static String multiActionFormat = "[%c] %u %m";
		public static String whisperActionFormat = "%u \u25b6 %r : %m";
	}

}
