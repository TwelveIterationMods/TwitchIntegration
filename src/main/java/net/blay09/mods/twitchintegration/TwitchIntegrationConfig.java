package net.blay09.mods.twitchintegration;

import net.minecraftforge.common.config.Config;

@Config(modid = TwitchIntegration.MOD_ID, type = Config.Type.INSTANCE)
@Config.LangKey("twitchintegration.config")
public class TwitchIntegrationConfig {

	@Config.Name("Use Anonymous Login")
	@Config.Comment("If you login anonymously you can read chat, but you will not be able to type to Twitch chat from within Minecraft.")
	public static boolean useAnonymousLogin = false;

	@Config.Name("Show Whispers")
	public static boolean showWhispers = false;

	@Config.Name("Port")
	public static int port = 6667;

	@Config.Name("Disable User Colors")
	public static boolean disableUserColors = false;

	@Config.Name("Disable Name Badges")
	public static boolean disableNameBadges = false;

	@Config.Name("Do not store token")
	@Config.Comment("Set this if you're on a public computer or concerned about security. You will have to re-authenticate every time you start Minecraft.")
	public static boolean doNotStoreToken = false;

	@Config.Name("User Blacklist")
	@Config.Comment("Messages by these users will not display in chat. Useful to hide bots for example.")
	public static String[] userBlacklist = new String[0];
}
