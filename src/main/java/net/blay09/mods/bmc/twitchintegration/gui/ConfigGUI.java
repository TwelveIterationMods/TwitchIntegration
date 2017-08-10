package net.blay09.mods.bmc.twitchintegration.gui;

import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegrationConfig;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;

public class ConfigGUI extends GuiConfig {

	public ConfigGUI(GuiScreen parentScreen) {
		super(parentScreen, collectConfigElements(), TwitchIntegration.MOD_ID, "config", false, false, "Twitch Integration");
	}

	private static List<IConfigElement> collectConfigElements() {
		List<IConfigElement> list;
		// TODO put buttons on top and color them?
		list = ConfigElement.from(TwitchIntegrationConfig.class).getChildElements();
		list.add(new DummyConfigElement.DummyCategoryElement("Authenticate", "authenticate", ConfigEntryAuthenticate.class)); // i18n, change to Edit Authentication once authenticated
		return list;
	}

	public static class ConfigEntryAuthenticate extends GuiConfigEntries.CategoryEntry {
		public ConfigEntryAuthenticate(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
		}

		@Override
		protected GuiScreen buildChildScreen() {
			return new GuiTwitchAuthentication(owningScreen);
		}
	}

}
