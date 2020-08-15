package net.blay09.mods.twitchintegration.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.blay09.javatmi.TMIClient;
import net.blay09.javatmi.TwitchMessage;
import net.blay09.mods.twitchintegration.TwitchIntegrationConfig;
import net.blay09.mods.twitchintegration.TwitchManager;
import net.blay09.mods.twitchintegration.gui.screen.TwitchAuthenticationScreen;
import net.blay09.mods.twitchintegration.handler.TwitchChatHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandSource;
import net.minecraft.command.Commands;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;

public class TwitchCommand {

    public static void register(CommandDispatcher<CommandSource> dispatcher) {
        dispatcher.register(Commands.literal("twitch")
                .then(Commands.literal("authenticate").executes(TwitchCommand::authenticateTwitch))
                .then(Commands.literal("send")
                        .then(Commands.argument("channel", StringArgumentType.string()))
                        .then(Commands.argument("message", StringArgumentType.greedyString()))
                        .executes(TwitchCommand::sendMessage)));
    }

    private static int sendMessage(CommandContext<CommandSource> context) {
        String channel = StringArgumentType.getString(context, "channel");
        String message = StringArgumentType.getString(context, "message");
        TMIClient twitchClient = TwitchManager.getClient();
        final TwitchChatHandler twitchChatHandler = TwitchManager.getTwitchChatHandler();
        if (twitchClient != null) {
            if (TwitchIntegrationConfig.CLIENT.useAnonymousLogin.get() || twitchClient.getIRCConnection().getNick().startsWith("justinfan")) {
                ITextComponent readOnlyChatText = new TranslationTextComponent("twitchintegration.error.read_only_chat");
                readOnlyChatText.getStyle().setFormatting(TextFormatting.RED);
                context.getSource().sendFeedback(readOnlyChatText, false);
                return 0;
            }

            if (channel.startsWith("#")) {
                twitchClient.send(channel, message);
                if (message.startsWith("/me ")) {
                    message = message.substring(4);
                    twitchChatHandler.onChatMessage(twitchClient, channel, twitchChatHandler.getThisUser(twitchClient, channel), new TwitchMessage(message, -1, true, 0));
                } else {
                    twitchChatHandler.onChatMessage(twitchClient, channel, twitchChatHandler.getThisUser(twitchClient, channel), new TwitchMessage(message, -1, false, 0));
                }
            } else {
                twitchClient.getTwitchCommands().whisper(channel, message);
                twitchChatHandler.onWhisperMessage(twitchClient, twitchChatHandler.getThisUser(twitchClient, null), twitchChatHandler.getUser(channel), message);
            }
        }
        return 1;
    }

    private static int authenticateTwitch(CommandContext<CommandSource> context) {
        Minecraft.getInstance().displayGuiScreen(new TwitchAuthenticationScreen(null));
        return 1;
    }
}
