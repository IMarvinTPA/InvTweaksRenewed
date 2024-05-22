package invtweaks.tree;


import invtweaks.InvTweaksConst;
import invtweaks.InvTweaksMod;
import invtweaks.api.IItemTree;
import invtweaks.api.IItemTreeCategory;
import invtweaks.api.IItemTreeItem;
import invtweaks.util.Utils;
import net.minecraft.world.item.enchantment.*;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.registries.ForgeRegistries;

import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.Map.Entry;

import com.google.common.collect.Multimap;

/**
 * Contains the whole hierarchy of categories and items, as defined in the XML item tree. Is used to recognize keywords
 * and store item orders.
 *
 * @author Jimeo Wan
 */
public class InvTweaksItemTree implements IItemTree {
    public static final String UNKNOWN_ITEM = "unknown";

    private static final Logger log = InvTweaksMod.LOGGER;
    
    /**
     * All categories, stored by name
     */
    
    private Map<String, IItemTreeCategory> categories = new HashMap<>();
    /**
     * Items stored by ID. A same ID can hold several names.
     */
    
    private Map<String, List<IItemTreeItem>> itemsById = new HashMap<>(500);
    /**
     * Items stored by name. A same name can match several IDs.
     */
    
    private Map<String, List<IItemTreeItem>> itemsByName = new HashMap<>(500);

    private String rootCategory;
    
    private List<OreDictInfo> oresRegistered = new ArrayList<>();
    
    private List<ItemStack> allGameItems = new ArrayList<ItemStack>();

    private int highestOrder = 0;
    
    private int lastTreeOrder = 0;

    public InvTweaksItemTree() {
        reset();
    }

    public void reset() {
        // Reset tree
        categories.clear();
        itemsByName.clear();
        itemsById.clear();

    }

    /**
     * Checks if given item ID matches a given keyword (either the item's name is the keyword, or it is in the keyword
     * category)
     */
    @Override
    public boolean matches( List<IItemTreeItem> items,  String keyword) {

        if(items == null) {
            return false;
        }

        // The keyword is an item
        for( IItemTreeItem item : items) {
            if(item.getName() != null && item.getName().equals(keyword)) {
                return true;
            }
        }

        // The keyword is a category
        IItemTreeCategory category = getCategory(keyword);
        if(category != null) {
            for(IItemTreeItem item : items) {
                if(category.contains(item)) {
                    return true;
                }
            }
        }

        // Everything is stuff
        return keyword.equals(rootCategory);

    }

    @Override
    public int getKeywordDepth(String keyword) {
        try {
            return getRootCategory().findKeywordDepth(keyword);
        } catch(NullPointerException e) {
            log.error("The root category is missing: " + e.getMessage());
            return 0;
        }
    }

    @Override
    public int getKeywordOrder(String keyword) {
        List<IItemTreeItem> items = getItems(keyword);
        if(items != null && items.size() != 0) {
            return items.get(0).getOrder();
        } else {
            try {
                return getRootCategory().findCategoryOrder(keyword);
            } catch(NullPointerException e) {
                log.error("The root category is missing: " + e.getMessage());
                return -1;
            }
        }
    }

    /**
     * Checks if the given keyword is valid (i.e. represents either a registered item or a registered category)
     */
    @Override
    public boolean isKeywordValid(String keyword) {

        // Is the keyword an item?
        if(containsItem(keyword)) {
            return true;
        }

        // Or maybe a category ?
        else {
            IItemTreeCategory category = getCategory(keyword);
            return category != null;
        }
    }

    /**
     * Returns a reference to all categories.
     */
    
    @Override
    public Collection<IItemTreeCategory> getAllCategories() {
        return categories.values();
    }

    @Override
    public IItemTreeCategory getRootCategory() {
        return categories.get(rootCategory);
    }

    @Override
    public void setRootCategory( IItemTreeCategory category) {
        rootCategory = category.getName();
        categories.put(rootCategory, category);
    }

    @Override
    public IItemTreeCategory getCategory(String keyword) {
        return categories.get(keyword);
    }

    @Override
    public boolean isItemUnknown(String id, int damage) {
        return itemsById.get(id) == null;
    }

    
    @Override
    public List<IItemTreeItem> getItems( String id, int damage,  CompoundTag extra) {
        if(id == null) {
            return new ArrayList<>();
        }

        List<IItemTreeItem> items = itemsById.get(id);
               
        List<IItemTreeItem> filteredItems = new ArrayList<>();
        if(items != null) {
            filteredItems.addAll(items);
        }

        items = filteredItems;
        filteredItems = new ArrayList<>(items);

        // Filter items that don't match extra data
        if(extra != null && !items.isEmpty()) {
            items.stream().filter(item -> !NbtUtils.compareNbt(item.getExtraData(), extra, true)).forEach(filteredItems::remove);
        }

        // If there's no matching item, create new ones
        if(filteredItems.isEmpty()) {
        	//Make them all sort in the same sort pool at the end.
            int newItemOrder = Integer.MAX_VALUE;
        	//int newItemOrder = highestOrder + 1;
             IItemTreeItem newItemId = new InvTweaksItemTreeItem(String.format("%s-%d", id, damage), id, damage, null,
                    newItemOrder, getRootCategory().getName() + "\\_uncategorized\\" + String.format("%s-%d", id, damage));
             IItemTreeItem newItemDamage = new InvTweaksItemTreeItem(id, id,
                    InvTweaksConst.DAMAGE_WILDCARD, null, newItemOrder, getRootCategory().getName() + "\\_uncategorized\\" + id);
            addItem(getRootCategory().getName(), newItemId);
            //addItem(getRootCategory().getName(), newItemDamage);
            filteredItems.add(newItemId);
            //filteredItems.add(newItemDamage);
        }

        filteredItems.removeIf(Objects::isNull);

        return filteredItems;

    }

    public List<IItemTreeItem> getAllItems() {
        List<IItemTreeItem> flatItems = new ArrayList<>();
        
        if (allGameItems.size() == 0)
        {
            populateGameItems();
        }
        
        for (String id : itemsById.keySet()) {
            ResourceLocation rId = null;
            boolean foundEmpty = false;
            try {
                rId = new ResourceLocation(id);
            }
            catch (Exception ex) {
                //Probably should log this invalid thing.
            }
            //Distill the list to what is actually useful for this server.
            if (rId != null && ForgeRegistries.ITEMS.containsKey(rId)) {
                List<IItemTreeItem> myList = itemsById.get(id);
                for (IItemTreeItem item : myList) {
                    if (foundEmpty == false && item.getExtraData() == null) {
                        foundEmpty = true;
                        //For the generic all filter.
                        flatItems.add(item);
                    } else if (item.getExtraData() != null) {
                        //For special filtering.
                        flatItems.add(item);
                    }
                }
            }
        }
        return flatItems;
    }

    
    @Override
    public List<IItemTreeItem> getItems(String id, int damage) {
        return getItems(id, damage, null);
    }

    @Override
    public List<IItemTreeItem> getItems(String name) {
        return itemsByName.get(name);
    }

    
    @Override
    public IItemTreeItem getRandomItem( Random r) {
        return (IItemTreeItem) itemsByName.values().toArray()[r.nextInt(itemsByName.size())];
    }

    @Override
    public boolean containsItem(String name) {
        return itemsByName.containsKey(name);
    }

    @Override
    public boolean containsCategory(String name) {
        return categories.containsKey(name);
    }

    
    @Override
    public IItemTreeCategory addCategory(String parentCategory, String newCategory) throws NullPointerException {
         IItemTreeCategory addedCategory = new InvTweaksItemTreeCategory(newCategory);
        addCategory(parentCategory, addedCategory);
        return addedCategory;
    }

    
    @Override
    public IItemTreeItem addItem(String parentCategory, String name, String id, int damage, int order)
            throws NullPointerException {
        return addItem(parentCategory, name, id, damage, null, order);
    }

    
    @Override
    public IItemTreeItem addItem(String parentCategory, String name, String id, int damage, CompoundTag extra, int order)
            throws NullPointerException {
         InvTweaksItemTreeItem addedItem = new InvTweaksItemTreeItem(name, id, damage, extra, order, getRootCategory().findKeywordPath(parentCategory) + "\\" + name);
        addItem(parentCategory, addedItem);
        return addedItem;
    }

    @Override
    public void addCategory(String parentCategory,  IItemTreeCategory newCategory) throws NullPointerException {
        // Build tree
        categories.get(parentCategory).addCategory(newCategory);

        // Register category
        categories.put(newCategory.getName(), newCategory);
    }

    @Override
    public void addItem(String parentCategory,  IItemTreeItem newItem) throws NullPointerException {
        highestOrder = Math.max(highestOrder, newItem.getOrder());

        // Build tree
        categories.get(parentCategory).addItem(newItem);

        // Register item
        if(itemsByName.containsKey(newItem.getName())) {
            itemsByName.get(newItem.getName()).add(newItem);
        } else {
             List<IItemTreeItem> list = new ArrayList<>();
            list.add(newItem);
            itemsByName.put(newItem.getName(), list);
        }
        
        if(itemsById.containsKey(newItem.getId())) {
            List<IItemTreeItem> list = itemsById.get(newItem.getId());
            if (list.get(0).getOrder() == Integer.MAX_VALUE) {
                list.remove(0);
            }
            list.add(newItem);
        } else {
            List<IItemTreeItem> list = new ArrayList<>();
            list.add(newItem);
            itemsById.put(newItem.getId(), list);
        }
    }

    public int getHighestOrder() {
        return highestOrder;
    }

    public int getLastTreeOrder() {
        return lastTreeOrder;
    }

    @Override
    public void registerOre(String category, String name, String oreName, int order, String path) {
    	try {
	    	ResourceLocation tagId = new ResourceLocation(oreName.toLowerCase());
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);
	    	
	        for( Item i :  ForgeRegistries.ITEMS.tags().getTag(tagKey)) {
	            if(i != null) {
	                addItem(category,
	                        new InvTweaksItemTreeItem(name, Utils.getItemRegistryString(i), InvTweaksConst.DAMAGE_WILDCARD, null, order, path));
                            //log.info(String.format("An OreDictionary name '%s' has '%s'", oreName, i.getRegistryName().toString()));
	            } else {
	                log.warn(String.format("An OreDictionary entry for %s is null", oreName));
	            }
	        }
            //if (ItemTags.getCollection().getTagByID(tagId).getAllElements().size() == 0) {
                //log.info(String.format("An OreDictionary name '%s'is empty.", oreName));
            //}
	        oresRegistered.add(new OreDictInfo(category, name, oreName, order, path));
    	} catch (Exception ex) {
    		log.warn(String.format("An OreDictionary name '%s' contains invalid characters.", oreName));
    	}
    	
    }

    public void tagsRegistered() {
        oresRegistered.stream().forEach(ore -> {
            ResourceLocation tagId = new ResourceLocation(ore.oreName.toLowerCase()); //Should be only safe tag names by this point.
            TagKey<Item> tagKey = TagKey.create(Registries.ITEM, tagId);	    	
	        for( Item i :  ForgeRegistries.ITEMS.tags().getTag(tagKey)) {
	            if(i != null) {
	                addItem(ore.category,
	                        new InvTweaksItemTreeItem(ore.name, Utils.getItemRegistryString(i), InvTweaksConst.DAMAGE_WILDCARD, null, ore.order, ore.orePath));
                            //log.info(String.format("An OreDictionary name '%s' has '%s'", oreName, i.getRegistryName().toString()));
	            }
            }
        });
    }
    
   
    public void registerClass(String category, String name, String className, CompoundTag extraData, int order, String path)
    {
        if (allGameItems.size() == 0)
        {
            populateGameItems();
        }
        
        for(ItemStack stack : allGameItems)
        {
            Item item = stack.getItem();
            boolean isClass = InstanceOfClassNameKind(item, className);
            if (isClass) {
                boolean doIt = true;
                if (extraData != null) {
                    if (doIt && extraData.contains("toolclass")) 
                    {
                        String tclass = extraData.getString("toolclass");
                        //We don't want the set, we want the one we will use during comparisons.
                        //An empty toolclass will match non-tools.                        
                        doIt = tclass.equals(getToolClass(stack, item));                        
                    }
                    if (doIt && extraData.contains("armortype")) 
                    {
                    	if (item instanceof ArmorItem) {
	                        ArmorItem armor = (ArmorItem) item;
	                        String keyArmorType = extraData.getString("armortype");
	                        String itemArmorType = armor.getEquipmentSlot().getName().toLowerCase();
	                        doIt = (keyArmorType.equals(itemArmorType));
	                        armor = null;
                    	} else {
                    		//The ArmorItem isn't a proper ArmorItem (HorseArmorItem)
                    		doIt = false;
                    	}
                    	
                    }
                    if (doIt && extraData.contains("isshield")) 
                    {                        
                        doIt = item.canPerformAction(stack, ToolActions.SHIELD_BLOCK);
                    }
                }
                //Checks out, add it to the tree:
                if (doIt) {
                    int dmg = item.isDamageable(stack) ? InvTweaksConst.DAMAGE_WILDCARD : stack.getDamageValue();
                    addItem(category,
                            new InvTweaksItemTreeItem(name, Utils.getItemRegistryString(item), dmg, null, order, path));
                }
            }
        }
    }

    private static class OreDictInfo {
        String category;
        String name;
        String oreName;
        int order;
        String orePath;

        OreDictInfo(String category_, String name_, String oreName_, int order_, String orePath_) {
            category = category_;
            name = name_;
            oreName = oreName_;
            order = order_;
            orePath = orePath_;
        }
    }
    
    private void populateGameItems()
    {
        for (Item item: ForgeRegistries.ITEMS)
        {            
            allGameItems.add(item.getDefaultInstance());
            //addData(itemDump, item, rl, false, includeToolClass, dumpNBT, new ItemStack(item, 1, 0));
        }
    }
    
    private boolean InstanceOfClassNameKind(Object o, String className)
    {
        @SuppressWarnings("rawtypes")
        Class testClass = o.getClass();
        while (testClass != null)
        {
            if (testClass.getName().toLowerCase().endsWith(className))
                return true;
            //The secret sauce:
            testClass = testClass.getSuperclass();
        }
        return false;
    }
    
    public void endFileRead()
    {
        //We are done with this, let's release the memory.
        allGameItems.clear();
        
        //Remember where the last entry was placed in the tree for the API to leave these unsorted.
        lastTreeOrder = highestOrder;
    }

    public TreeComparator getTreeComparator() {
        return new TreeComparator(this);
    }
    
    public class TreeComparator implements Comparator<ItemStack>
    {
        final private InvTweaksItemTree tree;
        final private boolean isAPI;
        public TreeComparator(InvTweaksItemTree myTree, boolean useApiMode) {
            tree = myTree;
            isAPI = useApiMode;
        }

        public TreeComparator(InvTweaksItemTree myTree) {
            tree = myTree;
            isAPI = false;
        }

        @Override
        public int compare(ItemStack o1, ItemStack o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return 1;
            else if (o2 == null)
                return -1;
            return tree.compareItems(o1, o2, isAPI);
        }
    }

    public int getItemOrder(ItemStack itemStack) {
        List<IItemTreeItem> items = this.getItems(Utils.getItemRegistryString(itemStack),
                itemStack.getDamageValue(), itemStack.getTag());
        int min_value = Integer.MAX_VALUE;
        for (IItemTreeItem item : items) {
            if (item.getOrder() < min_value) {
                min_value = item.getOrder();
            }
        }
        return min_value;
    }

    public int compareItems(ItemStack i, ItemStack j) {
        return compareItems(i, j, getItemOrder(i), getItemOrder(j), false);
    }

    public int compareItems(ItemStack i, ItemStack j, boolean onlyTreeSort) {
        return compareItems(i, j, getItemOrder(i), getItemOrder(j), onlyTreeSort);
    }

    public boolean debugTree = true;
    public String mostRecentComparison = "";
    
    public int compareItems(ItemStack i, ItemStack j, int orderI, int orderJ, boolean api) {
        if (i.isEmpty() && j.isEmpty()) {
            //Technically, if both are empty, they are equal.
            if (debugTree) mostRecentComparison = "Both stacks are Empty.";
            return 0;
        } else if(j.isEmpty()) {            
            if (debugTree) mostRecentComparison = "J is Empty.";
            return -1;
        } else if(i.isEmpty() || orderI == -1) {
            if (debugTree) mostRecentComparison = "I is Empty or orderI was -1.";
            return 1;
        } else {
            if (debugTree) mostRecentComparison = "";
            if (api) {
                if (debugTree) mostRecentComparison = "API Active, ";
                int lastOrder = this.getLastTreeOrder();
                if (orderI > lastOrder) orderI = Integer.MAX_VALUE;
                if (orderJ > lastOrder) orderJ = Integer.MAX_VALUE;
            }
            
            if (debugTree) mostRecentComparison += "I: " + orderI + ", J: " + orderJ;

            //If items are in different order slots, they are inherently comparator contract friendly.
            if(orderI != orderJ) {
                if (debugTree) mostRecentComparison += ", Normal: " + (orderI - orderJ);
                return orderI - orderJ;
            }
            
            //All items in the same sort slot need to be treated the same for the comparator contract.

            //Allow external sorting systems to take control of unsorted items not handled by the tree.
            if (orderI == Integer.MAX_VALUE && orderJ == Integer.MAX_VALUE && api == true) {
                if (debugTree) mostRecentComparison += ", API Bailout.";
                return 0;
            }

            Item iItem = i.getItem(), jItem = j.getItem();
            //Sort By Tool type then Harvest Level, (Better first.)
            int cTool = compareTools(i, j, iItem, jItem);
            if (debugTree) mostRecentComparison += ", Tool: " + cTool;
            if (cTool != 0)
                return cTool;
            
            //Sort by main-hand damage capability:  (Higher first, faster first for same damage)
            //Most tools also do damage, so they were tested as tools first.
            //If a tool reaches here, it has the same max durabilty, harvest level, and tool class.
            int cSword = compareSword(i, j, iItem, jItem);
            if (debugTree) mostRecentComparison += ", Sword: " + cSword;
            if (cSword != 0)
                return cSword;
            
            //Sort By Armor utility:  (More First)
            int cArmor = compareArmor(i, j, iItem, jItem);
            if (debugTree) mostRecentComparison += ", Armor: " + cArmor;
            if (cArmor != 0)
                return cArmor;
                            
            //Sort my display name:
            int cName = compareNames(i, j);
            if (debugTree) mostRecentComparison += ", Name" + cName;
            if (cName != 0)
                return cName;
            
            //Sort By enchantments:
            int cEnchant = compareEnchantment(i, j);
            if (cEnchant != 0)
                return cEnchant;
                
            //Use durability to sort, favoring more durable items.  (Non-Tools, Non-Armor, Non-Weapons.)
            int maxDamage = compareMaxDamage(i, j);
            if (debugTree) mostRecentComparison += ", Max Damage: " + maxDamage;
            if (maxDamage != 0)
                return maxDamage;  

            //Use remaining durability to sort, favoring config option on damaged.
            int curDamage = compareCurDamage(i, j);
            if (debugTree) mostRecentComparison += ", Current Damage: " + curDamage;
            if (curDamage != 0)
                return curDamage;  

            //Use stack size to put bigger stacks first.
            if (j.getCount() != i.getCount()) {
                 if (debugTree) mostRecentComparison += ", Stack Size";
                return j.getCount() - i.getCount();
            }
            
            //Final catch all:
            if (debugTree) mostRecentComparison += ", Final: " + Utils.getItemRegistryString(i).compareTo(Utils.getItemRegistryString(j));
            return Utils.getItemRegistryString(i).compareTo(Utils.getItemRegistryString(j));
            
        }
    }   
    
    private int compareNames(ItemStack i, ItemStack j) {
        boolean iHasName = false;
        boolean jHasName = false;
        String iDisplayName = i.getDisplayName().getString();
        String jDisplayName = j.getDisplayName().getString();

        //Custom named items come first.
        if(iHasName || jHasName) {
            if(!iHasName) {
                if (debugTree) mostRecentComparison += ", J has custom Name";
                return -1;
            } else if(!jHasName) {
                if (debugTree) mostRecentComparison += ", I has custom Name";
                return 1;
            }
        }
        //Differently named items (either both custom or both default, like bees or resource chickens.) 
        if(!iDisplayName.equals(jDisplayName)) {
            if (debugTree) mostRecentComparison += ", Name: " + iDisplayName.compareTo(jDisplayName);
            return iDisplayName.compareTo(jDisplayName);
        }
        
        return 0;        
    }
    
    private int compareMaxDamage(ItemStack i, ItemStack j) {
        //Use durability to sort, favoring more durable items.
        int maxDamage1 = i.getMaxDamage() <= 0 ? Integer.MAX_VALUE : i.getMaxDamage();
        int maxDamage2 = j.getMaxDamage() <= 0 ? Integer.MAX_VALUE : j.getMaxDamage();
        return maxDamage2 - maxDamage1;      	
    }

    private int compareCurDamage(ItemStack i, ItemStack j) {
        //Use remaining durability to sort, favoring more damaged.
        int curDamage1 = i.getDamageValue();
        int curDamage2 = j.getDamageValue();
        //if(i.isDamageable() && !InvTweaksConfig.getConfig().getProperty(InvTweaksConfig.PROP_INVERT_TOOL_DAMAGE).equals(InvTweaksConfig.VALUE_TRUE)) {
        //    return curDamage2 - curDamage1;
        //} else {
            return curDamage1 - curDamage2;
        //}
    }
    
    public String getToolClass(ItemStack itemStack, Item item)
    {
        if (itemStack == null || item == null) return "";
        //ToolActions.
        if (itemStack.canPerformAction(ToolActions.PICKAXE_DIG)) {
            return "pickaxe";
        }
        if (itemStack.canPerformAction(ToolActions.AXE_DIG)) {
            return "axe";
        }
        if (itemStack.canPerformAction(ToolActions.SHOVEL_DIG)) {
            return "shovel";
        }
        if (itemStack.canPerformAction(ToolActions.HOE_DIG) || itemStack.canPerformAction(ToolActions.HOE_TILL)) {
            return "hoe";
        }
        if (itemStack.canPerformAction(ToolActions.SHEARS_HARVEST) || itemStack.canPerformAction(ToolActions.SHEARS_DIG)) {
            return "shears";
        }
        if (itemStack.canPerformAction(ToolActions.FISHING_ROD_CAST)) {
            return "fishingrod";
        }

        if (item instanceof HoeItem) return "hoe";
        if (item instanceof ShearsItem) return "shears";
        if (item instanceof FishingRodItem) return "fishingrod";
        
        return "";
    }
    
    private int compareTools(ItemStack i, ItemStack j, Item iItem, Item jItem)
    {
        String toolClass1 = getToolClass(i, iItem);
        String toolClass2 = getToolClass(j, jItem);

        if (debugTree) mostRecentComparison += ", ToolClass (" + toolClass1 + ", " + toolClass2 + ")";
        boolean isTool1 = toolClass1 != "";
        boolean isTool2 = toolClass2 != "";
        if (!isTool1  || !isTool2 ) {
            //This should catch any instances where one of the stacks is null.
            return Boolean.compare(isTool2, isTool1);
        } else {
            int toolClassComparison = toolClass1.compareTo(toolClass2);
            if (toolClassComparison != 0) {
                return toolClassComparison;
            }

            // // If they were the same type, sort with the better harvest level first.
            // int harvestLevel1 = i.tier .getHarvestLevel(i, i.getToolTypes().iterator().next(), null, null);
            // int harvestLevel2 = jItem.getHarvestLevel(j, j.getToolTypes().iterator().next(), null, null);
            // int toolLevelComparison = harvestLevel2 - harvestLevel1;
            // if (debugTree) mostRecentComparison += ", HarvestLevel (" + harvestLevel1 + ", " + harvestLevel2 + ")";
            // if (toolLevelComparison != 0) {
            //     return Integer.compare(harvestLevel2 , harvestLevel1);
            // }
        }
        
        return compareMaxDamage(i, j);
        
    }

    private int compareSword(ItemStack itemStack1, ItemStack itemStack2, Item iItem, Item jItem)
    {
        Multimap<Attribute, AttributeModifier> multimap1 = itemStack1 != null ? itemStack1.getAttributeModifiers(EquipmentSlot.MAINHAND) : null;
        Multimap<Attribute, AttributeModifier> multimap2 = itemStack2 != null ? itemStack2.getAttributeModifiers(EquipmentSlot.MAINHAND) : null;

        boolean hasDamage1 = itemStack1 != null ? multimap1.containsKey(Attributes.ATTACK_DAMAGE) : false;
        boolean hasDamage2 = itemStack2 != null ? multimap2.containsKey(Attributes.ATTACK_DAMAGE) : false;
        boolean hasSpeed1 = itemStack1 != null ? multimap1.containsKey(Attributes.ATTACK_SPEED) : false;
        boolean hasSpeed2 = itemStack2 != null ? multimap2.containsKey(Attributes.ATTACK_SPEED) : false;

        if (debugTree) mostRecentComparison += ", HasDamage (" + hasDamage1 + ", " + hasDamage2 + ")";
        
        if (!hasDamage1 || !hasDamage2) {
            return Boolean.compare(hasDamage2, hasDamage1);
        } else {
            Collection<AttributeModifier> damageMap1 = multimap1.get(Attributes.ATTACK_DAMAGE);
            Collection<AttributeModifier> damageMap2 = multimap2.get(Attributes.ATTACK_DAMAGE);
            Double attackDamage1 = ((AttributeModifier) damageMap1.toArray()[0]).getAmount();
            Double attackDamage2 = ((AttributeModifier) damageMap2.toArray()[0]).getAmount();
            // This funny comparison is because Double == Double never seems to work.
            int damageComparison = Double.compare(attackDamage2, attackDamage1);
            if (damageComparison == 0 && hasSpeed1 && hasSpeed2) {
                // Same damage, sort faster weapon first.
                Collection<AttributeModifier> speedMap1 = multimap1.get(Attributes.ATTACK_SPEED);
                Collection<AttributeModifier> speedMap2 = multimap2.get(Attributes.ATTACK_SPEED);
                Double speed1 = ((AttributeModifier) speedMap1.toArray()[0]).getAmount();
                Double speed2 = ((AttributeModifier) speedMap2.toArray()[0]).getAmount();
                int speedComparison = Double.compare(speed2, speed1);
                if (speedComparison != 0)
                    return speedComparison;

            } else if (damageComparison != 0) {
                // Higher damage first.
                return damageComparison;
            } 
            return compareMaxDamage(itemStack1, itemStack2);
        }
    }
    
    private int compareArmor(ItemStack i, ItemStack j, Item iItem, Item jItem)
    {
        int isArmor1 = (iItem instanceof ArmorItem) ? 1 : 0;
        int isArmor2 = (jItem instanceof ArmorItem) ? 1 : 0;
        if (isArmor1 == 0 || isArmor2 == 0) { 
            //This should catch any instances where one of the stacks is null.
            return isArmor2 - isArmor1;
        } else {
        	ArmorItem a1 = (ArmorItem) iItem;
        	ArmorItem a2 = (ArmorItem) jItem;
            if (a1.getEquipmentSlot() != a2.getEquipmentSlot()) {
                return a2.getEquipmentSlot().compareTo(a1.getEquipmentSlot());
            } else if (a1.getDefense() != a2.getDefense()) {
                return a2.getDefense() - a1.getDefense();
            } else if (a1.getToughness() != a2.getToughness()) {
                return a2.getToughness() > a1.getToughness() ? -1 : 1;
            }
            return compareMaxDamage(i, j);
        }
    }
    
    private int compareEnchantment(ItemStack i, ItemStack j) {
        Map<Enchantment, Integer> iEnchs = EnchantmentHelper.getEnchantments(i);
        Map<Enchantment, Integer> jEnchs = EnchantmentHelper.getEnchantments(j);
        
        //Pick the item with the most enchantments first.
        if(iEnchs.size() != jEnchs.size()) {
            if (debugTree) mostRecentComparison += ", Enchantment Count";
            return jEnchs.size() - iEnchs.size();            
        }
        
        //Just a random seed value.  The Level 0 bit will help it clear up any real enchantments.
        Enchantment iEnchMaxId = Enchantments.AQUA_AFFINITY, jEnchMaxId = Enchantments.AQUA_AFFINITY;
        int iEnchMaxLvl = 0;
        int jEnchMaxLvl = 0;

        // TODO: This is really arbitrary but there's not really a good way to do this generically.
        for(Map.Entry<Enchantment, Integer> ench : iEnchs.entrySet()) {
        	Enchantment enchId = ench.getKey();
            if(ench.getValue() > iEnchMaxLvl) {
                iEnchMaxId = enchId;
                iEnchMaxLvl = ench.getValue();
            } else if(ench.getValue() == iEnchMaxLvl && enchId.getRarity().compareTo(iEnchMaxId.getRarity()) > 0) {
                iEnchMaxId = enchId;
            }
        }

        for(Map.Entry<Enchantment, Integer> ench : jEnchs.entrySet()) {
        	Enchantment enchId = ench.getKey();
            if(ench.getValue() > jEnchMaxLvl) {
                jEnchMaxId = enchId;
                jEnchMaxLvl = ench.getValue();
            } else if(ench.getValue() == jEnchMaxLvl && enchId.getRarity().compareTo(jEnchMaxId.getRarity()) > 0) {
                jEnchMaxId = enchId;
            }
        }

        //The highest enchantment ID, (random actual enchantment.)
        if(iEnchMaxId != jEnchMaxId) {
            if (debugTree) mostRecentComparison += ", Highest Enchantment";
            return iEnchMaxId.getRarity().compareTo(jEnchMaxId.getRarity());                        
        }
        
        //Highest level if they both have the same coolest enchantment.
        if(iEnchMaxLvl != jEnchMaxLvl) {
            if (debugTree) mostRecentComparison += ", Highest Enchantment Level";
            return jEnchMaxLvl - iEnchMaxLvl;
        }
        
        //Enchantments aren't different.
        if (debugTree) mostRecentComparison += ", Enchantment Level same";
        return 0;
    }

}
