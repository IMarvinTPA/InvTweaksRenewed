package invtweaks.network;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import invtweaks.InvTweaksConst;
import invtweaks.InvTweaksMod;
import invtweaks.api.IItemTreeItem;
import invtweaks.config.InvTweaksConfig;
import invtweaks.config.Ruleset;
import invtweaks.tree.InvTweaksItemTree;
import invtweaks.tree.InvTweaksItemTreeCategory;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class PacketUpdateConfig {
    private final List<UnmodifiableConfig> cats;
    private final List<String> rules;
    private final List<UnmodifiableConfig> contOverrides;
    private final boolean autoRefill;
    private final InvTweaksItemTree tree;
    private boolean firstPage;
    private String lastItemId;

    @SuppressWarnings("unused")
    public PacketUpdateConfig() {
        this(Collections.emptyList(), Collections.emptyList(), Collections.emptyList(), false, new InvTweaksItemTree(), "");
    }

    public PacketUpdateConfig(
            List<UnmodifiableConfig> cats,
            List<String> rules,
            List<UnmodifiableConfig> contOverrides,
            boolean autoRefill,
            InvTweaksItemTree tree,
            String lastItemId) {
        this.firstPage = true;
        this.cats = cats;
        this.rules = rules;
        this.autoRefill = autoRefill;
        this.contOverrides = contOverrides;
        this.tree = tree;
        this.lastItemId = lastItemId;
    }

    public PacketUpdateConfig(FriendlyByteBuf buf) {
        //InvTweaksMod.LOGGER.info("Recieved update packet with buffer of size: " + buf.readableBytes());
        lastItemId = "";
        int page = buf.readVarInt();
        if (page == 0) {
            this.cats = new ArrayList<>();
            int catsSize = buf.readVarInt();
            for (int i = 0; i < catsSize; ++i) {
                CommentedConfig subCfg = CommentedConfig.inMemory();
                subCfg.set("name", buf.readUtf(32767));
                List<String> spec = new ArrayList<>();
                int specSize = buf.readVarInt();
                for (int j = 0; j < specSize; ++j) {
                    spec.add(buf.readUtf(32767));
                }
                subCfg.set("spec", spec);
                cats.add(subCfg);
            }        
            this.rules = new ArrayList<>();
            int rulesSize = buf.readVarInt();
            for (int i = 0; i < rulesSize; ++i) {
                rules.add(buf.readUtf(32767));
            }
            this.contOverrides = new ArrayList<>();
            int contOverridesSize = buf.readVarInt();
            for (int i = 0; i < contOverridesSize; ++i) {
                CommentedConfig contOverride = CommentedConfig.inMemory();
                contOverride.set("containerClass", buf.readUtf(32767));
                contOverride.set("x", buf.readInt());
                contOverride.set("y", buf.readInt());
                contOverride.set("sortRange", buf.readUtf(32767));
                contOverrides.add(contOverride);
            }
            this.autoRefill = buf.readBoolean();
            this.tree = new InvTweaksItemTree();
            InvTweaksItemTreeCategory root = new InvTweaksItemTreeCategory("stuff");
            //Categories don't matter for sorting.  Might matter if the tree takes over the Categories role.
            tree.setRootCategory(root);            
        } else {
            //More pages.
            this.firstPage = false;
            this.cats = new ArrayList<>();
            this.rules = new ArrayList<>();
            this.contOverrides = new ArrayList<>();
            this.autoRefill = false;
            this.tree = new InvTweaksItemTree();
            InvTweaksItemTreeCategory root = new InvTweaksItemTreeCategory("stuff");
            //Categories don't matter for sorting.  Might matter if the tree takes over the Categories role.
            tree.setRootCategory(root);
        }

        int treeSize = buf.readVarInt();
        for (int i = 0; i < treeSize; ++i) {
                String itemName = buf.readUtf(32767);
                String itemId = buf.readUtf(32767);
                String itemExtraBuffer = buf.readUtf(32767);
                CompoundTag extraData = null;
                int itemOrder =  buf.readVarInt();
                
                //InvTweaksMod.LOGGER.info("Reading: " + i + "," + itemName + "," + itemId + "," + itemExtraBuffer + "," + itemOrder + "," + buf.readableBytes());

                if (itemExtraBuffer.length() > 0) {
                    try {
                    extraData = TagParser.parseTag(itemExtraBuffer);
                    }
                    catch (CommandSyntaxException ex) {
                        extraData = null;
                        InvTweaksMod.LOGGER.error("Sort Item " + itemId + " has bad JSON: " + itemExtraBuffer);
                    }
                }
                if (itemId == "[ExpectMore]" || buf.readableBytes() == 0) {
                    lastItemId = itemId;
                    break;
                }
                tree.addItem("stuff", itemName, itemId, InvTweaksConst.DAMAGE_WILDCARD, extraData, itemOrder);
        }        
        
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get()
                .enqueueWork(
                        () -> {
                            if (firstPage) {
                                InvTweaksConfig.setPlayerCats(
                                        Objects.requireNonNull(ctx.get().getSender()),
                                        InvTweaksConfig.cfgToCompiledCats(cats));
                                InvTweaksConfig.setPlayerRules(
                                        Objects.requireNonNull(ctx.get().getSender()),
                                        new Ruleset(rules));
                                InvTweaksConfig.setPlayerAutoRefill(ctx.get().getSender(), autoRefill);
                                InvTweaksConfig.setPlayerContOverrides(
                                        Objects.requireNonNull(ctx.get().getSender()),
                                        InvTweaksConfig.cfgToCompiledContOverrides(contOverrides));
                                InvTweaksConfig.setPlayerTree(Objects.requireNonNull(ctx.get().getSender()),
                                        tree);
                                InvTweaksMod.LOGGER.info("Received config from client!");
                            } else {
                                InvTweaksItemTree existingTree = InvTweaksConfig.getPlayerTreeOnly(Objects.requireNonNull(ctx.get().getSender()));
                                if (existingTree != null) {
                                    for (IItemTreeItem item : tree.getAllItems()) {
                                        existingTree.addItem("stuff", item);
                                    }                          
                                }      
                                InvTweaksMod.LOGGER.info("Received more config from client!");
                            }                            
                        });
        ctx.get().setPacketHandled(true);
    }

    public void encode(FriendlyByteBuf buf) {
        if (lastItemId.isEmpty()){
            buf.writeVarInt(0);  //First page.
            buf.writeVarInt(cats.size());
            for (UnmodifiableConfig subCfg : cats) {
                buf.writeUtf(subCfg.getOrElse("name", ""));
                List<String> spec = subCfg.getOrElse("spec", Collections.emptyList());
                buf.writeVarInt(spec.size());
                for (String subSpec : spec) {
                    buf.writeUtf(subSpec);
                }
            }
            buf.writeVarInt(rules.size());
            for (String subRule : rules) {
                buf.writeUtf(subRule);
            }
            buf.writeVarInt(contOverrides.size());
            for (UnmodifiableConfig contOverride : contOverrides) {
                buf.writeUtf(contOverride.getOrElse("containerClass", ""));
                int x = contOverride.getIntOrElse("x", InvTweaksConfig.NO_POS_OVERRIDE);
                int y = contOverride.getIntOrElse("y", InvTweaksConfig.NO_POS_OVERRIDE);
                buf.writeInt(x).writeInt(y);
                buf.writeUtf(contOverride.getOrElse("sortRange", InvTweaksConfig.NO_SPEC_OVERRIDE));
            }
            buf.writeBoolean(autoRefill);
        } else {
            buf.writeVarInt(1);  //Subsequent page..
        }

        if (tree == null) {
            buf.writeVarInt(0);    
            return;
        }
        List<IItemTreeItem> allItems = tree.getAllItems();
        buf.writeVarInt(allItems.size());
        boolean export = lastItemId.isEmpty();

        for (IItemTreeItem item : allItems) {
            if (export) {
                encodeItem(buf, item.getName(), item.getId(), item.getExtraData(), item.getOrder());
            }
            else if (lastItemId.equals(item.getId())) {
                export = true;
            }
            if (buf.writerIndex() > 31000) {
                //InvTweaksMod.LOGGER.info("Ran out of room at: " + item.getId());
                encodeItem(buf, "[ExpectMore]", "[ExpectMore]", null, 0);
                InvTweaksConfig.setLastItemId(item.getId());
                export = false;
                break;
            }
        }
        if (export) {
            //We are done!
            InvTweaksConfig.setLastItemId("");
        }
        //InvTweaksMod.LOGGER.info("Created update packet with buffer of size: " + buf.writerIndex());
    }

    private void encodeItem(FriendlyByteBuf buf, String itemName, String itemId, CompoundTag extraData, int itemOrder) {
        buf.writeUtf(itemName);
        buf.writeUtf(itemId);
        if (extraData != null) {
            buf.writeUtf(extraData.toString()); //Converts to JSON.
        } else {
            buf.writeUtf(""); //Converts to JSON.
        }
        buf.writeVarInt(itemOrder);
        //InvTweaksMod.LOGGER.info("Writing: " + itemName + "," + itemId + ",...," + itemOrder + "," + buf.readableBytes());
    }
}
