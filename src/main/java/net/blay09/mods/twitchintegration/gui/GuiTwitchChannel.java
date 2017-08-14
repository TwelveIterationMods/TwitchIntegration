package net.blay09.mods.twitchintegration.gui;

import com.google.common.collect.Lists;
import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.blay09.mods.twitchintegration.handler.TwitchChannel;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatViewManager;
import net.blay09.mods.chattweaks.chat.ChatView;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraftforge.fml.client.config.ConfigGuiType;
import net.minecraftforge.fml.client.config.GuiConfig;
import net.minecraftforge.fml.client.config.IConfigElement;

import java.util.List;

public class GuiTwitchChannel extends GuiConfig {

	private final boolean isNew;
	private final TwitchChannel twitchChannel;
	private static SmartyConfigElement nameElement;
	private static SmartyConfigElement subscribersOnlyElement;
	private static SmartyConfigElement deletedMessagesElement;
	private static SmartyConfigElement activeElement;

	public GuiTwitchChannel(GuiScreen parentScreen, TwitchChannel twitchChannel, boolean isNew) {
		super(parentScreen, getConfigElements(twitchChannel), ChatTweaks.MOD_ID, "config", false, false, "Twitch Channel Configuration");
		this.twitchChannel = twitchChannel;
		this.isNew = isNew;
	}

	@Override
	protected void actionPerformed(GuiButton button) {
		if (button.id == 2000) {
			entryList.saveConfigElements();

			if (!twitchChannel.getName().equalsIgnoreCase((String) nameElement.get())) {
				TwitchIntegration.getTwitchManager().renameTwitchChannel(twitchChannel, (String) nameElement.get());
			}
			twitchChannel.setSubscribersOnly((boolean) subscribersOnlyElement.get());
			twitchChannel.setDeletedMessages(TwitchChannel.DeletedMessages.valueOf((String) deletedMessagesElement.get()));
			twitchChannel.setActive((boolean) activeElement.get());

			TwitchIntegration.getTwitchManager().saveChannels();

			ChatView chatView = ChatViewManager.getChatView(twitchChannel.getName());
			if(isNew && chatView == null) {
				twitchChannel.createDefaultView();
			}

			mc.displayGuiScreen(parentScreen);
			return;
		}
		super.actionPerformed(button);
	}

	private static List<IConfigElement> getConfigElements(TwitchChannel twitchChannel) {
		List<IConfigElement> list = Lists.newArrayList();

		nameElement = new SmartyConfigElement("Name", twitchChannel.getName(), ConfigGuiType.STRING, "twitchintegration.config.channel.name");
		list.add(nameElement);

		TwitchChannel.DeletedMessages[] deletedMessages = TwitchChannel.DeletedMessages.values();
		String[] deletedMessagesNames = new String[deletedMessages.length];
		for (int i = 0; i < deletedMessages.length; i++) {
			deletedMessagesNames[i] = deletedMessages[i].name();
		}
		deletedMessagesElement = new SmartyConfigElement("Deleted Messages", "Replace", ConfigGuiType.STRING, "twitchintegration.config.channel.deleted_messages", deletedMessagesNames);
		deletedMessagesElement.set(twitchChannel.getDeletedMessages().name());
		list.add(deletedMessagesElement);

		subscribersOnlyElement = new SmartyConfigElement("Subscribers only", false, ConfigGuiType.BOOLEAN, "twitchintegration.config.channel.subscribers_only");
		subscribersOnlyElement.set(twitchChannel.isSubscribersOnly());
		list.add(subscribersOnlyElement);

		activeElement = new SmartyConfigElement("Active", true, ConfigGuiType.BOOLEAN, "twitchintegration.config.channel.active");
		activeElement.set(twitchChannel.isActive());
		list.add(activeElement);

		return list;
	}

}
