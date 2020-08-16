package net.blay09.mods.twitchintegration.chat;

public enum DeletedMessagesMode {
    Show,
    Strikethrough,
    Replace,
    Hide;

    public static DeletedMessagesMode fromName(String name) {
        try {
            return valueOf(name);
        } catch (IllegalArgumentException e) {
            return Replace;
        }
    }
}
