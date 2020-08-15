package net.blay09.mods.twitchintegration.handler;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import net.blay09.javairc.IRCUser;
import net.blay09.javatmi.TMIAdapter;
import net.blay09.javatmi.TMIClient;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.TwitchChannelManager;
import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.twitchintegration.TwitchManager;
import net.blay09.mods.twitchintegration.api.ChatConsumer;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.compat.vanilla.VanillaChatConsumer;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

import javax.annotation.Nullable;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class TwitchChatHandler extends TMIAdapter {

    /**
     * TODO What is this used for?
     */
    private static final Pattern PATTERN_FORMAT = Pattern.compile("(?<=%[ucmr])|(?=%[ucmr])");

    /*private final Comparator<PositionedEmote> emoteComparator = Comparator.comparingInt(PositionedEmote::getStart);
    private final Predicate<IEmote> noTwitchEmotes = input -> !(input.getSource() instanceof TwitchGlobalEmoteSource || input.getSource() instanceof TwitchChannelEmoteSource);
    private final IEmoteScanner emoteScanner = new EmoteScanner();*/

    private final ChatConsumer chatConsumer = new VanillaChatConsumer();

    private final Multimap<ChannelUser, TwitchMessage> messages = ArrayListMultimap.create();
    private final Map<String, TwitchUser> usersByUsername = Maps.newHashMap();
    private Map<String, TwitchUser> usersByChannel = Maps.newHashMap();

    /*private final List<ChatImage> tmpBadges = new ArrayList<>();
    private final List<ChatImage> tmpEmotes = new ArrayList<>();
    private final List<String> tmpBadgeNames = Lists.newArrayList();*/

    private void storeUserState(@Nullable String channel, TwitchUser user) {
        usersByUsername.put(user.getNick(), user);
        if (channel != null) {
            usersByChannel.put(channel, user);
        }
    }

    @Override
    public void onUserState(TMIClient client, String channel, TwitchUser user) {
        storeUserState(channel, user);
    }

    @Override
    public void onChatMessage(TMIClient client, String channel, TwitchUser user, TwitchMessage message) {
        onTwitchChat(client, channel, user, message);
    }

    private void onTwitchChat(final TMIClient client, final String channel, final TwitchUser user, final TwitchMessage twitchMessage) {
        Minecraft.getInstance().enqueue(() -> {
            TwitchChannel twitchChannel = TwitchChannelManager.getChannel(channel);

            // If subscriber-only chat is enabled client-side, ignore messages from non-subscribers
            if (twitchChannel != null && twitchChannel.isSubscribersOnly() && !user.isSubscriber() && !user.isMod()) {
                return;
            }

            // Hide messages by users on the configured blacklist
            if (isBlacklisted(user)) {
                return;
            }

            // Fetch the channel id from the message if it's not known yet
            if (twitchChannel != null && twitchChannel.getId() == -1 && twitchMessage.getChannelId() != -1) {
                twitchChannel.setId(twitchMessage.getChannelId());
            }

            boolean isSelf = user.getNick().equals(client.getIRCConnection().getNick());

            // Apply Twitch Emotes
            /* TODO Emotes
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
            */

            // Apply Emotes
            /* TODO emotes int index = 0;
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
            String message = sb.toString();*/
            String message = twitchMessage.getMessage();

            // Apply Name Badges
            /* TODO Badges
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
                user.setBadges(tmpBadgeNames.toArray(new String[0]));
            }
            if (twitchChannel != null && user.hasBadges() && !TwitchIntegrationConfig.CLIENT.disableNameBadges.get()) {
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
             */

            chatConsumer.onChatMessage(twitchChannel, user, twitchMessage);

            messages.put(new ChannelUser(channel, user.getNick().toLowerCase(Locale.ENGLISH)), twitchMessage);
            storeUserState(channel, user);
        });
    }

    private boolean isBlacklisted(TwitchUser user) {
        return TwitchIntegrationConfig.CLIENT.userBlacklist.get().stream().anyMatch(it -> user.getNick().equalsIgnoreCase(it));
    }

    @Override
    public void onSubscribe(TMIClient client, final String channel, final String username, final boolean prime) {
        Minecraft.getInstance().enqueue(() -> {
            TwitchChannel twitchChannel = TwitchChannelManager.getChannel(channel);
            final TranslationTextComponent subscribeText = Messages.styledLang((prime ? "chat.subscribePrime" : "chat.subscribe"), TextFormatting.YELLOW, username);
            chatConsumer.onSystemMessage(twitchChannel, subscribeText);
        });
    }

    @Override
    public void onResubscribe(TMIClient client, final String channel, final TwitchUser user, final int months, String message) {
        Minecraft.getInstance().enqueue(() -> {
            TwitchChannel twitchChannel = TwitchChannelManager.getChannel(channel);
            final TranslationTextComponent resubscribeText = Messages.styledLang("chat.resubscribe", TextFormatting.YELLOW, user.getDisplayName(), months);
            chatConsumer.onSystemMessage(twitchChannel, resubscribeText);
        });
        if (message != null) {
            onTwitchChat(client, channel, user, new TwitchMessage(message, -1, false, 0));
        }
    }

    @Override
    public void onWhisperMessage(TMIClient client, TwitchUser user, String message) {
        onWhisperMessage(client, user, getOrCreateClientUser(client, null), message);
    }

    public void onWhisperMessage(final TMIClient client, final TwitchUser user, final TwitchUser receiver, final String message) {
        Minecraft.getInstance().enqueue(() -> {
            if (TwitchIntegrationConfig.CLIENT.showWhispers.get()) {
                boolean isSelf = user.getNick().equals(client.getIRCConnection().getNick());

                /* TODO emotes
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
                String transformedMessage = sb.toString();*/

                storeUserState(null, user);

                chatConsumer.onWhisperMessage(user, new TwitchMessage(message, -1, false, 0));
            }
        });
    }

    @Override
    public void onTimeout(TMIClient client, final String channel, final String username) {
        Minecraft.getInstance().enqueue(() -> {
            TwitchChannel twitchChannel = TwitchChannelManager.getChannel(channel);
            if (twitchChannel != null) {
                chatConsumer.purgeUserMessages(twitchChannel, new ChannelUser(channel, username));
            }
        });
    }

    @Override
    public void onClearChat(TMIClient client, final String channel) {
        Minecraft.getInstance().enqueue(() -> {
            final TwitchChannel twitchChannel = TwitchChannelManager.getChannel(channel);
            chatConsumer.clearChat(twitchChannel);
        });
    }

    @Override
    public void onUnhandledException(TMIClient client, final Exception e) {
        TwitchChatIntegration.logger.error("Unhandled exception: ", e);
        Minecraft.getInstance().enqueue(() -> {
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.sendStatusMessage(new StringTextComponent(TextFormatting.RED + "Twitch Integration encountered an unhandled exception. The connection has been terminated. Please review your log files and let the mod developer know."), false);
            }
        });
        TwitchManager.disconnect();
    }

    public TwitchUser getOrCreateClientUser(TMIClient client, String channel) {
        if (channel == null) {
            channel = !usersByChannel.isEmpty() ? usersByChannel.keySet().iterator().next() : "";
        }

        if (!usersByChannel.containsKey(channel)) {
            storeUserState(channel, new TwitchUser(new IRCUser(client.getIRCConnection().getNick(), null, null)));
        }

        return usersByChannel.get(channel);
    }

    public TwitchUser getUser(String username) {
        return usersByUsername.computeIfAbsent(username.toLowerCase(Locale.ENGLISH), k -> new TwitchUser(new IRCUser(username, null, null)));
    }

}
