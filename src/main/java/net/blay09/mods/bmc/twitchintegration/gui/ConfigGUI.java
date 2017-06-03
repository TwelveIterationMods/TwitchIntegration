package net.blay09.mods.bmc.twitchintegration.gui;

import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegrationConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;

public class ConfigGUI extends GuiConfig {

	public ConfigGUI(GuiScreen parentScreen) {
		super(parentScreen, collectConfigElements(), TwitchIntegration.MOD_ID, "config", false, false, "Twitch Integration");
	}

	private static List<IConfigElement> collectConfigElements() {
		List<IConfigElement> toReturn;
		toReturn = ConfigElement.from(TwitchIntegrationConfig.class).getChildElements();
		return toReturn;
	}

}
