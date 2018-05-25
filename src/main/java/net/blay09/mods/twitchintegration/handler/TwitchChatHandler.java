package net.blay09.mods.twitchintegration.handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.blay09.javairc.IRCUser;
import net.blay09.javatmi.TMIAdapter;
import net.blay09.javatmi.TMIClient;
import net.blay09.javatmi.TwitchEmote;
import net.blay09.javatmi.TwitchUser;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.mods.twitchintegration.TwitchIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.chattweaks.ChatManager;
import net.blay09.mods.chattweaks.ChatTweaks;
import net.blay09.mods.chattweaks.ChatViewManager;
import net.blay09.mods.chattweaks.chat.ChatChannel;
import net.blay09.mods.chattweaks.chat.ChatMessage;
import net.blay09.mods.chattweaks.chat.ChatView;
import net.blay09.mods.chattweaks.chat.emotes.EmoteScanner;
import net.blay09.mods.chattweaks.chat.emotes.IEmote;
import net.blay09.mods.chattweaks.chat.emotes.IEmoteScanner;
import net.blay09.mods.chattweaks.chat.emotes.PositionedEmote;
import net.blay09.mods.chattweaks.chat.emotes.twitch.TwitchEmotesAPI;
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

import javax.annotation.Nullable;
import java.awt.*;
import java.util.*;
import java.util.List;
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

    private static final Pattern PATTERN_FORMAT = Pattern.compile("(?<=%[ucmr])|(?=%[ucmr])");
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
        onTwitchChat(client, channel, user, message);
    }

    public void onTwitchChat(final TMIClient client, final String channel, final TwitchUser user, final TwitchMessage twitchMessage) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);

            // If subscriber-only chat is enabled client-side, ignore messages from non-subscribers
            if (twitchChannel != null && twitchChannel.isSubscribersOnly() && !user.isSubscriber() && !user.isMod()) {
                return;
            }

            // Hide messages by users on the configured blacklist
            if (Arrays.stream(TwitchIntegrationConfig.userBlacklist).anyMatch(it -> user.getNick().equalsIgnoreCase(it))) {
				return;
            }

            // Fetch the channel id from the message if it's not known yet
            if (twitchChannel != null && twitchChannel.getId() == -1 && twitchMessage.getChannelId() != -1) {
                twitchChannel.setId(twitchMessage.getChannelId());
            }

            boolean isSelf = user.getNick().equals(client.getIRCConnection().getNick());

            // Apply Twitch Emotes
            tmpEmotes.clear();
            List<PositionedEmote> emoteList = (isSelf && !user.hasEmotes()) ? emoteScanner.scanForEmotes(twitchMessage.getMessage(), null) : emoteScanner.scanForEmotes(twitchMessage.getMessage(), noTwitchEmotes);
            if (user.hasEmotes()) {
                for (TwitchEmote twitchEmote : user.getEmotes()) {
                    IEmote emote = TwitchEmotesAPI.getEmoteById(twitchEmote.getId());
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
                sb.append(ChatTweaks.TEXT_FORMATTING_EMOTE);
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
            if (twitchChannel != null && user.hasBadges() && !TwitchIntegrationConfig.disableNameBadges) {
                for (String badgeName : user.getBadges()) {
                    int slash = badgeName.indexOf('/');
                    int version = 1;
                    if (slash != -1) {
                        version = Integer.parseInt(badgeName.substring(slash + 1, badgeName.length()));
                        badgeName = badgeName.substring(0, slash);
                    }
                    TwitchBadge badge = twitchChannel.getBadge(badgeName, version);
                    if (badge != null) {
                        ChatImage image = new ChatImageDefault(badgeIndex, badge.getChatRenderable(), badge.getTooltipProvider());
                        badgeIndex += image.getSpaces();
                        tmpBadges.add(image);
                    }
                }
            }

            ChatChannel targetChannel = twitchChannel != null ? twitchChannel.getChatChannel() : null;

            // Format Message
            ITextComponent senderComponent = formatSenderComponent(user, tmpBadges);
            ITextComponent messageComponent = formatMessageComponent(message, twitchMessage.isAction);
            ITextComponent textComponent = formatComponent(senderComponent, messageComponent, twitchMessage.isAction());
            ChatMessage chatMessage = ChatTweaks.createChatMessage(textComponent);
            chatMessage.setSender(senderComponent);
            chatMessage.setMessage(messageComponent);
            chatMessage.setOutputVar("c", formatChannelComponent(channel));
            chatMessage.withRGB(twitchMessage.isAction() ? 2 : 1);
            for (ChatImage chatImage : tmpBadges) {
                chatMessage.addImage(chatImage);
            }
            for (ChatImage chatImage : tmpEmotes) {
                chatMessage.addImage(chatImage);
            }
            if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
                int nameColor = ChatTweaks.colorFromHex(user.getColor());
                chatMessage.setRGBColor(0, getAcceptableNameColor(nameColor));
            } else {
                chatMessage.setRGBColor(0, 0x808080);
            }
            if (twitchMessage.isAction()) {
                if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
                    int nameColor = ChatTweaks.colorFromHex(user.getColor());
                    chatMessage.setRGBColor(1, getAcceptableNameColor(nameColor));
                } else {
                    chatMessage.setRGBColor(1, 0x808080);
                }
            }

            messages.put(new ChannelUser(channel, user.getNick().toLowerCase(Locale.ENGLISH)), chatMessage);
            users.put(user.getNick().toLowerCase(Locale.ENGLISH), user);
            ChatTweaks.addChatMessage(chatMessage, targetChannel);
        });
    }

    @Override
    public void onSubscribe(TMIClient client, final String channel, final String username, final boolean prime) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);
            ChatTweaks.addChatMessage(new TextComponentTranslation(TwitchIntegration.MOD_ID + (prime ? ":chat.subscribePrime" : ":chat.subscribe"), username), twitchChannel != null ? twitchChannel.getChatChannel() : null);
        });
    }

    @Override
    public void onResubscribe(TMIClient client, final String channel, final TwitchUser user, final int months, String message) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);
            ChatTweaks.addChatMessage(new TextComponentTranslation(TwitchIntegration.MOD_ID + ":chat.resubscribe", user.getDisplayName(), months), twitchChannel != null ? twitchChannel.getChatChannel() : null);
        });
        if (message != null) {
            onTwitchChat(client, channel, user, new TwitchMessage(message, -1, false, 0));
        }
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
                        IEmote emote = TwitchEmotesAPI.getEmoteById(twitchEmote.getId());
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
                    sb.append(ChatTweaks.TEXT_FORMATTING_EMOTE);
                    for (int i = 0; i < emoteData.getEmote().getWidthInSpaces(); i++) {
                        sb.append(' ');
                    }
                    tmpEmotes.add(new ChatImageEmote(imageIndex, emoteData.getEmote()));
                    index = emoteData.getEnd() + 1;
                }
                if (index < message.length()) {
                    sb.append(message.substring(index));
                }
                String transformedMessage = sb.toString();

                ChatChannel whisperChannel = ChatManager.getTemporaryChannel("(" + (isSelf ? receiver.getDisplayName() : user.getDisplayName()) + ")");

                // Format Message
                boolean isAction = transformedMessage.startsWith("/me ") && transformedMessage.length() > 4;
                if (isAction) {
                    transformedMessage = transformedMessage.substring(4);
                }
                ITextComponent senderComponent = formatSenderComponent(user, tmpBadges);
                ITextComponent messageComponent = formatMessageComponent(transformedMessage, isAction);
                ITextComponent textComponent = formatComponent(senderComponent, messageComponent, isAction);
                ChatMessage chatMessage = ChatTweaks.createChatMessage(textComponent);
                chatMessage.setSender(senderComponent);
                chatMessage.setMessage(messageComponent);
                chatMessage.setOutputVar("r", formatSenderComponent(receiver, null));
                chatMessage.withRGB(isAction ? 2 : 1);
                for (ChatImage chatImage : tmpEmotes) {
                    chatMessage.addImage(chatImage);
                }
                if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
                    int nameColor = ChatTweaks.colorFromHex(user.getColor());
                    chatMessage.setRGBColor(0, getAcceptableNameColor(nameColor));
                } else {
                    chatMessage.setRGBColor(0, 0x808080);
                }
                if (isAction) {
                    if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
                        int nameColor = ChatTweaks.colorFromHex(user.getColor());
                        chatMessage.setRGBColor(1, getAcceptableNameColor(nameColor));
                    } else {
                        chatMessage.setRGBColor(1, 0x808080);
                    }
                }

                ChatView whisperView = ChatViewManager.getChatView(whisperChannel.getName());
                if (whisperView == null) {
                    whisperView = new ChatView(whisperChannel.getName());
                    whisperView.addChannel(whisperChannel);
                    whisperView.setOutgoingPrefix("/twitch " + (isSelf ? receiver.getNick().toLowerCase(Locale.ENGLISH) : user.getNick().toLowerCase(Locale.ENGLISH)) + " ");
                    whisperView.setTemporary(true);
                    ChatViewManager.addChatView(whisperView);
                }

                users.put(user.getNick().toLowerCase(Locale.ENGLISH), user);
                ChatTweaks.addChatMessage(chatMessage, whisperChannel);
            }
        });
    }

    @Override
    public void onTimeout(TMIClient client, final String channel, final String username) {
        Minecraft.getMinecraft().addScheduledTask(() -> {
            TwitchChannel twitchChannel = twitchManager.getTwitchChannel(channel);
            if (twitchChannel != null) {
                switch (twitchChannel.getDeletedMessages()) {
                    case Hide:
                        for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
                            ChatManager.removeChatLine(message.getId());
                        }
                        ChatTweaks.refreshChat();
                        break;
                    case Strikethrough:
                        for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
                            message.getTextComponent().getStyle().setStrikethrough(true);
                        }
                        ChatTweaks.refreshChat();
                        break;
                    case Replace:
                        for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
                            ITextComponent removedMessageComponent = new TextComponentString("<message deleted>");
                            removedMessageComponent.getStyle().setColor(TextFormatting.GRAY);
                            message.setMessage(removedMessageComponent);
                            message.setTextComponent(formatComponent(message.getSender(), removedMessageComponent, false));
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
        TwitchIntegration.logger.error("Unhandled exception: ", e);
        Minecraft.getMinecraft().addScheduledTask(() -> {
            if (Minecraft.getMinecraft().player != null) {
                Minecraft.getMinecraft().player.sendStatusMessage(new TextComponentString(TextFormatting.RED + "Twitch Integration encountered an unhandled exception. The connection has been terminated. Please review your log files and let the mod developer know."), false);
            }
        });
        twitchManager.disconnect();
    }

    @Nullable
    public static ITextComponent formatSenderComponent(@Nullable TwitchUser user, @Nullable List<ChatImage> nameBadges) {
        if (user == null) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        if (nameBadges != null) {
            for (ChatImage chatImage : nameBadges) {
                sb.append(ChatTweaks.TEXT_FORMATTING_EMOTE);
                for (int i = 0; i < chatImage.getSpaces(); i++) {
                    sb.append(' ');
                }
            }
        }
        return new TextComponentString(sb.toString() + ChatTweaks.TEXT_FORMATTING_RGB + user.getDisplayName() + ChatTweaks.TEXT_FORMATTING_RGB);
    }

    public static ITextComponent formatMessageComponent(String message, boolean isAction) {
        message = TextFormatting.getTextWithoutFormattingCodes(message);
        assert message != null;
        return ForgeHooks.newChatWithLinks(message);
    }

    @Nullable
    public static ITextComponent formatChannelComponent(@Nullable String channel) {
        return channel != null ? new TextComponentString(channel) : null;
    }

    public static ITextComponent formatComponent(ITextComponent senderComponent, ITextComponent messageComponent, boolean isAction) {
        ITextComponent textComponent = new TextComponentString("");
        textComponent.appendSibling(senderComponent);
        textComponent.appendText(isAction ? " " : ": ");
        textComponent.appendSibling(messageComponent);
        return textComponent;
    }

    public TwitchUser getThisUser(TMIClient client, String channel) {
        if (channel == null) {
            channel = !thisUsers.isEmpty() ? thisUsers.keySet().iterator().next() : "";
        }

        if (!thisUsers.containsKey(channel)) {
            thisUsers.put(channel, new TwitchUser(new IRCUser(client.getIRCConnection().getNick(), null, null)));
        }
        return thisUsers.get(channel);
    }

    public TwitchUser getUser(String username) {
        return users.computeIfAbsent(username.toLowerCase(Locale.ENGLISH), k -> new TwitchUser(new IRCUser(username, null, null)));
    }

    private final float[] tmpHSB = new float[3];

    public int getAcceptableNameColor(int color) {
        int red = (color >> 16 & 255);
        int green = (color >> 8 & 255);
        int blue = (color & 255);
        Color.RGBtoHSB(red, green, blue, tmpHSB);
        float brightness = tmpHSB[2];
        if (brightness < 0.4f) {
            brightness = 0.4f;
        }
        return Color.HSBtoRGB(tmpHSB[0], tmpHSB[1], brightness);
    }
}
