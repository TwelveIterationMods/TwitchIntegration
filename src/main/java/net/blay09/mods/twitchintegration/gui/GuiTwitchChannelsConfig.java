package net.blay09.mods.twitchintegration.gui;

import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.blay09.mods.twitchintegration.handler.TwitchChannel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.GuiConfigEntries;
import net.minecraftforge.fml.client.config.GuiEditArray;
import net.minecraftforge.fml.client.config.GuiEditArrayEntries;
import net.minecraftforge.fml.client.config.IConfigElement;

public class GuiTwitchChannelsConfig extends GuiEditArray {

	public GuiTwitchChannelsConfig(GuiScreen parentScreen, IConfigElement configElement, int slotIndex, Object[] currentValues, boolean enabled) {
		super(parentScreen, configElement, slotIndex, currentValues, enabled);
	}

	@Override
	public void initGui() {
		super.initGui();

		fixForge();
	}

	private void fixForge() { // TODO remove in 1.12
		entryList = new TwitchChannelEditArrayEntries(this, mc, configElement, beforeValues, currentValues);

		// Workaround for a Forge bug where it would call constructors of existing elements with .toString() ... fixed in 1.12
		// Basically we just repopulate the entire list with proper constructor calls
		entryList.listEntries.clear();
		for (Object twitchChannel : currentValues) {
			entryList.listEntries.add(new TwitchChannelArrayEntry(this, entryList, configElement, twitchChannel));
		}
		entryList.listEntries.add(new GuiEditArrayEntries.BaseEntry(this, entryList, configElement));
	}

	public void saveAndUpdateList() {
		((TwitchChannelEditArrayEntries) entryList).saveList();
		currentValues = ((GuiConfig) parentScreen).entryList.getListEntry(slotIndex).getCurrentValues();
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		super.actionPerformed(button);

		if (button == btnUndoChanges) {
			fixForge();
		} else if(button == btnDefault) {
			fixForge();
		}
	}

	public static IConfigElement getDummyElement() {
		SmartyListElement dummy = new SmartyListElement("Channels", TwitchIntegration.getTwitchManager().createDefaults(), ConfigGuiType.STRING, "twitchintegration.config.twitch_channels");
		dummy.setConfigEntryClass(TwitchChannelConfigEntry.class);
		dummy.setArrayEntryClass(TwitchChannelArrayEntry.class);
		dummy.setCustomEditListEntryClass(TwitchChannelArrayEntry.class);
		dummy.set(TwitchIntegration.getTwitchManager().getChannels().toArray());
		return dummy;
	}

	public static class TwitchChannelEditArrayEntries extends GuiEditArrayEntries {

		public TwitchChannelEditArrayEntries(GuiEditArray parent, Minecraft mc, IConfigElement configElement, Object[] beforeValues, Object[] currentValues) {
			super(parent, mc, configElement, beforeValues, currentValues);
		}

		@Override
		public void addNewEntry(int index) {
			super.addNewEntry(index);

			// We need to force a list save here because it otherwise only saves on Done button
			((GuiTwitchChannelsConfig) owningGui).saveAndUpdateList();

			// Open the newly created entry immediately
			TwitchChannelArrayEntry entry = (TwitchChannelArrayEntry) listEntries.get(index);
			if(entry.twitchChannel != null) {
				mc.displayGuiScreen(new GuiTwitchChannel(owningGui, entry.twitchChannel, true));
			}
		}

		public void saveList() {
			saveListChanges();
		}
	}

	/**
	 * This is the config entry that's used for the main configuration screen (the "category" screen)
	 */
	public static class TwitchChannelConfigEntry extends GuiConfigEntries.ArrayEntry {
		protected final GuiButtonExt btnValueFixed;

		public TwitchChannelConfigEntry(GuiConfig owningScreen, GuiConfigEntries owningEntryList, IConfigElement configElement) {
			super(owningScreen, owningEntryList, configElement);
			btnValue.visible = false;
			drawLabel = false;

			btnValueFixed = new GuiButtonExt(0, 0, 0, 300, 18, I18n.format(name));
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
			super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected);

			btnValueFixed.xPosition = listWidth / 2 - 150;
			btnValueFixed.yPosition = y;
			btnValueFixed.enabled = enabled();
			btnValueFixed.drawButton(mc, mouseX, mouseY);
		}

		@Override
		public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			if (btnValueFixed.mousePressed(mc, x, y)) {
				btnValueFixed.playPressSound(mc.getSoundHandler());
				valueButtonPressed(index);
				updateValueButtonText();
				return true;
			} else {
				return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
			}
		}

		@Override
		public void valueButtonPressed(int slotIndex) {
			mc.displayGuiScreen(new GuiTwitchChannelsConfig(this.owningScreen, configElement, slotIndex, currentValues, enabled()));
		}

		@Override
		public boolean saveConfigElement() {
			TwitchIntegration.getTwitchManager().removeAllChannels();
			for(Object twitchChannel : currentValues) {
				TwitchIntegration.getTwitchManager().addChannel((TwitchChannel) twitchChannel);
			}
			TwitchIntegration.getTwitchManager().updateChannelStates();
			TwitchIntegration.getTwitchManager().saveChannels();
			return super.saveConfigElement();
		}
	}

	/**
	 * This is a single entry in the list of twitch channels.
	 */
	public static class TwitchChannelArrayEntry extends GuiEditArrayEntries.BaseEntry {
		private final GuiButtonExt button;
		private final TwitchChannel twitchChannel;

		public TwitchChannelArrayEntry(GuiEditArray owningScreen, GuiEditArrayEntries owningEntryList, IConfigElement configElement, Object value) {
			super(owningScreen, owningEntryList, configElement);

			if (value.equals("")) { // TODO fix in 1.12, see fixForge()
				twitchChannel = new TwitchChannel("");
			} else if (value instanceof TwitchChannel) {
				twitchChannel = (TwitchChannel) value;
			} else {
				twitchChannel = null;
			}

			button = new GuiButtonExt(0, 0, 0, owningEntryList.controlWidth - 12, 18, I18n.format(String.valueOf(value)));
		}

		@Override
		public void drawEntry(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected) {
			super.drawEntry(slotIndex, x, y, listWidth, slotHeight, mouseX, mouseY, isSelected);

			button.xPosition = listWidth / 4 + 12;
			button.yPosition = y;

			if(twitchChannel != null) {
				button.displayString = twitchChannel.getName();
				if(!twitchChannel.isActive()) {
					button.displayString += TextFormatting.DARK_AQUA + " (disabled)";
				}
			} else {
				button.displayString = "invalid";
			}

			button.drawButton(owningEntryList.getMC(), mouseX, mouseY);
		}

		@Override
		public boolean mousePressed(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			if (button.mousePressed(owningEntryList.getMC(), x, y)) {
				button.playPressSound(owningEntryList.getMC().getSoundHandler());
				((GuiTwitchChannelsConfig) owningScreen).saveAndUpdateList();
				owningScreen.mc.displayGuiScreen(new GuiTwitchChannel(owningScreen, twitchChannel, false));
				return true;
			}

			return super.mousePressed(index, x, y, mouseEvent, relativeX, relativeY);
		}

		@Override
		public void mouseReleased(int index, int x, int y, int mouseEvent, int relativeX, int relativeY) {
			button.mouseReleased(x, y);
			super.mouseReleased(index, x, y, mouseEvent, relativeX, relativeY);
		}

		@Override
		public Object getValue() {
			return twitchChannel;
		}
	}
}
