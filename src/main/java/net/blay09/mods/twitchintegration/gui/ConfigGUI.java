package net.blay09.mods.twitchintegration.gui;

import com.google.common.collect.Lists;
import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.config.ConfigElement;
import net.minecraftforge.fml.client.config.DummyConfigElement;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

import javax.annotation.Nullable;
import java.util.List;

public class ConfigGUI extends GuiConfig {

	public ConfigGUI(GuiScreen parentScreen) {
		super(parentScreen, collectConfigElements(), TwitchIntegration.MOD_ID, "config", false, false, "Twitch Integration");
	}

	private static List<IConfigElement> collectConfigElements() {
		List<IConfigElement> list = Lists.newArrayList();

		boolean isAuthenticated = ChatTweaks.getAuthManager().getToken(TwitchIntegration.MOD_ID) != null;
		boolean isConnected = TwitchIntegration.getTwitchManager().isConnected();

		String connectLangKey = isConnected ? "twitchintegration.config.disconnect" : "twitchintegration.config.connect";
		list.add(new DummyConfigElement.DummyCategoryElement(connectLangKey, connectLangKey, ConfigEntryConnect.class));

		String authLangKey = isAuthenticated ? "twitchintegration.config.edit_authentication" : "twitchintegration.config.authenticate";
		list.add(new DummyConfigElement.DummyCategoryElement(authLangKey, authLangKey, ConfigEntryAuthenticate.class));

		list.add(GuiTwitchChannelsConfig.getDummyElement());

		list.addAll(ConfigElement.from(TwitchIntegrationConfig.class).getChildElements());

		// I was going to just make it an entry that immediately updates the property, but some genius made the BooleanEntry constructor private
		list.removeIf(p -> p.getName().equals("Use Anonymous Login"));

		return list;
	}

	public static class ConfigEntryConnect extends GuiConfigEntries.CategoryEntry {

		private boolean enabled;

		public ConfigEntryConnect(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
		}

		@Override
		public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			if (btnSelectCategory.mousePressed(this.mc, x, y)) {
				btnSelectCategory.playPressSound(mc.getSoundHandler());
				if(TwitchIntegration.getTwitchManager().isConnected()) {
					TwitchIntegration.getTwitchManager().disconnect();
				} else {
					TwitchIntegration.getTwitchManager().connect();
				}
				return true;
			}
			return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
			updateButton();
			super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
		}

		@Override
		@Nullable
		protected GuiScreen buildChildScreen() {
			return null;
		}

		private void updateButton() {
			boolean isAuthenticated = TwitchIntegrationConfig.useAnonymousLogin || ChatTweaks.getAuthManager().getToken(TwitchIntegration.MOD_ID) != null;
			boolean isConnected = TwitchIntegration.getTwitchManager().isConnected();
			btnSelectCategory.displayString = I18n.format(isConnected ? "twitchintegration.config.disconnect" : "twitchintegration.config.connect");
			if (isAuthenticated) {
				btnSelectCategory.displayString = TextFormatting.GREEN + btnSelectCategory.displayString;
				enabled = true;
			} else {
				enabled = isConnected;
			}
		}

		@Override
		public boolean enabled() {
			return enabled;
		}
	}

	public static class ConfigEntryAuthenticate extends GuiConfigEntries.CategoryEntry {
		public ConfigEntryAuthenticate(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partial) {
			boolean isAuthenticated = ChatTweaks.getAuthManager().getToken(TwitchIntegration.MOD_ID) != null;
			btnSelectCategory.displayString = I18n.format(isAuthenticated ? "twitchintegration.config.edit_authentication" : "twitchintegration.config.authenticate");
			if (ChatTweaks.getAuthManager().getToken(TwitchIntegration.MOD_ID) == null && !TwitchIntegrationConfig.useAnonymousLogin) {
				btnSelectCategory.displayString = TextFormatting.GREEN + btnSelectCategory.displayString;
			}
			super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected, partial);
		}

		@Override
		protected GuiScreen buildChildScreen() {
			return new GuiTwitchAuthentication(owningScreen);
		}
	}

}
