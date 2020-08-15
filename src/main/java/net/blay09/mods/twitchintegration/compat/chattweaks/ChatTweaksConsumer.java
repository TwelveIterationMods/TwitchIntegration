//package net.blay09.mods.twitchintegration.handler;
//
//import net.blay09.javatmi.TwitchUser;
//import net.blay09.mods.chattweaks.ChatTweaks;
//import net.blay09.mods.chattweaks.api.ChatChannel;
//import net.blay09.mods.chattweaks.core.ChatManager;
//import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
//import net.blay09.mods.twitchintegration.chat.TwitchChannel;
//import net.minecraft.util.text.ITextComponent;
//import net.minecraft.util.text.StringTextComponent;
//import net.minecraft.util.text.TextComponent;
//import net.minecraft.util.text.TextFormatting;
//import net.minecraftforge.common.ForgeHooks;
//
//import javax.annotation.Nullable;
//import java.util.Locale;
//
//public class ChatTweaksConsumer implements ChatConsumer {
//
//    @Override
//    public void addChatMessage(TwitchChannel channel) {
//        ChatChannel targetChannel = channel != null ? channel.getChatChannel() : null;
//
//        // Format Message
//        ITextComponent senderComponent = formatSenderComponent(user, tmpBadges);
//        ITextComponent messageComponent = formatMessageComponent(message, twitchMessage.isAction());
//        ITextComponent textComponent = formatComponent(senderComponent, messageComponent, twitchMessage.isAction());
//        ChatMessage chatMessage = ChatTweaks.createChatMessage(textComponent);
//        chatMessage.setSender(senderComponent);
//        chatMessage.setMessage(messageComponent);
//        chatMessage.setOutputVar("c", formatChannelComponent(channel));
//        chatMessage.withRGB(twitchMessage.isAction() ? 2 : 1);
//        for (ChatImage chatImage : tmpBadges) {
//            chatMessage.addImage(chatImage);
//        }
//        for (ChatImage chatImage : tmpEmotes) {
//            chatMessage.addImage(chatImage);
//        }
//        if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
//            int nameColor = ChatTweaks.colorFromHex(user.getColor());
//            chatMessage.setRGBColor(0, getAcceptableNameColor(nameColor));
//        } else {
//            chatMessage.setRGBColor(0, 0x808080);
//        }
//        if (twitchMessage.isAction()) {
//            if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
//                int nameColor = ChatTweaks.colorFromHex(user.getColor());
//                chatMessage.setRGBColor(1, getAcceptableNameColor(nameColor));
//            } else {
//                chatMessage.setRGBColor(1, 0x808080);
//            }
//        }
//        ChatTweaks.addChatMessage(chatMessage, targetChannel);
//    }
//
//    public void addSystemMessage(TwitchChannel channel, ITextComponent message) {
//        // TODO
//    }
//
//    @Override
//    public void addWhisperMessage() {
//        ChatChannel whisperChannel = ChatManager.getTemporaryChannel("(" + (isSelf ? receiver.getDisplayName() : user.getDisplayName()) + ")");
//
//        // Format Message
//        boolean isAction = transformedMessage.startsWith("/me ") && transformedMessage.length() > 4;
//        if (isAction) {
//            transformedMessage = transformedMessage.substring(4);
//        }
//        ITextComponent senderComponent = formatSenderComponent(user, tmpBadges);
//        ITextComponent messageComponent = formatMessageComponent(transformedMessage, isAction);
//        ITextComponent textComponent = formatComponent(senderComponent, messageComponent, isAction);
//        ChatMessage chatMessage = ChatTweaks.createChatMessage(textComponent);
//        chatMessage.setSender(senderComponent);
//        chatMessage.setMessage(messageComponent);
//        chatMessage.setOutputVar("r", formatSenderComponent(receiver, null));
//        chatMessage.withRGB(isAction ? 2 : 1);
//        for (ChatImage chatImage : tmpEmotes) {
//            chatMessage.addImage(chatImage);
//        }
//        if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
//            int nameColor = ChatTweaks.colorFromHex(user.getColor());
//            chatMessage.setRGBColor(0, getAcceptableNameColor(nameColor));
//        } else {
//            chatMessage.setRGBColor(0, 0x808080);
//        }
//        if (isAction) {
//            if (user.hasColor() && !TwitchIntegrationConfig.disableUserColors) {
//                int nameColor = ChatTweaks.colorFromHex(user.getColor());
//                chatMessage.setRGBColor(1, getAcceptableNameColor(nameColor));
//            } else {
//                chatMessage.setRGBColor(1, 0x808080);
//            }
//        }
//
//        ChatView whisperView = ChatViewManager.getChatView(whisperChannel.getName());
//        if (whisperView == null) {
//            whisperView = new ChatView(whisperChannel.getName());
//            whisperView.addChannel(whisperChannel);
//            whisperView.setOutgoingPrefix("/twitch " + (isSelf ? receiver.getNick().toLowerCase(Locale.ENGLISH) : user.getNick().toLowerCase(Locale.ENGLISH)) + " ");
//            whisperView.setTemporary(true);
//            ChatViewManager.addChatView(whisperView);
//        }
//
//
//        ChatTweaks.addChatMessage(chatMessage, whisperChannel);
//    }
//
//    @Override
//    public void clearChat(TwitchChannel twitchChannel) {
//        for (ChatMessage message : messages.values()) {
//            ChatManager.removeChatLine(message.getId());
//        }
//        ChatTweaks.refreshChat();
//    }
//
//    @Nullable
//    public static ITextComponent formatSenderComponent(@Nullable TwitchUser user, @Nullable List<ChatImage> nameBadges) {
//        if (user == null) {
//            return null;
//        }
//        StringBuilder sb = new StringBuilder();
//        if (nameBadges != null) {
//            for (ChatImage chatImage : nameBadges) {
//                sb.append(ChatTweaks.TEXT_FORMATTING_EMOTE);
//                for (int i = 0; i < chatImage.getSpaces(); i++) {
//                    sb.append(' ');
//                }
//            }
//        }
//        return new StringTextComponent(sb.toString() + ChatTweaks.TEXT_FORMATTING_RGB + user.getDisplayName() + ChatTweaks.TEXT_FORMATTING_RGB);
//    }
//
//    public static ITextComponent formatMessageComponent(String message, boolean isAction) {
//        message = TextFormatting.getTextWithoutFormattingCodes(message);
//        assert message != null;
//        return ForgeHooks.newChatWithLinks(message);
//    }
//
//    @Nullable
//    public static ITextComponent formatChannelComponent(@Nullable String channel) {
//        return channel != null ? new StringTextComponent(channel) : null;
//    }
//
//    public static ITextComponent formatComponent(ITextComponent senderComponent, ITextComponent messageComponent, boolean isAction) {
//        TextComponent textComponent = new StringTextComponent("");
//        textComponent.append(senderComponent);
//        textComponent.appendString(isAction ? " " : ": ");
//        textComponent.append(messageComponent);
//        return textComponent;
//    }
//
//    @Override
//    public void purgeUserMessages(TwitchChannel twitchChannel, ChannelUser user) {
//        switch (twitchChannel.getDeletedMessages()) {
//            case Hide:
//                for (ChatMessage message : messages.get(user)) {
//                    ChatManager.removeChatLine(message.getId());
//                }
//                ChatTweaks.refreshChat();
//                break;
//            case Strikethrough:
//                for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
//                    message.getTextComponent().getStyle().setStrikethrough(true);
//                }
//                ChatTweaks.refreshChat();
//                break;
//            case Replace:
//                for (ChatMessage message : messages.get(new ChannelUser(channel, username))) {
//                    ITextComponent removedMessageComponent = new TextComponentString("<message deleted>");
//                    removedMessageComponent.getStyle().setColor(TextFormatting.GRAY);
//                    message.setMessage(removedMessageComponent);
//                    message.setTextComponent(formatComponent(message.getSender(), removedMessageComponent, false));
//                    message.clearImages();
//                }
//                ChatTweaks.refreshChat();
//                break;
//        }
//
//    }
//}
