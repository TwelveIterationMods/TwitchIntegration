package net.blay09.mods.bmc.twitchintegration;

import net.blay09.mods.bmc.twitchintegration.handler.TwitchChannel;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchChatHandler;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchManager;
import net.blay09.mods.chattweaks.chat.emotes.twitch.BTTVChannelEmotes;
import net.blay09.mods.chattweaks.chat.emotes.twitch.FFZChannelEmotes;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = TwitchIntegration.MOD_ID, name = "Twitch Integration", acceptedMinecraftVersions = "[1.11.2]", clientSideOnly = true, dependencies = "required-after:chattweaks",
guiFactory = "net.blay09.mods.bmc.twitchintegration.gui.GuiFactory")
public class TwitchIntegration {

	public static final String MOD_ID = "twitchintegration";
	// TODO Edit Message Format is empty

	@Mod.Instance(MOD_ID)
	public static TwitchIntegration instance;

	public static Logger logger = LogManager.getLogger(MOD_ID);

	private TwitchManager twitchManager;
	private TwitchChatHandler twitchChatHandler;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);

		ClientCommandHandler.instance.registerCommand(new CommandTwitch());
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		twitchManager = new TwitchManager(new File(Minecraft.getMinecraft().mcDataDir, "config/TwitchIntegration/twitch_channels.json"));
		twitchChatHandler = new TwitchChatHandler(twitchManager);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {}

	@SubscribeEvent
	public void onWorldJoined(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		if(!twitchManager.isConnected()) {
			twitchManager.connect();
		}
	}

	@SubscribeEvent
	public void onConfigChanged(ConfigChangedEvent event) {
		if(MOD_ID.equals(event.getModID())) {
			ConfigManager.sync(MOD_ID, Config.Type.INSTANCE);
		}
	}

	public static TwitchChatHandler getTwitchChatHandler() {
		return instance.twitchChatHandler;
	}

	public static TwitchManager getTwitchManager() {
		return instance.twitchManager;
	}

	// Let's just put this here for now...
	public static void loadChannelEmotes(TwitchChannel channel) {
		new Thread(() -> {
			try {
				new BTTVChannelEmotes(channel.getName());
			} catch (Exception e) {
				TwitchIntegration.logger.error("Failed to load BTTV channel emotes: ", e);
			}
			try {
				new FFZChannelEmotes(channel.getName());
			} catch (Exception e) {
				TwitchIntegration.logger.error("Failed to load FFZ channel emotes: ", e);
			}
		}).start();
	}
}
