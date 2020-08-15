package net.blay09.mods.twitchintegration.compat.chattweaks;

public class ChatTweaksAddon {

    /*@SubscribeEvent
    public void onTwitchChannelRemoved(TwitchChannelRemovedEvent event) {
        final ChatChannel chatChannel = event.getTwitchChannel().getChatChannel();
        if (chatChannel != null) {
            ChatManager.removeChatChannel(chatChannel.getName());
        }
    }

    public void createOrUpdateChatChannel() {
        if (chatChannel != null && !chatChannel.getName().equalsIgnoreCase(name)) {
            ChatManager.removeChatChannel(chatChannel.getName());
        }
        chatChannel = ChatManager.getChatChannel(name);
        if (chatChannel == null) {
            chatChannel = new ChatChannelImpl(name, "Twitch Chat for '" + name + "'", new ResourceLocation(TwitchChatIntegration.MOD_ID, "icon.png"), null);
            ChatManager.addChatChannel(chatChannel);
        }
    }

    public void createDefaultView() {
        if (chatChannel != null) {
            ChatView twitchView = new ChatViewImpl(name);
            twitchView.addChannel(chatChannel.getName());
            twitchView.setOutgoingPrefix("/twitch send " + name.toLowerCase(Locale.ENGLISH) + " ");
            ChatViewManager.registerView(twitchView);
        }
    }


            channel.createOrUpdateChatChannel();
            channel.createDefaultView();

    */

}
