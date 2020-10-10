package ru.quarter.zc.capability;

import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import org.apache.logging.log4j.util.Strings;

public class Inventory extends InventoryBasic {

    public Inventory() {
        this(0);
    }

    public Inventory(int slotCount) {
        this("", slotCount);
    }

    public Inventory(String title, int slotCount) {
        super(title, !Strings.isEmpty(title), slotCount);
    }

    public void resize(int size) {
        NonNullList<ItemStack> newList = NonNullList.withSize(size, ItemStack.EMPTY);
        size = Math.min(size, getSizeInventory());
        for (int i = 0; i < size; i++) {
            newList.set(i, getStackInSlot(i));
        }
        this.inventoryContents = newList;
    }
}
