package net.blay09.mods.twitchintegration.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.blay09.javatmi.TMIClient;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.javatmi.TwitchUser;
import net.blay09.mods.twitchintegration.TwitchChannelManager;
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

public class TwitchCommand {

    private static final SuggestionProvider<CommandSource> whisperSuggestionProvider = (context, builder) -> {
        for (TwitchUser user : TwitchSessionManager.getTwitchChatHandler().getWhisperUsers()) {
            builder.suggest(user.getNick());
        }
        return builder.buildFuture();
    };

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("twitch").requires(it -> it.getEntity() instanceof PlayerEntity && !(it.getEntity() instanceof FakePlayer))
                .then(Commands.literal("authenticate").executes(TwitchCommand::authenticateTwitch))
                .then(Commands.literal("join")
                        .then(Commands.argument("channel", StringArgumentType.string()).executes(TwitchCommand::joinChannel)))
                .then(Commands.literal("leave")
                        .then(Commands.argument("channel", StringArgumentType.string()).executes(TwitchCommand::leaveChannel)))
                .then(Commands.literal("remove")
                        .then(Commands.argument("channel", StringArgumentType.string()).executes(TwitchCommand::removeChannel)))
                .then(Commands.literal("config")
                        .then(Commands.argument("channel", StringArgumentType.string()).executes(TwitchCommand::printChannelConfig)
                                .then(Commands.literal("subscribersOnly")
                                        .then(Commands.argument("subscribersOnly", BoolArgumentType.bool())
                                                .executes(TwitchCommand::changeSubscribersOnly)))
                                .then(Commands.literal("deletedMessagesMode")
                                        .then(Commands.argument("mode", EnumArgument.enumArgument(DeletedMessagesMode.class))
                                                .executes(TwitchCommand::changeDeletedMessagesMode)))))
                .then(Commands.literal("whisper")
                        .then(Commands.argument("user", StringArgumentType.string()).suggests(whisperSuggestionProvider)
                                .then(Commands.argument("message", StringArgumentType.greedyString()).executes(TwitchCommand::sendWhisper))))
                .then(Commands.literal("send")
                        .then(Commands.argument("channel", StringArgumentType.string())
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

            final TwitchChatHandler twitchChatHandler = TwitchSessionManager.getTwitchChatHandler();
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

            final TwitchChatHandler twitchChatHandler = TwitchSessionManager.getTwitchChatHandler();
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
        String channel = StringArgumentType.getString(context, "channel");

        TwitchChannelManager.joinChannel(channel);
        return 1;
    }

    private static int leaveChannel(CommandContext<CommandSource> context) {
        String channel = StringArgumentType.getString(context, "channel");

        TwitchChannelManager.leaveChannel(channel);
        return 1;
    }

    private static int removeChannel(CommandContext<CommandSource> context) {
        String channel = StringArgumentType.getString(context, "channel");

        TwitchChannelManager.removeChannelByName(channel);
        return 1;
    }

    private static int printChannelConfig(CommandContext<CommandSource> context) {
        final String channelName = StringArgumentType.getString(context, "channel");
        final TwitchChannel channel = TwitchChannelManager.getChannel(channelName);
        if (channel == null) {
            throw new CommandException(Messages.lang("commands.config.channelNotFound", channelName));
        }

        final ITextComponent configText = Messages.lang("commands.config.channelConfig", channelName, channel.isSubscribersOnly(), channel.getDeletedMessages());
        context.getSource().sendFeedback(configText, true);
        return 1;
    }

    private static int changeSubscribersOnly(CommandContext<CommandSource> context) {
        final String channelName = StringArgumentType.getString(context, "channel");
        final boolean subscribersOnly = BoolArgumentType.getBool(context, "subscribersOnly");
        final TwitchChannel channel = TwitchChannelManager.getChannel(channelName);
        if (channel == null) {
            throw new CommandException(Messages.lang("commands.config.channelNotFound", channelName));
        }

        channel.setSubscribersOnly(subscribersOnly);
        final ITextComponent configText = Messages.lang("commands.config.channelConfigChanged", channelName, "subscribersOnly", subscribersOnly);
        context.getSource().sendFeedback(configText, true);

        return 1;
    }

    private static int changeDeletedMessagesMode(CommandContext<CommandSource> context) {
        final String channelName = StringArgumentType.getString(context, "channel");
        final DeletedMessagesMode mode = context.getArgument("mode", DeletedMessagesMode.class);
        final TwitchChannel channel = TwitchChannelManager.getChannel(channelName);
        if (channel == null) {
            throw new CommandException(Messages.lang("commands.config.channelNotFound", channelName));
        }

        channel.setDeletedMessagesMode(mode);
        final ITextComponent configText = Messages.lang("commands.config.channelConfigChanged", channelName, "deletedMessagesMode", mode);
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
