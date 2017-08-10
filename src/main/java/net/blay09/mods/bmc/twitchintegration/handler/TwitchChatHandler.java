package net.blay09.mods.bmc.twitchintegration.handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.blay09.javairc.IRCUser;
import net.blay09.javatmi.TMIAdapter;
import net.blay09.javatmi.TMIClient;
import net.blay09.javatmi.TwitchEmote;
import net.blay09.javatmi.TwitchUser;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.bmc.twitchintegration.util.TwitchHelper;
import net.blay09.mods.chattweaks.ChatManager;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatViewManager;
import net.blay09.mods.chattweaks.balyware.CachedAPI;
import net.blay09.mods.chattweaks.chat.ChatChannel;
import net.blay09.mods.chattweaks.chat.ChatMessage;
import net.blay09.mods.chattweaks.chat.ChatView;
import net.blay09.mods.chattweaks.chat.emotes.EmoteScanner;
import net.blay09.mods.chattweaks.chat.emotes.IEmote;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteScanner;
import net.blay09.mods.chattweaks.chat.emotes.PositionedEmote;
import net.blay09.mods.chattweaks.chat.emotes.twitch.TwitchAPI;
import net.blay09.mods.chattweaks.chat.emotes.twitch.TwitchGlobalEmotes;
import net.blay09.mods.chattweaks.chat.emotes.twitch.TwitchSubscriberEmotes;
import net.blay09.mods.chattweaks.image.ChatImage;
import net.blay09.mods.chattweaks.image.ChatImageDefault;
import net.blay09.mods.chattweaks.image.ChatImageEmote;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.ForgeHooks;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class TwitchChatHandler extends TMIAdapter {

	private static class ChannelUser {
		private final String channel;
		private final String username;

		public ChannelUser(String channel, String username) {
			this.channel = channel;
			this.username = username;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			ChannelUser that = (ChannelUser) o;
			return channel.equals(that.channel) && username.equals(that.username);
		}

		@Override
		public int hashCode() {
			int result = channel.hashCode();
			result = 31 * result + username.hashCode();
			return result;
		}
	}

	private static final Pattern PATTERN_ARGUMENT = Pattern.compile("%[ucmr]");
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("[HH:mm]");
	private final TwitchManager twitchManager;

	public TwitchChatHandler(TwitchManager twitchManager) {
		this.twitchManager = twitchManager;
	}

	private final Comparator<PositionedEmote> emoteComparator = Comparator.comparingInt(PositionedEmote::getStart);

	private final Predicate<IEmote> noTwitchEmotes = input -> !(input.getLoader() instanceof TwitchGlobalEmotes || input.getLoader() instanceof TwitchSubscriberEmotes);

	private final IEmoteScanner emoteScanner = new EmoteScanner();
	private final Multimap<ChannelUser, ChatMessage> messages = ArrayListMultimap.create();
	private final Map<String, TwitchUser> users = Maps.newHashMap();
	private Map<String, TwitchUser> thisUsers = Maps.newHashMap();

	private final List<ChatImage> tmpBadges = Lists.newArrayList();
	private final List<ChatImage> tmpEmotes = Lists.newArrayList();
	private final List<String> tmpBadgeNames = Lists.newArrayList();

	@Override
	public void onUserState(TMIClient client, String channel, TwitchUser user) {
		thisUsers.put(channel, user);
	}

	@Override
	public void onChatMessage(TMIClient client, String channel, TwitchUser user, TwitchMessage message) {
		onTwitchChat(client, twitchManager.isMultiMode() ? TwitchIntegrationConfig.Format.multiMessageFormat : TwitchIntegrationConfig.Format.singleMessageFormat, channel, user, message);
	}

	public void onTwitchChat(final TMIClient client, final String format, final String channel, final TwitchUser user, final TwitchMessage twitchMessage) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);

			// If subscriber-only chat is enabled client-side, ignore messages from non-subscribers
			if (twitchChannel != null && twitchChannel.isSubscribersOnly() && !user.isSubscriber() && !user.isMod()) {
				return;
			}

			// Fetch the channel id from the message if it's not known yet
			if (twitchChannel != null && twitchChannel.getId() == -1 && twitchMessage.getChannelId() != -1) {
				twitchChannel.setId(twitchMessage.getChannelId());
			}

			boolean isSelf = user.getNick().equals(client.getIRCConnection().getNick());

			// If the channel id is not known yet (because this is the first message sent and it's from this client), poll the API instead
			if (twitchChannel != null && twitchChannel.getId() == -1 && isSelf) {
				JsonObject object = CachedAPI.loadCachedAPI("https://api.twitch.tv/kraken/users?login=" + twitchChannel.getName() + "&client_id=" + TwitchHelper.OAUTH_CLIENT_ID, "twitch_" + twitchChannel.getName(), "application/vnd.twitchtv.v5+json");
				if (object != null && object.has("users")) {
					JsonArray array = object.getAsJsonArray("users");
					if(array.size() > 0) {
						JsonObject userObject = array.get(0).getAsJsonObject();
						if(userObject.has("_id")) {
							twitchChannel.setId(userObject.get("_id").getAsInt());
						}
					}
				}
			}

			// Apply Twitch Emotes
			tmpEmotes.clear();
			List<PositionedEmote> emoteList = (isSelf && !user.hasEmotes()) ? emoteScanner.scanForEmotes(twitchMessage.getMessage(), null) : emoteScanner.scanForEmotes(twitchMessage.getMessage(), noTwitchEmotes);
			if (user.hasEmotes()) {
				for (TwitchEmote twitchEmote : user.getEmotes()) {
					IEmote emote = TwitchAPI.getEmoteById(twitchEmote.getId());
					if (emote != null) {
						emoteList.add(new PositionedEmote(emote, twitchEmote.getStart(), twitchEmote.getEnd()));
					}
				}
			}
			emoteList.sort(emoteComparator);

			// Apply Emotes
			int index = 0;
			StringBuilder sb = new StringBuilder();
			for (PositionedEmote emoteData : emoteList) {
				if (index < emoteData.getStart()) {
					sb.append(twitchMessage.getMessage().substring(index, emoteData.getStart())).append(' '); // This space is definitely not a dirty hack, don't worry
				}
				int imageIndex = sb.length() + 1;
				sb.append("§*");
				for (int i = 0; i < emoteData.getEmote().getWidthInSpaces(); i++) {
					sb.append(' ');
				}
				tmpEmotes.add(new ChatImageEmote(imageIndex, emoteData.getEmote()));
				index = emoteData.getEnd() + 1;
			}
			if (index < twitchMessage.getMessage().length()) {
				sb.append(twitchMessage.getMessage().substring(index));
			}
			String message = sb.toString();

			// Apply Name Badges
			tmpBadges.clear();
			int badgeIndex = 0;
			if (isSelf && !user.hasBadges()) {
				tmpBadgeNames.clear();
				if (channel.equals("#" + user.getNick())) {
					tmpBadgeNames.add("broadcaster");
				} else if (user.isMod()) {
					tmpBadgeNames.add("moderator");
				}
				if (user.isTurbo()) {
					tmpBadgeNames.add("turbo");
				}
				if (user.isSubscriber()) {
					tmpBadgeNames.add("subscriber/" + user.getSubscribedMonths());
				}
				if (user.getCheeredBits() > 0) {
					tmpBadgeNames.add("bits/" + user.getCheeredBits());
				}
				user.setBadges(tmpBadgeNames.toArray(new String[tmpBadgeNames.size()]));
			}
			if (user.hasBadges()) {
				for (String badgeName : user.getBadges()) {
					int slash = badgeName.indexOf('/');
					int slashVal = 0;
					if (slash != -1) {
						slashVal = Integer.parseInt(badgeName.substring(slash + 1, badgeName.length()));
						badgeName = badgeName.substring(0, slash);
					}
					TwitchBadge badge;
					switch (badgeName) {
						case "subscriber":
							badge = TwitchBadge.getSubscriberBadge(twitchChannel, slashVal);
							break;
						case "bits":
							badge = TwitchBadge.getBadge(badgeName + slashVal);
							break;
						default:
							badge = TwitchBadge.getBadge(badgeName);
							break;
					}
					if (badge != null) {
						ChatImage image = new ChatImageDefault(badgeIndex, badge.getChatRenderable(), badge.getTooltipProvider());
						badgeIndex += image.getSpaces();
						tmpBadges.add(image);
					}
				}
			}

			ChatChannel targetChannel = twitchChannel != null ? twitchChannel.getChatChannel() : null;

			// Format Message
			ITextComponent textComponent = formatComponent(format, channel, user, message, tmpBadges, tmpEmotes, null, twitchMessage.isAction()); // TODO ensure chatters can't §fuckshitup
			ChatMessage chatMessage = ChatTweaks.createChatMessage(textComponent);
			chatMessage.setManaged(true);
			chatMessage.withRGB(twitchMessage.isAction() ? 2 : 1);
			for (ChatImage chatImage : tmpBadges) {
				chatMessage.addImage(chatImage);
			}
			for (ChatImage chatImage : tmpEmotes) {
				chatMessage.addImage(chatImage);
			}
			if (user.hasColor()) {
				int nameColor = ChatTweaks.colorFromHex(user.getColor());
				chatMessage.setRGBColor(0, nameColor);
			} else {
				chatMessage.setRGBColor(0, 0x808080);
			}
			if (twitchMessage.isAction()) {
				if (user.hasColor()) {
					int nameColor = ChatTweaks.colorFromHex(user.getColor());
					chatMessage.setRGBColor(1, nameColor);
				} else {
					chatMessage.setRGBColor(1, 0x808080);
				}
			}

			messages.put(new ChannelUser(channel, user.getNick().toLowerCase()), chatMessage);
			users.put(user.getNick().toLowerCase(), user);
			ChatTweaks.addChatMessage(chatMessage, targetChannel);
		});
	}

	@Override
	public void onSubscribe(TMIClient client, final String channel, final String username, final boolean prime) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);
			if (twitchManager.isMultiMode()) {
				ChatTweaks.addChatMessage(new TextComponentTranslation(TwitchIntegration.MOD_ID + (prime ? ":chat.subscribePrimeMulti": ":chat.subscribeMulti"), channel, username), twitchChannel != null ? twitchChannel.getChatChannel() : null);
			} else {
				ChatTweaks.addChatMessage(new TextComponentTranslation(TwitchIntegration.MOD_ID + (prime ? ":chat.subscribePrime": ":chat.subscribe"), username), twitchChannel != null ? twitchChannel.getChatChannel() : null);
			}
		});
	}

	@Override
	public void onResubscribe(TMIClient client, final String channel, final TwitchUser user, final int months, String message) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);
			if (twitchManager.isMultiMode()) {
				ChatTweaks.addChatMessage(new TextComponentTranslation(TwitchIntegration.MOD_ID + ":chat.resubscribeMulti", channel, user.getDisplayName(), months), twitchChannel != null ? twitchChannel.getChatChannel() : null);
			} else {
				ChatTweaks.addChatMessage(new TextComponentTranslation(TwitchIntegration.MOD_ID + ":chat.resubscribe", user.getDisplayName(), months), twitchChannel != null ? twitchChannel.getChatChannel() : null);
			}
		});
		if (message != null)
			onTwitchChat(client, twitchManager.isMultiMode()? TwitchIntegrationConfig.Format.multiMessageFormat : TwitchIntegrationConfig.Format.singleMessageFormat, channel, user, new TwitchMessage(message, -1, false, 0));
	}

	@Override
	public void onWhisperMessage(TMIClient client, TwitchUser user, String message) {
		onWhisperMessage(client, user, getThisUser(client, null), message);
	}

	public void onWhisperMessage(final TMIClient client, final TwitchUser user, final TwitchUser receiver, final String message) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			if (TwitchIntegrationConfig.showWhispers) {
				boolean isSelf = user.getNick().equals(client.getIRCConnection().getNick());

				// Apply Twitch Emotes
				tmpEmotes.clear();
				List<PositionedEmote> emoteList = (isSelf && !user.hasEmotes()) ? emoteScanner.scanForEmotes(message, null) : emoteScanner.scanForEmotes(message, noTwitchEmotes);
				if (user.hasEmotes()) {
					for (TwitchEmote twitchEmote : user.getEmotes()) {
						IEmote emote = TwitchAPI.getEmoteById(twitchEmote.getId());
						if (emote != null) {
							emoteList.add(new PositionedEmote(emote, twitchEmote.getStart(), twitchEmote.getEnd()));
						}
					}
				}
				emoteList.sort(emoteComparator);

				// Apply Emotes
				int index = 0;
				StringBuilder sb = new StringBuilder();
				for (PositionedEmote emoteData : emoteList) {
					if (index < emoteData.getStart()) {
						sb.append(message.substring(index, emoteData.getStart()));
					}
					int imageIndex = sb.length() + 1;
					sb.append("§*");
					for (int i = 0; i < emoteData.getEmote().getWidthInSpaces(); i++) {
						sb.append(' ');
					}
					tmpEmotes.add(new ChatImageEmote(imageIndex, emoteData.getEmote()));
					index = emoteData.getEnd() + 1;
				}
				if (index < message.length()) {
					sb.append(message.substring(index));
				}
				String message1 = sb.toString();

				ChatChannel whisperChannel = ChatManager.getTemporaryChannel("(" + (isSelf ? receiver.getDisplayName() : user.getDisplayName()) + ")");

				// Format Message
				String format = TwitchIntegrationConfig.Format.whisperMessageFormat;
				boolean isAction = message1.startsWith("/me ") && message1.length() > 4;
				if (isAction) {
					format = TwitchIntegrationConfig.Format.whisperActionFormat;
					message1 = message1.substring(4);
				}
				format = TextFormatting.GRAY + dateFormat.format(new Date()) + " " + TextFormatting.RESET + format;
				ITextComponent textComponent = formatComponent(format, null, user, message1, null, tmpEmotes, receiver, isAction);
				ChatMessage chatMessage = ChatTweaks.createChatMessage(textComponent);
				chatMessage.setManaged(true);
				chatMessage.withRGB(isAction ? 3 : 2);
				for (ChatImage chatImage : tmpEmotes) {
					chatMessage.addImage(chatImage);
				}
				if (user.hasColor()) {
					int nameColor = ChatTweaks.colorFromHex(user.getColor());
					chatMessage.setRGBColor(0, nameColor);
				} else {
					chatMessage.setRGBColor(0, 0x808080);
				}
				if (receiver.hasColor()) { // TODO this assumes that receiver is always in second place, which makes sense but isn't perfect
					int nameColor = ChatTweaks.colorFromHex(receiver.getColor());
					chatMessage.setRGBColor(1, nameColor);
				} else {
					chatMessage.setRGBColor(1, 0x808080);
				}
				if (isAction) { // TODO this assumes that message is always in third place, which makes sense but isn't perfect
					if (user.hasColor()) {
						int nameColor = ChatTweaks.colorFromHex(user.getColor());
						chatMessage.setRGBColor(2, nameColor);
					} else {
						chatMessage.setRGBColor(2, 0x808080);
					}
				}

				ChatView whisperView = ChatViewManager.getOrCreateChatView(whisperChannel.getName());
				whisperView.addChannel(whisperChannel);
				whisperView.setOutgoingPrefix("/twitch " + (isSelf ? receiver.getNick().toLowerCase() : user.getNick().toLowerCase()) + " ");
				whisperView.setTemporary(true);

				users.put(user.getNick().toLowerCase(), user);
				ChatTweaks.addChatMessage(chatMessage, whisperChannel);
			}
		});
	}

	@Override
	public void onTimeout(TMIClient client, final String channel, final String username) { // TODO test me
		Minecraft.getMinecraft().addScheduledTask(() -> {
			TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);
			if (twitchChannel != null) {
				switch (twitchChannel.getDeletedMessages()) {
					case HIDE:
						for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
							ChatManager.removeChatLine(message.getId());
						}
						ChatTweaks.refreshChat();
						break;
					case STRIKETHROUGH:
						for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
							message.getTextComponent().getStyle().setStrikethrough(true);
						}
						ChatTweaks.refreshChat();
						break;
					case REPLACE:
						TwitchUser user = users.get(username);
						if (user == null) {
							user = new TwitchUser(new IRCUser(username, null, null));
						}
						for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
							ITextComponent removedComponent = formatComponent(twitchManager.isMultiMode() ? TwitchIntegrationConfig.Format.multiMessageFormat : TwitchIntegrationConfig.Format.singleMessageFormat, channel, user, TextFormatting.GRAY + "<message deleted>", null, null, null, false);
							message.setTextComponent(removedComponent);
							message.clearImages();
						}
						ChatTweaks.refreshChat();
						break;
				}
			}
		});
	}

	@Override
	public void onClearChat(TMIClient client, final String channel) {
		Minecraft.getMinecraft().addScheduledTask(() -> {
			for (ChatMessage message : messages.values()) {
				ChatManager.removeChatLine(message.getId());
			}
			ChatTweaks.refreshChat();
		});
	}

	@Override
	public void onUnhandledException(TMIClient client, final Exception e) {
		e.printStackTrace();
		Minecraft.getMinecraft().addScheduledTask(() -> {
			if(Minecraft.getMinecraft().player != null) {
				Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Twitch Integration encountered an unhandled exception. The connection has been terminated. Please review your log files and let the mod developer know."), false);
			}
		});
		twitchManager.disconnect();
	}

	public static ITextComponent formatComponent(String format, @Nullable String channel, TwitchUser user, String message, @Nullable List<ChatImage> nameBadges, @Nullable List<ChatImage> emotes, @Nullable TwitchUser whisperReceiver, boolean isAction) {
		String[] parts = format.split("(?<=" + PATTERN_ARGUMENT + ")|(?=" + PATTERN_ARGUMENT + ")"); // TODO cache this
		TextComponentString root = null;
		for(String key : parts) {
			if(key.charAt(0) == '%') {
				if(root == null) {
					root = new TextComponentString("");
				}
				switch(key.charAt(1)) {
					case 'c':
						root.appendText(channel != null ? channel : "%c");
						break;
					case 'u':
						StringBuilder sb = new StringBuilder();
						if(nameBadges != null) {
							for (ChatImage chatImage : nameBadges) {
								chatImage.setIndex(chatImage.getIndex() + root.getFormattedText().length());
								sb.append("§*");
								for(int i = 0; i < chatImage.getSpaces(); i++) {
									sb.append(' ');
								}
							}
						}
						ITextComponent userComponent = new TextComponentString(sb.toString() + ChatTweaks.TEXT_FORMATTING_RGB + user.getDisplayName() + ChatTweaks.TEXT_FORMATTING_RGB);
						root.appendSibling(userComponent);
						break;
					case 'r':
						if(whisperReceiver != null) {
							ITextComponent receiverComponent = new TextComponentString(ChatTweaks.TEXT_FORMATTING_RGB + whisperReceiver.getDisplayName() + ChatTweaks.TEXT_FORMATTING_RGB);
							root.appendSibling(receiverComponent);
						} else {
							root.appendText("%r");
						}
						break;
					case 'm':
						if(emotes != null) {
							for (ChatImage chatImage : emotes) {
								chatImage.setIndex(chatImage.getIndex() + root.getFormattedText().length());
							}
						}
						message = TextFormatting.getTextWithoutFormattingCodes(message);
						root.appendSibling(ForgeHooks.newChatWithLinks(isAction ? ChatTweaks.TEXT_FORMATTING_RGB + message : message));
						break;
				}
			} else {
				if(root == null) {
					root = new TextComponentString(key);
				} else {
					root.appendSibling(new TextComponentString(key));
				}
			}
		}
		if(root == null) {
			root = new TextComponentString(format);
		}
		return root;
	}

	public TwitchUser getThisUser(TMIClient client, String channel) {
		if (channel == null)
			channel = !thisUsers.isEmpty() ? thisUsers.keySet().iterator().next(): "";

		if(!thisUsers.containsKey(channel)) {
			thisUsers.put(channel, new TwitchUser(new IRCUser(client.getIRCConnection().getNick(), null, null)));
		}
		return thisUsers.get(channel);
	}

	public TwitchUser getUser(String username) {
		return users.computeIfAbsent(username.toLowerCase(), k -> new TwitchUser(new IRCUser(username, null, null)));
	}
}
