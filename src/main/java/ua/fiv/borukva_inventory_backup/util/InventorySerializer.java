package ua.fiv.borukva_inventory_backup.util;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import ua.fiv.borukva_inventory_backup.ModInit;
import net.minecraft.nbt.*;

public class InventorySerializer {

    public static CompoundTag deserializeInventory(String json) {
        CompoundTag inventoryTag = new CompoundTag();

        try{
            json = "{" + "Inventory: "+ json + "}";

            inventoryTag = TagParser.parseCompoundFully(json);

            return validateComponents(inventoryTag);
        } catch (CommandSyntaxException e){
            ModInit.LOGGER.error(e.getMessage());
        }

        return inventoryTag;
    }

    private static CompoundTag validateComponents(CompoundTag compound){
        ListTag oldList = compound.getList("Inventory").get();

        for(int i=0; i<oldList.size(); i++){
            CompoundTag elem = (CompoundTag)oldList.get(i);
            if(elem.contains("count") && elem.contains("id")){
                elem.putByte("Slot", (byte) i);
            }
            if(elem.getCompound("components").map(CompoundTag::isEmpty).orElse(false)){
                elem.remove("components");
            }

            oldList.set(i, elem);
        }

        compound.remove("Inventory");
        compound.put("Inventory", oldList);
        return compound;
    }


}
