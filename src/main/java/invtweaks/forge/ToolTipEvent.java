package invtweaks.forge;

import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import invtweaks.InvTweaksMod;
import invtweaks.config.InvTweaksConfig;
import invtweaks.tree.InvTweaksItemTree;
//import invtweaks.InvTweaksConfigManager;
import invtweaks.api.IItemTreeCategory;
import invtweaks.api.IItemTreeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
//import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
//import net.minecraftforge.fml.relauncher.Side;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber()
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
	        	InvTweaksItemTree tree = InvTweaksConfig.getTree();
        		if (tree == null) {
        			return;
        		}
        		String id = current.getItem().getRegistryName().toString();
	        	int dmg = current.getDamage();
	        	CompoundNBT tag = current.getTag();
	        	List<IItemTreeItem> items = tree.getItems(id, dmg, tag);
	            if (items.isEmpty())
	                return;
	
	            Set<String> paths = new HashSet<>();
	            IItemTreeCategory root = InvTweaksConfig.getTree().getRootCategory();
	            int unsortedZone = InvTweaksConfig.getTree().getLastTreeOrder();
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
	                        event.getToolTip().add(new StringTextComponent(TextFormatting.DARK_GRAY + path + " (" + item.getOrder() + ")"));
	                        paths.add(path);
	                    }                    
	                } else {
	                    if (!paths.contains(path)) {
	                        event.getToolTip().add(new StringTextComponent(TextFormatting.DARK_GRAY + "T:" + path + " (" + item.getOrder() + ")"));
	                        paths.add(path);
	                    }
	                    if (!paths.contains(altPath)) {
	                        event.getToolTip().add(new StringTextComponent(TextFormatting.DARK_GRAY + "M:" + altPath + " (" + item.getOrder() + ")"));
	                        paths.add(altPath);
	                    }
	                }
	            }
	        }
        } catch(Exception ex) 
        {}
    }
}
