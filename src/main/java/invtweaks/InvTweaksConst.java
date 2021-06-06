package invtweaks;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.loading.FMLPaths;

import org.apache.logging.log4j.Level;

import java.io.File;

public class InvTweaksConst {

    // Mod version
    public static final String MOD_VERSION = "@VERSION@";

    // Mod tree version
    // Change only when the tree evolves significantly enough to need to override all configs
    public static final String TREE_VERSION = "1.16";

    public static final String INVTWEAKS_CHANNEL = "InventoryTweaks";

    // File constants
    public static final File MINECRAFT_DIR = FMLPaths.GAMEDIR.get().toFile();;
    public static final File MINECRAFT_CONFIG_DIR = FMLPaths.CONFIGDIR.get().toFile();
    public static final File INVTWEAKS_CONFIG_DIR = new File(MINECRAFT_CONFIG_DIR, "InvTweaks/");
    public static final File INVTWEAKS_TREES_DIR = new File(INVTWEAKS_CONFIG_DIR, "trees/");
    public static final File CONFIG_PROPS_FILE = new File(INVTWEAKS_CONFIG_DIR, "InvTweaks.cfg");
    public static final File CONFIG_RULES_FILE = new File(INVTWEAKS_CONFIG_DIR, "InvTweaksRules.txt");
    public static final File CONFIG_TREE_FILE = new File(INVTWEAKS_CONFIG_DIR, "InvTweaksTree.txt");
    public static final File OLD_CONFIG_TREE_FILE = new File(MINECRAFT_CONFIG_DIR, "InvTweaksTree.txt");
    public static final File OLDER_CONFIG_TREE_FILE = new File(MINECRAFT_CONFIG_DIR, "InvTweaksTree.xml");
    public static final File OLDER_CONFIG_RULES_FILE = new File(MINECRAFT_DIR, "InvTweaksRules.txt");
    
    public static final File TEMP_DIR = new File(MINECRAFT_CONFIG_DIR, "tmp");
    public static final File MERGED_TREE_FILE = new File(TEMP_DIR, "InvTweaksTree.txt");
    public static final File MERGED_TREE_FILE_ALT = new File(INVTWEAKS_TREES_DIR, "InvTweaksTree.txt");

    public static final String INVTWEAKS_RESOURCE_DOMAIN = "inventorytweaks";
    public static final ResourceLocation DEFAULT_CONFIG_FILE = new ResourceLocation(INVTWEAKS_RESOURCE_DOMAIN,
            "defaultconfig.dat");
    public static final ResourceLocation DEFAULT_CONFIG_TREE_FILE = new ResourceLocation(INVTWEAKS_RESOURCE_DOMAIN,
            "itemtree.xml");

    public static final String HELP_URL = "http://inventory-tweaks.readthedocs.org";
    public static final String TREE_URL = "https://github.com/IMarvinTPA/InventoryTweaksTrees";

    // Global mod constants
    public static final String INGAME_LOG_PREFIX = "InvTweaks: ";
    public static final Level DEBUG = Level.INFO;
    public static final int JIMEOWAN_ID = 54696386; // Used in GUIs
    
    //To make the code compile, but unused otherwise.
    public static final int DAMAGE_WILDCARD = -1;

}
