package net.blay09.mods.bmc.twitchintegration.handler;

import net.blay09.mods.chattweaks.image.ITooltipProvider;
import net.blay09.mods.chattweaks.image.renderable.IChatRenderable;

public class TwitchBadge {

	private final IChatRenderable chatRenderable;
	private final ITooltipProvider tooltipProvider;

	public TwitchBadge(IChatRenderable chatRenderable, ITooltipProvider tooltipProvider) {
		this.chatRenderable = chatRenderable;
		chatRenderable.setScale(0.5f);
		this.tooltipProvider = tooltipProvider;
	}

	public IChatRenderable getChatRenderable() {
		return chatRenderable;
	}

	public ITooltipProvider getTooltipProvider() {
		return tooltipProvider;
	}

}
