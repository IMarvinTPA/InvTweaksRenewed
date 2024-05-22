package invtweaks.tree;

import invtweaks.api.IItemTreeItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;

import java.util.Objects;

/**
 * Representation of an item in the item tree.
 *
 * @author Jimeo Wan
 */
public class InvTweaksItemTreeItem implements IItemTreeItem {

    private String name;
    
    private String id;
    private int damage;
    private CompoundTag extraData;
    private int order;
    private String path;

    /**
     * @param name_   The item name
     * @param id_     The item ID
     * @param damage_ The item variant or InvTweaksConst.DAMAGE_WILDCARD
     * @param order_  The item order while sorting
     */
    public InvTweaksItemTreeItem(String name_, String id_, int damage_, CompoundTag extraData_, int order_, String path_) {
        name = name_;
        if(id_ == null) {
            id =  null;
        } else if(id_.indexOf(':') == -1) {
        	id = "minecraft:" + id_;
        } else {
        	id = id_;
        }        
        damage = damage_;
        extraData = extraData_;
        order = order_;
        path = path_;
    }

    @Override
    public String getName() {
        return name;
    }

    
    @Override
    public String getId() {
        return id;
    }

    @Override
    public int getDamage() {
        return damage;
    }

    @Override
    public CompoundTag getExtraData() {
        return extraData;
    }

    @Override
    public int getOrder() {
        return order;
    }
    
    @Override
    public String getPath() {
        return path;
    }

    /**
     * Warning: the item equality is not reflective. They are equal if "o" matches the item constraints (the opposite
     * can be false).
     */
    public boolean equals( Object o) {
        if(o == null || !(o instanceof IItemTreeItem)) {
            return false;
        }
         IItemTreeItem item = (IItemTreeItem) o;
        return Objects.equals(id, item.getId())
                && NbtUtils.compareNbt(extraData, item.getExtraData(), true);
    }

    public String toString() {
        return name;
    }

    @Override
    public int compareTo( IItemTreeItem item) {
        return item.getOrder() - order;
    }

}
