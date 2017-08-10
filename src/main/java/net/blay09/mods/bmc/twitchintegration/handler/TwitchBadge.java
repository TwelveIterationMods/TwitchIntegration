package net.blay09.mods.bmc.twitchintegration.handler;

import com.google.common.collect.Maps;
import com.google.gson.JsonObject;
import net.blay09.mods.bmc.twitchintegration.TwitchIntegration;
import net.blay09.mods.chattweaks.balyware.CachedAPI;
import net.blay09.mods.chattweaks.image.ITooltipProvider;
import net.blay09.mods.chattweaks.image.renderable.IChatRenderable;
import net.blay09.mods.chattweaks.image.renderable.ImageLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IResource;
import net.minecraft.util.ResourceLocation;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

public class TwitchBadge {

	private static final Map<String, TwitchBadge> twitchBadges = Maps.newHashMap();

	private final IChatRenderable chatRenderable;
	private final ITooltipProvider tooltipProvider;

	public TwitchBadge(IChatRenderable chatRenderable, ITooltipProvider tooltipProvider) {
		this.chatRenderable = chatRenderable;
		this.tooltipProvider = tooltipProvider;
	}

	public IChatRenderable getChatRenderable() {
		return chatRenderable;
	}

	public ITooltipProvider getTooltipProvider() {
		return tooltipProvider;
	}

	@Nullable
	public static TwitchBadge getSubscriberBadge(TwitchChannel channel, int subMonths) {
		TwitchBadge badge = twitchBadges.get(channel.getName() + "_" + subMonths);
		if(badge == null && channel.getId() != -1) {
			JsonObject object = CachedAPI.loadCachedAPI("https://badges.twitch.tv/v1/badges/channels/" + channel.getId() + "/display", "badges_" + channel.getName(), null); // TODO use https://dev.twitch.tv/docs/v5/reference/chat/#get-chat-badges-by-channel
			JsonObject badges = object.getAsJsonObject("badge_sets");
			if(badges.has("subscriber")) {
				JsonObject jsonImage = badges.getAsJsonObject("subscriber").getAsJsonObject("versions").getAsJsonObject(String.valueOf(subMonths));
				try {
					IChatRenderable chatRenderable = ImageLoader.loadImage(new URI(jsonImage.get("image_url_1x").getAsString()), new File(Minecraft.getMinecraft().mcDataDir, "bmc/cache/badge_" + channel.getName() + "_" + subMonths));
					chatRenderable.setScale(0.45f);
					badge = new TwitchBadge(chatRenderable, ITooltipProvider.EMPTY);
				} catch (IOException | URISyntaxException e) {
					e.printStackTrace();
				}
				twitchBadges.put(channel.getName() + "_" + subMonths, badge);
			}
		}
		return badge;
	}

	public static void loadInbuiltBadge(String name) {
		try {
			IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(new ResourceLocation(TwitchIntegration.MOD_ID, "badges/badge_" + name + ".png"));
			IChatRenderable chatRenderable = ImageLoader.loadImage(resource.getInputStream(), null);
			chatRenderable.setScale(0.45f);
			twitchBadges.put(name, new TwitchBadge(chatRenderable, ITooltipProvider.EMPTY));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Nullable
	public static TwitchBadge getBadge(String name) {
		return twitchBadges.get(name);
	}
}
