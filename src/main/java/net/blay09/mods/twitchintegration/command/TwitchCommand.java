package net.blay09.mods.twitchintegration.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.blay09.javatmi.TMIClient;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.twitchintegration.TwitchSessionManager;
import net.blay09.mods.twitchintegration.chat.DeletedMessagesMode;
import net.blay09.mods.twitchintegration.chat.TwitchChannel;
import net.blay09.mods.twitchintegration.gui.screen.TwitchAuthenticationScreen;
import net.blay09.mods.twitchintegration.handler.TwitchChatHandler;
import net.blay09.mods.twitchintegration.util.Messages;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.server.command.EnumArgument;

import java.util.Collection;

public class TwitchCommand {

    private static final SuggestionProvider<CommandSource> whisperSuggestionProvider = (context, builder) -> {
        for (TwitchUser user : TwitchSessionManager.getChatHandler().getWhisperUsers()) {
            builder.suggest(user.getNick());
        }
        return builder.buildFuture();
    };

    private static final SuggestionProvider<CommandSource> channelSuggestionProvider = (context, builder) -> {
        for (TwitchChannel channel : TwitchSessionManager.getChannelManager().getChannels()) {
            builder.suggest(channel.getName());
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("twitch").requires(it -> it.getEntity() instanceof PlayerEntity && !(it.getEntity() instanceof FakePlayer))
                .then(Commands.literal("authenticate").executes(TwitchCommand::authenticateTwitch))
                .then(Commands.literal("channels").executes(TwitchCommand::listChannels))
                .then(Commands.literal("join")
                        .then(Commands.argument("channel", StringArgumentType.string()).suggests(channelSuggestionProvider).executes(TwitchCommand::joinChannel)))
                .then(Commands.literal("leave")
                        .then(Commands.argument("channel", StringArgumentType.string()).suggests(channelSuggestionProvider).executes(TwitchCommand::leaveChannel)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("channel", StringArgumentType.string()).suggests(channelSuggestionProvider).executes(TwitchCommand::removeChannel)))
                .then(Commands.literal("config")
                        .then(Commands.argument("channel", StringArgumentType.string()).suggests(channelSuggestionProvider)
                                .then(Commands.literal("subscribersOnly").executes(context -> printChannelConfig(context, "subscribersOnly"))
                                        .then(Commands.argument("subscribersOnly", BoolArgumentType.bool())
                                                .executes(context -> changeChannelConfig(context, "subscribersOnly"))))
                                .then(Commands.literal("deletedMessagesMode").executes(context -> printChannelConfig(context, "deletedMessagesMode"))
                                        .then(Commands.argument("mode", EnumArgument.enumArgument(DeletedMessagesMode.class))
                                                .executes(context -> changeChannelConfig(context, "deletedMessagesMode"))))))
                .then(Commands.literal("whisper")
                        .then(Commands.argument("user", StringArgumentType.string()).suggests(whisperSuggestionProvider)
                                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(TwitchCommand::sendWhisper))))
                .then(Commands.literal("send")
                        .then(Commands.argument("channel", StringArgumentType.string()).suggests(channelSuggestionProvider)
                                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(TwitchCommand::sendMessage)))));
    }

    private static int sendWhisper(CommandContext<CommandSource> context) {
        String user = StringArgumentType.getString(context, "user");
        String message = StringArgumentType.getString(context, "message");
        TMIClient twitchClient = TwitchSessionManager.getClient();
        if (twitchClient != null) {
            if (isReadOnlyChat(twitchClient)) {
                context.getSource().sendFeedback(Messages.styledLang("error.read_only_chat", TextFormatting.RED), false);
                return 0;
            }

            twitchClient.getTwitchCommands().whisper(user, message);

            final TwitchChatHandler twitchChatHandler = TwitchSessionManager.getChatHandler();
            twitchChatHandler.onWhisperMessage(twitchClient, twitchChatHandler.getOrCreateClientUser(twitchClient, null), twitchChatHandler.getWhisperUser(user), message);
        }
        return 1;
    }

    private static int sendMessage(CommandContext<CommandSource> context) {
        String channel = StringArgumentType.getString(context, "channel");
        String message = StringArgumentType.getString(context, "message");
        TMIClient twitchClient = TwitchSessionManager.getClient();
        if (twitchClient != null) {
            if (isReadOnlyChat(twitchClient)) {
                context.getSource().sendFeedback(Messages.styledLang("error.read_only_chat", TextFormatting.RED), false);
                return 0;
            }

            twitchClient.send("#" + channel, message);

            final TwitchChatHandler twitchChatHandler = TwitchSessionManager.getChatHandler();
            if (message.startsWith("/me ")) {
                message = message.substring(4);
                twitchChatHandler.onChatMessage(twitchClient, channel, twitchChatHandler.getOrCreateClientUser(twitchClient, channel), new TwitchMessage(message, -1, true, 0));
            } else {
                twitchChatHandler.onChatMessage(twitchClient, channel, twitchChatHandler.getOrCreateClientUser(twitchClient, channel), new TwitchMessage(message, -1, false, 0));
            }
        }
        return 1;
    }

    private static int joinChannel(CommandContext<CommandSource> context) {
        String channelName = StringArgumentType.getString(context, "channel");

        TwitchSessionManager.getChannelManager().joinChannel(channelName);

        final ITextComponent channelNameText = Messages.styledString(channelName, TextFormatting.YELLOW);
        context.getSource().sendFeedback(Messages.lang("commands.join", channelNameText), true);
        return 1;
    }

    private static int leaveChannel(CommandContext<CommandSource> context) {
        String channelName = StringArgumentType.getString(context, "channel");

        TwitchSessionManager.getChannelManager().leaveChannel(channelName);

        final ITextComponent channelNameText = Messages.styledString(channelName, TextFormatting.YELLOW);
        context.getSource().sendFeedback(Messages.lang("commands.leave", channelNameText), true);
        return 1;
    }

    private static int removeChannel(CommandContext<CommandSource> context) {
        String channelName = StringArgumentType.getString(context, "channel");

        TwitchSessionManager.getChannelManager().removeChannelByName(channelName);

        final ITextComponent channelNameText = Messages.styledString(channelName, TextFormatting.YELLOW);
        context.getSource().sendFeedback(Messages.lang("commands.remove", channelNameText), true);
        return 1;
    }

    private static int listChannels(CommandContext<CommandSource> context) {
        final Collection<TwitchChannel> channels = TwitchSessionManager.getChannelManager().getChannels();
        if (channels.isEmpty()) {
            throw new CommandException(Messages.lang("commands.channels.noChannels"));
        }

        StringBuilder builder = new StringBuilder();
        for (TwitchChannel channel : channels) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(Messages.format("commands.channels.channelListItem", channel.getName(), channel.isEnabled()));
        }
        final ITextComponent channelListItemsText = Messages.styledString(builder.toString(), TextFormatting.WHITE);
        final ITextComponent channelListText = Messages.styledLang("commands.channels.channelList", TextFormatting.YELLOW, channelListItemsText);
        context.getSource().sendFeedback(channelListText, true);
        return 1;
    }

    private static int printChannelConfig(CommandContext<CommandSource> context, String option) {
        final String channelName = StringArgumentType.getString(context, "channel");
        final TwitchChannel channel = TwitchSessionManager.getChannelManager().getChannel(channelName);
        if (channel == null) {
            throw new CommandException(Messages.lang("commands.config.channelNotFound", channelName));
        }

        Object value = null;
        if (option.equals("subscribersOnly")) {
            value = channel.isSubscribersOnly();
        } else if (option.equals("deletedMessagesMode")) {
            value = channel.getDeletedMessagesMode();
        }
        final ITextComponent channelNameText = Messages.styledString(channelName, TextFormatting.YELLOW);
        final ITextComponent optionText = Messages.styledString(option, TextFormatting.GRAY);
        final ITextComponent valueText = Messages.styledString(String.valueOf(value), TextFormatting.GREEN);
        final ITextComponent configText = Messages.lang("commands.config.channelConfigValue", channelNameText, optionText, valueText);
        context.getSource().sendFeedback(configText, true);
        return 1;
    }

    private static int changeChannelConfig(CommandContext<CommandSource> context, String option) {
        final String channelName = StringArgumentType.getString(context, "channel");

        final TwitchChannel channel = TwitchSessionManager.getChannelManager().getChannel(channelName);
        if (channel == null) {
            throw new CommandException(Messages.lang("commands.config.channelNotFound", channelName));
        }

        Object value = null;
        if (option.equals("subscribersOnly")) {
            final boolean subscribersOnly = BoolArgumentType.getBool(context, "subscribersOnly");
            channel.setSubscribersOnly(subscribersOnly);
            value = subscribersOnly;
        } else if (option.equals("deletedMessagesMode")) {
            final DeletedMessagesMode mode = context.getArgument("mode", DeletedMessagesMode.class);
            channel.setDeletedMessagesMode(mode);
            value = mode;
        }

        TwitchSessionManager.getChannelManager().save();

        final ITextComponent channelNameText = Messages.styledString(channelName, TextFormatting.YELLOW);
        final ITextComponent optionText = Messages.styledString(option, TextFormatting.GRAY);
        final ITextComponent valueText = Messages.styledString(String.valueOf(value), TextFormatting.GREEN);
        final ITextComponent configText = Messages.lang("commands.config.channelConfigChanged", channelNameText, optionText, valueText);
        context.getSource().sendFeedback(configText, true);

        return 1;
    }

    private static int authenticateTwitch(CommandContext<CommandSource> context) {
        Minecraft.getInstance().displayGuiScreen(new TwitchAuthenticationScreen(null));
        return 1;
    }

    private static boolean isReadOnlyChat(TMIClient twitchClient) {
        return TwitchIntegrationConfig.CLIENT.useAnonymousLogin.get() || twitchClient.getIRCConnection().getNick().startsWith("justinfan");
    }
}
