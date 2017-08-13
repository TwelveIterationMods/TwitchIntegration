package net.blay09.mods.bmc.twitchintegration;

import net.minecraftforge.common.config.Config;

@Config(modid = TwitchIntegration.MOD_ID, type = Config.Type.INSTANCE)
@Config.LangKey("twitchintegration.config")
public class TwitchIntegrationConfig {

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

}
