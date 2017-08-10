package net.blay09.mods.bmc.twitchintegration;

import net.blay09.mods.bmc.twitchintegration.handler.TwitchBadge;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchChatHandler;
import net.blay09.mods.bmc.twitchintegration.handler.TwitchManager;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.FMLNetworkEvent;

import java.io.File;

@Mod(modid = TwitchIntegration.MOD_ID, name = "Twitch Integration", acceptedMinecraftVersions = "[1.11.2]", clientSideOnly = true, dependencies = "required-after:chattweaks",
guiFactory = "net.blay09.mods.bmc.twitchintegration.gui.GuiFactory")
public class TwitchIntegration {

	public static final String MOD_ID = "twitchintegration";

	@Mod.Instance(MOD_ID)
	public static TwitchIntegration instance;

	private TwitchManager twitchManager;
	private TwitchChatHandler twitchChatHandler;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent event) {
		MinecraftForge.EVENT_BUS.register(this);

		ClientCommandHandler.instance.registerCommand(new CommandTwitch());
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		twitchManager = new TwitchManager(new File(Minecraft.getMinecraft().mcDataDir, "config/ChatTweaks/twitch_channels.json"));
		twitchChatHandler = new TwitchChatHandler(twitchManager);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent event) {
		TwitchBadge.loadInbuiltBadge("broadcaster");
		TwitchBadge.loadInbuiltBadge("moderator");
		TwitchBadge.loadInbuiltBadge("turbo");
		TwitchBadge.loadInbuiltBadge("staff");
		TwitchBadge.loadInbuiltBadge("admin");
		TwitchBadge.loadInbuiltBadge("global_mod");
		TwitchBadge.loadInbuiltBadge("premium");
		TwitchBadge.loadInbuiltBadge("bits1");
		TwitchBadge.loadInbuiltBadge("bits100");
		TwitchBadge.loadInbuiltBadge("bits1000");
		TwitchBadge.loadInbuiltBadge("bits5000");
		TwitchBadge.loadInbuiltBadge("bits10000");
		TwitchBadge.loadInbuiltBadge("bits100000");

		// TODO needs changes when we update to new channel badges
	}

	@SubscribeEvent
	public void onWorldJoined(FMLNetworkEvent.ClientConnectedToServerEvent event) {
		if(!twitchManager.isConnected()) {
			twitchManager.connect();
		}
	}

	public static TwitchChatHandler getTwitchChatHandler() {
		return instance.twitchChatHandler;
	}

	public static TwitchManager getTwitchManager() {
		return instance.twitchManager;
	}
}
