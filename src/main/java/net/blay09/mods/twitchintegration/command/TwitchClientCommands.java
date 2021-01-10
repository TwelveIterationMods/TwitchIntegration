package net.blay09.mods.twitchintegration.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.blay09.mods.twitchintegration.TwitchChatIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.command.CommandException;
import net.minecraft.command.CommandSource;
import net.minecraft.util.SharedConstants;
import net.minecraft.util.Util;
import net.minecraft.util.text.*;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientChatEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = TwitchChatIntegration.MOD_ID, value = Dist.CLIENT)
public class TwitchClientCommands {

    private static CommandDispatcher<CommandSource> dispatcher;

    @SubscribeEvent
    public static void registerCommands(RegisterCommandsEvent event) {
        dispatcher = new CommandDispatcher<>();
        TwitchCommand.register(dispatcher);
        TwitchCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onClientChat(ClientChatEvent event) {
        if (event.getMessage().startsWith("/twitch ")) {
            final String message = event.getMessage();
            event.setCanceled(true);

            Minecraft.getInstance().enqueue(() -> {
                CommandSource source = Minecraft.getInstance().player.getCommandSource();
                handleCommand(source, message);

                Minecraft.getInstance().ingameGUI.getChatGUI().addToSentMessages(message);
            });
        }
    }

    private static int handleCommand(CommandSource source, String command) {
        StringReader reader = new StringReader(command);
        if (reader.canRead() && reader.peek() == '/') {
            reader.skip();
        }

        try {
            return dispatcher.execute(reader, source);
        } catch (CommandException e) {
            source.sendErrorMessage(e.getComponent());
            return 0;
        } catch (CommandSyntaxException e) {
            source.sendErrorMessage(TextComponentUtils.toTextComponent(e.getRawMessage()));
            if (e.getInput() != null && e.getCursor() >= 0) {
                int pos = Math.min(e.getInput().length(), e.getCursor());
                IFormattableTextComponent textComponent = (new StringTextComponent("")).mergeStyle(TextFormatting.GRAY).modifyStyle((style) -> style.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command)));
                if (pos > 10) {
                    textComponent.appendString("...");
                }

                textComponent.appendString(e.getInput().substring(Math.max(0, pos - 10), pos));
                if (pos < e.getInput().length()) {
                    ITextComponent errorComponent = (new StringTextComponent(e.getInput().substring(pos))).mergeStyle(TextFormatting.RED, TextFormatting.UNDERLINE);
                    textComponent.append(errorComponent);
                }

                textComponent.append((new TranslationTextComponent("command.context.here")).mergeStyle(TextFormatting.RED, TextFormatting.ITALIC));
                source.sendErrorMessage(textComponent);
            }
        } catch (Exception e) {
            IFormattableTextComponent textComponent = new StringTextComponent(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            if (TwitchChatIntegration.logger.isDebugEnabled()) {
                TwitchChatIntegration.logger.error("Command exception: {}", command, e);
                StackTraceElement[] stackTrace = e.getStackTrace();
                for (int i = 0; i < Math.min(stackTrace.length, 3); ++i) {
                    textComponent.appendString("\n\n").appendString(stackTrace[i].getMethodName()).appendString("\n ").appendString(stackTrace[i].getFileName()).appendString(":").appendString(String.valueOf(stackTrace[i].getLineNumber()));
                }
            }

            source.sendErrorMessage((new TranslationTextComponent("command.failed")).modifyStyle((style) -> style.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, textComponent))));
            if (SharedConstants.developmentMode) {
                source.sendErrorMessage(new StringTextComponent(Util.getMessage(e)));
                TwitchChatIntegration.logger.error("'" + command + "' threw an exception", e);
            }

            return 0;
        }

        return 0;
    }

}
