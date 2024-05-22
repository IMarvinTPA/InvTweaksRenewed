package invtweaks;

import invtweaks.config.InvTweaksConfig;
import invtweaks.network.NetworkDispatcher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.loading.FMLPaths;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.util.logging.Level;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(InvTweaksMod.MODID)
public class InvTweaksMod {
    public static final String MODID = "invtweaks";
    public static final Logger LOGGER = LogManager.getLogger(InvTweaksMod.MODID);

    @SuppressWarnings("java:S1118")
    public InvTweaksMod() {
        NetworkDispatcher.register();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, InvTweaksConfig.CLIENT_CONFIG);

        InvTweaksConfig.loadConfig(InvTweaksConfig.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve("invtweaks-client.toml"));
    }

    public static void addChatMessage(String message) {
    	Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) {
            return;
        }
        var component = Component.literal(message);
        player.sendSystemMessage(component);
    }

    private static String buildLogString(Level level, String message) {
        return InvTweaksConst.INGAME_LOG_PREFIX + ((level.equals(Level.SEVERE)) ? "[ERROR] " : "") + message;
    }
    
    public static void logInGame(String message) {
        logInGame(message, false);
    }
    
    public static void logInGame(String message, boolean alreadyTranslated) {
        String formattedMsg = buildLogString(Level.INFO,
                (alreadyTranslated) ? message : Component.translatable(message).getString());

        addChatMessage(formattedMsg);
        LOGGER.info(formattedMsg);
    }
}
