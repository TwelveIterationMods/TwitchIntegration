package net.blay09.mods.bmc.twitchintegration;

import net.minecraftforge.common.config.Config;

@Config(modid = TwitchIntegration.MOD_ID, type = Config.Type.INSTANCE)
@Config.LangKey("twitchintegration.config")
public class TwitchIntegrationConfig {

	@Config.LangKey("twitchintegration.config.format")
	public static Format format = new Format();

	@Config.Name("Use Anonymous Login")
	public static boolean useAnonymousLogin = false;

	@Config.Name("Show Whispers")
	public static boolean showWhispers = false;

	@Config.Name("Port")
	public static int port = 6667;

	@Config.Name("Disable User Colors")
	public static boolean disableUserColors = false;

	@Config.Name("Disable Name Badges")
	public static boolean disableNameBadges = false;

	public static class Format {
		public static String singleMessageFormat = "%u: %m";
		public static String singleActionFormat = "%u %m";
		public static String whisperMessageFormat = "%u \u25b6 %r: %m";
		public static String whisperActionFormat = "%u \u25b6 %r : %m";
	}

}
