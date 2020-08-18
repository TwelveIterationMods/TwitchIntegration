package net.blay09.mods.twitchintegration.chat;

public class TwitchBadge {

	/*private final IChatRenderable chatRenderable;
	private final ITooltipProvider tooltipProvider;

	public TwitchBadge(IChatRenderable chatRenderable, ITooltipProvider tooltipProvider) {
		this.chatRenderable = chatRenderable;
		chatRenderable.setScale(0.5f);
		this.tooltipProvider = tooltipProvider;
	}

	public IChatRenderable getChatRenderable() {
		return chatRenderable;
	}

public void loadChannelBadges() {
        if (badges == null) {
            new Thread(() -> {
                if (id == -1) {
                    id = TwitchAPI.loadChannelId(name);
                    if (id == -1) {
                        return;
                    }
                }
                Map<String, TwitchBadge> badges = TwitchAPI.getDefaultBadges();
                badges.putAll(TwitchAPI.loadChannelSpecificBadges(this));

                this.badges = badges;
            }).start();
        }
    }

    @Nullable
    public TwitchBadge getBadge(String key, int version) {
        if (badges == null) {
            return null;
        }
        TwitchBadge badge = badges.get(key + "/" + version);
        if (badge == null) {
            badge = badges.get(key);
        }
        return badge;
    }

	public ITooltipProvider getTooltipProvider() {
		return tooltipProvider;

	}*/

}
