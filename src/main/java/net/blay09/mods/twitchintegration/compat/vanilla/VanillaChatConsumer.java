package net.blay09.mods.twitchintegration.compat.vanilla;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.TwitchSessionManager;
import net.blay09.mods.twitchintegration.api.ChatConsumer;
import net.blay09.mods.twitchintegration.chat.DeletedMessagesMode;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.handler.ChannelUser;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.util.text.*;
import net.minecraftforge.common.ForgeHooks;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

public class VanillaChatConsumer implements ChatConsumer {

    private final AtomicInteger idCounter = new AtomicInteger(9000);
    private final Multimap<String, VanillaChatMessage> userMessages = ArrayListMultimap.create();

    @Override
    public void onChatMessage(TwitchChannel channel, TwitchUser user, TwitchMessage message) {
        Style style = null;
        if (user.getColor() != null) {
            final Color color = Color.fromHex(user.getColor());
            style = Style.EMPTY.setColor(color);
        }

        TextComponent messageText = (TextComponent) ForgeHooks.newChatWithLinks(message.getMessage());
        boolean isMultiChannel = TwitchSessionManager.getJoinedChannels().size() > 1;
        if (isMultiChannel) {
            if (message.isAction()) {
                final ITextComponent channelNameText = Messages.styledString(channel.getName(), TextFormatting.YELLOW);
                final ITextComponent displayNameText = Messages.styledString(user.getDisplayName(), style);
                if (style != null) {
                    messageText.mergeStyle(style);
                }
                printChatMessage(Messages.lang("chat.multiChannel.action", channelNameText, displayNameText, messageText), user.getNick());
            } else {
                final ITextComponent channelText = Messages.styledString(channel.getName(), TextFormatting.YELLOW);
                final StringTextComponent displayNameText = Messages.styledString(user.getDisplayName(), style);
                printChatMessage(Messages.lang("chat.multiChannel.message", channelText, displayNameText, messageText), user.getNick());
            }
        } else {
            if (message.isAction()) {
                printChatMessage(Messages.styledLang("chat.action", style, user.getDisplayName(), messageText), user.getNick());
            } else {
                final StringTextComponent displayNameText = Messages.styledString(user.getDisplayName(), style);
                printChatMessage(Messages.lang("chat.message", displayNameText, messageText), user.getNick());
            }
        }
    }

    @Override
    public void onSystemMessage(TwitchChannel channel, ITextComponent message) {
        printChatMessage(message, null);
    }

    @Override
    public void onWhisperMessage(TwitchUser user, TwitchMessage message) {
        Style style = null;
        if (user.getColor() != null) {
            final Color color = Color.fromHex(user.getColor());
            style = Style.EMPTY.setColor(color);
        }

        ITextComponent messageText = ForgeHooks.newChatWithLinks(message.getMessage());
        if (message.isAction()) {
            printChatMessage(Messages.styledLang("chat.whisper.action", style, user.getDisplayName(), messageText), user.getNick());
        } else {
            final StringTextComponent displayName = Messages.styledString(user.getDisplayName(), style);
            printChatMessage(Messages.lang("chat.whisper.message", displayName, messageText), user.getNick());
        }
    }

    @Override
    public void clearChat(TwitchChannel channel) {
        for (VanillaChatMessage message : userMessages.values()) {
            Minecraft.getInstance().ingameGUI.getChatGUI().deleteChatLine(message.getId());
        }
    }

    @Override
    public void purgeUserMessages(TwitchChannel channel, ChannelUser user) {
        if (channel.getDeletedMessagesMode() != DeletedMessagesMode.Show) {
            final Collection<VanillaChatMessage> messages = userMessages.get(user.getUsername());
            for (VanillaChatMessage message : messages) {
                if (channel.getDeletedMessagesMode() == DeletedMessagesMode.Hide) {
                    Minecraft.getInstance().ingameGUI.getChatGUI().deleteChatLine(message.getId());
                } else if (channel.getDeletedMessagesMode() == DeletedMessagesMode.Strikethrough) {
                    final TextComponent textComponent = (TextComponent) message.getTextComponent();
                    final IFormattableTextComponent strikethroughText = textComponent.mergeStyle(TextFormatting.STRIKETHROUGH);
                    Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(strikethroughText, message.getId());
                } else if (channel.getDeletedMessagesMode() == DeletedMessagesMode.Replace) {
                    final TranslationTextComponent messageDeletedText = Messages.styledLang("chat.messageDeleted", TextFormatting.GRAY, user.getUsername());
                    Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(messageDeletedText, message.getId());
                }
            }
            messages.clear();
        }
    }

    private void printChatMessage(ITextComponent textComponent, @Nullable String senderNick) {
        int chatLineId = senderNick != null ? idCounter.incrementAndGet() : 0;
        Minecraft.getInstance().ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(textComponent, chatLineId);
        if (senderNick != null) {
            userMessages.put(senderNick, new VanillaChatMessage(chatLineId, textComponent));
        }
    }

}
