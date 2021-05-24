package invtweaks.util;

// import java.lang.reflect.*;

import com.google.common.base.Equivalence;
import com.google.common.collect.Multimap;
import com.google.common.collect.Streams;

import invtweaks.api.IItemTreeItem;
import invtweaks.config.InvTweaksConfig;
import it.unimi.dsi.fastutil.ints.IntArrays;
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet;
import net.minecraft.inventory.container.Slot;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.FishingRodItem;
import net.minecraft.item.HoeItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.ToolType;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;


import javax.annotation.ParametersAreNonnullByDefault;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

// import net.minecraftforge.fml.common.*;

public class Utils {
    public static final Equivalence<ItemStack> STACKABLE =
            new Equivalence<ItemStack>() {

                @Override
                @ParametersAreNonnullByDefault
                protected boolean doEquivalent(ItemStack a, ItemStack b) {
                    return ItemHandlerHelper.canItemStacksStack(a, b);
                }

                @Override
                protected int doHash(ItemStack t) {
                    List<Object> objs = new ArrayList<>(2);
                    if (!t.isEmpty()) {
                        objs.add(t.getItem());
                        if (t.hasTag()) {
                            objs.add(t.getTag());
                        }
                    }
                    return Arrays.hashCode(objs.toArray());
                }
            };
    // TODO improve fallback comparator
    public static final Comparator<ItemStack> FALLBACK_COMPARATOR =
            Comparator.comparing(is -> is.getItem().getRegistryName());

    public static int gridToPlayerSlot(int row, int col) {
        if (row < 0 || row >= 4 || col < 0 || col >= 9) {
            throw new IllegalArgumentException("Invalid coordinates (" + row + ", " + col + ")");
        }
        return ((row + 1) % 4) * 9 + col;
    }

    public static int gridRowToInt(String str) {
        if (str.length() != 1 || str.charAt(0) < 'A' || str.charAt(0) > 'D') {
            throw new IllegalArgumentException("Invalid grid row: " + str);
        }
        return str.charAt(0) - 'A';
    }

    public static int gridColToInt(String str) {
        if (str.length() != 1 || str.charAt(0) < '1' || str.charAt(0) > '9') {
            throw new IllegalArgumentException("Invalid grid column: " + str);
        }
        return str.charAt(0) - '1';
    }

    public static int[] gridSpecToSlots(String str, boolean global) {
        if (str.endsWith("rv")) {
            return gridSpecToSlots(str.substring(0, str.length() - 2) + "vr", global);
        }
        if (str.endsWith("r")) {
            return IntArrays.reverse(gridSpecToSlots(str.substring(0, str.length() - 1), global));
        }
        boolean vertical = false;
        if (str.endsWith("v")) {
            vertical = true;
            str = str.substring(0, str.length() - 1);
        }
        String[] parts = str.split("-");
        if (parts.length == 1) { // single point or row/column
            if (str.length() == 1) { // row/column
                try {
                    int row = gridRowToInt(str);
                    if (global) return gridSpecToSlots("A1-D9", false);
                    return IntStream.rangeClosed(0, 8).map(col -> gridToPlayerSlot(row, col)).toArray();
                } catch (IllegalArgumentException e) {
                    int col = gridColToInt(str);
                    if (global) return gridSpecToSlots("D1-A9v", false);
                    return directedRangeInclusive(3, 0).map(row -> gridToPlayerSlot(row, col)).toArray();
                }
            } else if (str.length() == 2) { // single point
                if (global) return gridSpecToSlots("A1-D9", false);
                return new int[]{
                        gridToPlayerSlot(gridRowToInt(str.substring(0, 1)), gridColToInt(str.substring(1, 2)))
                };
            } else {
                throw new IllegalArgumentException("Bad grid spec: " + str);
            }
        } else if (parts.length == 2) { // rectangle
            if (parts[0].length() == 2 && parts[1].length() == 2) {
                int row0 = gridRowToInt(parts[0].substring(0, 1));
                int col0 = gridColToInt(parts[0].substring(1, 2));
                int row1 = gridRowToInt(parts[1].substring(0, 1));
                int col1 = gridColToInt(parts[1].substring(1, 2));

                if (global) {
                    if (row0 > row1) {
                        row0 = 3;
                        row1 = 0;
                    } else {
                        row0 = 0;
                        row1 = 3;
                    }
                    if (col0 > col1) {
                        col0 = 8;
                        col1 = 0;
                    } else {
                        col0 = 0;
                        col1 = 8;
                    }
                }

                int _row0 = row0, _row1 = row1, _col0 = col0, _col1 = col1;

                if (vertical) {
                    return directedRangeInclusive(col0, col1)
                            .flatMap(
                                    col ->
                                            directedRangeInclusive(_row0, _row1).map(row -> gridToPlayerSlot(row, col)))
                            .toArray();
                } else {
                    return directedRangeInclusive(row0, row1)
                            .flatMap(
                                    row ->
                                            directedRangeInclusive(_col0, _col1).map(col -> gridToPlayerSlot(row, col)))
                            .toArray();
                }
            } else {
                throw new IllegalArgumentException("Bad grid spec: " + str);
            }
        } else {
            throw new IllegalArgumentException("Bad grid spec: " + str);
        }
    }

    public static IntStream directedRangeInclusive(int start, int end) {
        return IntStream.iterate(start, v -> (start > end ? v - 1 : v + 1))
                .limit(Math.abs(end - start) + 1);
    }

    public static <T extends Collection<ItemStack>> T collated(
            Iterable<ItemStack> iterable, Supplier<T> collSupp) {
        //noinspection UnstableApiUsage
        Map<Equivalence.Wrapper<ItemStack>, List<ItemStack>> mapping =
                Streams.stream(iterable)
                        .collect(
                                Collectors.groupingBy(STACKABLE::wrap, LinkedHashMap::new, Collectors.toList()));
        return mapping.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toCollection(collSupp));
    }

    /**
     * The {@code Set<Slot>} values' iterators are guaranteed to be {@code ListIterator}s.
     */
    public static Map<Equivalence.Wrapper<ItemStack>, Set<Slot>> gatheredSlots(
            Iterable<Slot> iterable) {
        //noinspection UnstableApiUsage
        return Streams.stream(iterable)
                .collect(
                        Collectors.groupingBy(
                                sl -> STACKABLE.wrap(sl.getStack().copy()), // @#*! itemstack mutability
                                LinkedHashMap::new,
                                Collectors.toCollection(ObjectLinkedOpenHashSet::new)));
    }

    // @SuppressWarnings("unchecked")
    public static List<ItemStack> condensed(Iterable<ItemStack> iterable) {
        List<ItemStack> coll = collated(iterable, ArrayList::new);
        // TODO special handling for Nether Chests-esque mods?
        ItemStackHandler stackBuffer = new ItemStackHandler(coll.size());
        int index = 0;
        for (ItemStack stack : coll) {
            stack = stack.copy();
            while (!(stack = stackBuffer.insertItem(index, stack, false)).isEmpty()) {
                ++index;
            }
        }
        return IntStream.range(0, stackBuffer.getSlots())
                .mapToObj(stackBuffer::getStackInSlot)
                .filter(is -> !is.isEmpty())
                .collect(Collectors.toList());
    }
    
    public static TreeComparator TREE_COMPARATOR = new TreeComparator();
    
    public static class TreeComparator implements Comparator<ItemStack>
    {
        @Override
        public int compare(ItemStack o1, ItemStack o2) {
            if (o1 == null && o2 == null)
                return 0;
            else if (o1 == null)
                return 1;
            else if (o2 == null)
                return -1;
            return Utils.compareItems(o1, o2, false);
        }
    }

    public static int getItemOrder(ItemStack itemStack) {
        // TODO: It looks like Mojang changed the internal name type to ResourceLocation. Evaluate how much of a pain that will be.
        List<IItemTreeItem> items = InvTweaksConfig.getTree().getItems(itemStack.getItem().getRegistryName().toString(),
                itemStack.getDamage(), itemStack.getTag());
        return (items.size() > 0) ? items.get(0).getOrder() : Integer.MAX_VALUE;
    }

    public static int compareItems(ItemStack i, ItemStack j) {
        return compareItems(i, j, getItemOrder(i), getItemOrder(j), false);
    }

    public static int compareItems(ItemStack i, ItemStack j, boolean onlyTreeSort) {
        return compareItems(i, j, getItemOrder(i), getItemOrder(j), onlyTreeSort);
    }

    static public boolean debugTree = true;
    static public String mostRecentComparison = "";
    
    static int compareItems(ItemStack i, ItemStack j, int orderI, int orderJ, boolean api) {
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
                int lastOrder = InvTweaksConfig.getTree().getLastTreeOrder();
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
            if (debugTree) mostRecentComparison += ", Final: " + i.getItem().getRegistryName().toString().compareTo(j.getItem().getRegistryName().toString());
            // TODO: It looks like Mojang changed the internal name type to ResourceLocation. Evaluate how much of a pain that will be.
            return i.getItem().getRegistryName().toString().compareTo(j.getItem().getRegistryName().toString());
    		/*return String.(i.getItem().getRegistryName().toString(),
                    j.getItem().getRegistryName().toString());*/
            
        }
    }   
    
    private static int compareNames(ItemStack i, ItemStack j) {
        boolean iHasName = i.hasDisplayName();
        boolean jHasName = j.hasDisplayName();
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
    
    private static int compareMaxDamage(ItemStack i, ItemStack j) {
        //Use durability to sort, favoring more durable items.
        int maxDamage1 = i.getMaxDamage() <= 0 ? Integer.MAX_VALUE : i.getMaxDamage();
        int maxDamage2 = j.getMaxDamage() <= 0 ? Integer.MAX_VALUE : j.getMaxDamage();
        return maxDamage2 - maxDamage1;      	
    }

    private static int compareCurDamage(ItemStack i, ItemStack j) {
        //Use remaining durability to sort, favoring more damaged.
        int curDamage1 = i.getDamage();
        int curDamage2 = j.getDamage();
        //if(i.isDamageable() && !InvTweaksConfig.getConfig().getProperty(InvTweaksConfig.PROP_INVERT_TOOL_DAMAGE).equals(InvTweaksConfig.VALUE_TRUE)) {
        //    return curDamage2 - curDamage1;
        //} else {
            return curDamage1 - curDamage2;
        //}
    }
    
    public static String getToolClass(ItemStack itemStack, Item item)
    {
        if (itemStack == null || item == null) return "";
        Set<ToolType> toolTypeSet = item.getToolTypes(itemStack);
        
        Set<String> toolClassSet = new HashSet<String>();

        for (ToolType toolClass: toolTypeSet) {
            //Swords are not "tools".
            if (toolClass.getName() != "sword") {
            	toolClassSet.add(toolClass.getName());
            }
        }

        //Minecraft hoes, shears, and fishing rods don't have tool class names.
        if (toolClassSet.isEmpty()) {
            if (item instanceof HoeItem) return "hoe";
            if (item instanceof ShearsItem) return "shears";
            if (item instanceof FishingRodItem) return "fishingrod";
            return "";
        }
        
        //Get the only thing.
        if (toolClassSet.size() == 1)
            return (String) toolClassSet.toArray()[0];
        
        //We have a preferred type to list tools under, primarily the pickaxe for harvest level.
        String[] prefOrder = {"pickaxe", "axe", "shovel", "hoe", "shears", "wrench"};
        for (int i = 0; i < prefOrder.length; i++)
            if (toolClassSet.contains(prefOrder[i])) 
                return prefOrder[i];
        
        //Whatever happens to be the first thing:
        return (String) toolClassSet.toArray()[0];
    }
    
    private static int compareTools(ItemStack i, ItemStack j, Item iItem, Item jItem)
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
            
            // If they were the same type, sort with the better harvest level first.
            int harvestLevel1 = iItem.getHarvestLevel(i, i.getToolTypes().iterator().next(), null, null);
            int harvestLevel2 = jItem.getHarvestLevel(j, j.getToolTypes().iterator().next(), null, null);
            int toolLevelComparison = harvestLevel2 - harvestLevel1;
            if (debugTree) mostRecentComparison += ", HarvestLevel (" + harvestLevel1 + ", " + harvestLevel2 + ")";
            if (toolLevelComparison != 0) {
                return Integer.compare(harvestLevel2 , harvestLevel1);
            }
        }
        
        return compareMaxDamage(i, j);
        
    }

    private static int compareSword(ItemStack itemStack1, ItemStack itemStack2, Item iItem, Item jItem)
    {
        Multimap<Attribute, AttributeModifier> multimap1 = itemStack1 != null ? itemStack1.getAttributeModifiers(EquipmentSlotType.MAINHAND) : null;
        Multimap<Attribute, AttributeModifier> multimap2 = itemStack2 != null ? itemStack2.getAttributeModifiers(EquipmentSlotType.MAINHAND) : null;

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
    
    private static int compareArmor(ItemStack i, ItemStack j, Item iItem, Item jItem)
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
            } else if (a1.getDamageReduceAmount() != a2.getDamageReduceAmount()) {
                return a2.getDamageReduceAmount() - a1.getDamageReduceAmount();
            } else if (a1.getToughness() != a2.getToughness()) {
                return a2.getToughness() > a1.getToughness() ? -1 : 1;
            }
            return compareMaxDamage(i, j);
        }
    }
    
    private static int compareEnchantment(ItemStack i, ItemStack j) {
        Map<Enchantment, Integer> iEnchs = EnchantmentHelper.getEnchantments(i);
        Map<Enchantment, Integer> jEnchs = EnchantmentHelper.getEnchantments(j);
        
        //Pick the item with the most enchantments first.
        if(iEnchs.size() != jEnchs.size()) {
            if (debugTree) mostRecentComparison += ", Enchantment Count";
            return jEnchs.size() - iEnchs.size();            
        }
        
        //Just a random seed value.  The Level 0 bit will help it clear up any real enchantments.
        ResourceLocation iEnchMaxId = Enchantments.AQUA_AFFINITY.getRegistryName(), jEnchMaxId = Enchantments.AQUA_AFFINITY.getRegistryName();
        int iEnchMaxLvl = 0;
        int jEnchMaxLvl = 0;

        // TODO: This is really arbitrary but there's not really a good way to do this generically.
        for(Map.Entry<Enchantment, Integer> ench : iEnchs.entrySet()) {
        	ResourceLocation enchId = ench.getKey().getRegistryName();
            if(ench.getValue() > iEnchMaxLvl) {
                iEnchMaxId = enchId;
                iEnchMaxLvl = ench.getValue();
            } else if(ench.getValue() == iEnchMaxLvl && enchId.compareTo(iEnchMaxId) > 0) {
                iEnchMaxId = enchId;
            }
        }

        for(Map.Entry<Enchantment, Integer> ench : jEnchs.entrySet()) {
        	ResourceLocation enchId = ench.getKey().getRegistryName();
            if(ench.getValue() > jEnchMaxLvl) {
                jEnchMaxId = enchId;
                jEnchMaxLvl = ench.getValue();
            } else if(ench.getValue() == jEnchMaxLvl && enchId.compareTo(jEnchMaxId) > 0) {
                jEnchMaxId = enchId;
            }
        }

        //The highest enchantment ID, (random actual enchantment.)
        if(iEnchMaxId != jEnchMaxId) {
            if (debugTree) mostRecentComparison += ", Highest Enchantment";
            return iEnchMaxId.compareTo(jEnchMaxId);                        
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
