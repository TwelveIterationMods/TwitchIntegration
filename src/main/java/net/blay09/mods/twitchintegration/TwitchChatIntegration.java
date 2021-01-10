package net.blay09.mods.twitchintegration;

import net.blay09.mods.twitchintegration.auth.TwitchAuthManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TwitchChatIntegration.MOD_ID)
public class TwitchChatIntegration {

    // V1 Vanilla-only
    // V2 Chat Tweaks
    // ...

    public static final String MOD_ID = "twitchchatintegration";

    public static Logger logger = LogManager.getLogger(MOD_ID);

    public TwitchChatIntegration() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setupClient);

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, TwitchIntegrationConfig.clientSpec);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void setupClient(FMLClientSetupEvent event) {
        TwitchAuthManager.loadStoredToken();
        TwitchSessionManager.init();
    }

}
