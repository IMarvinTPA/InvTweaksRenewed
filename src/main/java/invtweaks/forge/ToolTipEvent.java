package invtweaks.forge;

import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import invtweaks.InvTweaksMod;
import invtweaks.config.InvTweaksConfig;
import invtweaks.tree.InvTweaksItemTree;
import invtweaks.util.Utils;
//import invtweaks.InvTweaksConfigManager;
import invtweaks.api.IItemTreeCategory;
import invtweaks.api.IItemTreeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.relauncher.Side;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(Dist.CLIENT)
public class ToolTipEvent {

	@OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void tTipEvent(ItemTooltipEvent event) {
        ItemStack current = event.getItemStack();
        if (current.isEmpty())
        {
            return;
        }
        try {
	        if(true) { //cfgManager.getConfig().getProperty(InvTweaksConfig.PROP_TOOLTIP_PATH).equals("true")) {        
	        	InvTweaksItemTree tree = InvTweaksConfig.getSelfCompiledTree();
        		if (tree == null) {
        			return;
        		}
        		String id = Utils.getItemRegistryString(current);
	        	int dmg = current.getDamageValue();
	        	CompoundTag tag = current.getTag();
	        	List<IItemTreeItem> items = tree.getItems(id, dmg, tag);
	            if (items.isEmpty())
	                return;
	
	            Set<String> paths = new HashSet<>();
	            IItemTreeCategory root = InvTweaksConfig.getSelfCompiledTree().getRootCategory();
	            int unsortedZone = InvTweaksConfig.getSelfCompiledTree().getLastTreeOrder();
	            int minOrder = Integer.MAX_VALUE;
	            for(IItemTreeItem item: items) {
	                String path = item.getPath();
	                String altPath = root.findKeywordPath(item.getName());
	                int itemOrder = item.getOrder();
	                minOrder = Integer.min(minOrder, itemOrder);
	                //event.getToolTip().add( + "");
	
	                //If we have reported a match in the tree, don't report the unsorted matches.
	                //This can happen if a damage id is filtered and not all of the items have been
	                //placed in the tree.
	                if (minOrder <= unsortedZone && itemOrder > unsortedZone)
	                    break;
	                
	                if (path.equals(altPath) || itemOrder > unsortedZone) {
	                    if (!paths.contains(path)) {
	                        event.getToolTip().add(Component.literal(path + " (" + item.getOrder() + ")").withStyle(ChatFormatting.DARK_GRAY));
	                        paths.add(path);
	                    }                    
	                } else {
	                    if (!paths.contains(path)) {
	                        event.getToolTip().add(Component.literal("T:" + path + " (" + item.getOrder() + ")").withStyle(ChatFormatting.DARK_GRAY));
	                        paths.add(path);
	                    }
	                    if (!paths.contains(altPath)) {
	                        event.getToolTip().add(Component.literal("M:" + altPath + " (" + item.getOrder() + ")").withStyle(ChatFormatting.DARK_GRAY));
	                        paths.add(altPath);
	                    }
	                }
	            }
	        }
        } catch(Exception ex) 
        {}
    }
}
