package ru.quarter.zc;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ReportedException;

public class InventoryUtil {

    public static boolean addItemStackToInventory(IInventory inventory, ItemStack itemStackIn) {
        return add(inventory,-1, itemStackIn);
    }

    public static boolean add(IInventory inventory, int slot, final ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        } else {
            try {
                if (stack.isItemDamaged()) {
                    if (slot == -1) {
                        slot = getFirstEmptyStack(inventory);
                    }

                    if (slot >= 0) {
                        inventory.setInventorySlotContents(slot, stack.copy());
                        inventory.getStackInSlot(slot).setAnimationsToGo(5);
                        stack.setCount(0);
                        return true;
                    } else {
                        return false;
                    }
                } else {
                    int i;

                    while (true) {
                        i = stack.getCount();

                        if (slot == -1) {
                            stack.setCount(storePartialItemStack(inventory, stack));
                        } else {
                            stack.setCount(addResource(inventory, slot, stack));
                        }

                        if (stack.isEmpty() || stack.getCount() >= i) {
                            break;
                        }
                    }

                    return stack.getCount() < i;
                }
            } catch (Throwable throwable) {
                CrashReport crashreport = CrashReport.makeCrashReport(throwable, "Adding item to inventory");
                CrashReportCategory crashreportcategory = crashreport.makeCategory("Item being added");
                crashreportcategory.addCrashSection("Item ID", Integer.valueOf(Item.getIdFromItem(stack.getItem())));
                crashreportcategory.addCrashSection("Item data", Integer.valueOf(stack.getMetadata()));
                crashreportcategory.addDetail("Registry Name", () -> String.valueOf(stack.getItem().getRegistryName()));
                crashreportcategory.addDetail("Item Class", () -> stack.getItem().getClass().getName());
                crashreportcategory.addDetail("Item name", stack::getDisplayName);
                throw new ReportedException(crashreport);
            }
        }
    }

    /**
     * Returns the price item stack that is empty.
     */
    public static int getFirstEmptyStack(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }

        return -1;
    }

    /**
     * This function stores as many items of an ItemStack as possible in a matching slot and returns the quantity of
     * left over items.
     */
    private static int storePartialItemStack(IInventory inventory, ItemStack itemStackIn) {
        int i = storeItemStack(inventory, itemStackIn);

        if (i == -1) {
            i = getFirstEmptyStack(inventory);
        }

        return i == -1 ? itemStackIn.getCount() : addResource(inventory, i, itemStackIn);
    }

    /**
     * stores an itemstack in the users inventory
     */
    public static int storeItemStack(IInventory inventory, ItemStack itemStackIn) {
        for (int i = 0; i < inventory.getSizeInventory(); ++i) {
            if (canMergeStacks(inventory.getStackInSlot(i), itemStackIn, inventory.getInventoryStackLimit())) {
                return i;
            }
        }

        return -1;
    }

    private static boolean canMergeStacks(ItemStack stack1, ItemStack stack2, int limit) {
        return !stack1.isEmpty() && stackEqualExact(stack1, stack2) && stack1.isStackable() && stack1.getCount() < stack1.getMaxStackSize() && stack1.getCount() < limit;
    }

    /**
     * Checks item, NBT, and meta if the item is not damageable
     */
    private static boolean stackEqualExact(ItemStack stack1, ItemStack stack2) {
        return stack1.getItem() == stack2.getItem() && (!stack1.getHasSubtypes() || stack1.getMetadata() == stack2.getMetadata()) && ItemStack.areItemStackTagsEqual(stack1, stack2);
    }

    private static int addResource(IInventory inventory, int p_191973_1_, ItemStack p_191973_2_) {
        Item item = p_191973_2_.getItem();
        int i = p_191973_2_.getCount();
        ItemStack itemstack = inventory.getStackInSlot(p_191973_1_);

        if (itemstack.isEmpty()) {
            itemstack = p_191973_2_.copy(); // Forge: Replace Item clone above to preserve item capabilities when picking the item up.
            itemstack.setCount(0);

            if (p_191973_2_.hasTagCompound()) {
                itemstack.setTagCompound(p_191973_2_.getTagCompound().copy());
            }

            inventory.setInventorySlotContents(p_191973_1_, itemstack);
        }

        int j = i;

        if (i > itemstack.getMaxStackSize() - itemstack.getCount()) {
            j = itemstack.getMaxStackSize() - itemstack.getCount();
        }

        if (j > inventory.getInventoryStackLimit() - itemstack.getCount()) {
            j = inventory.getInventoryStackLimit() - itemstack.getCount();
        }

        if (j == 0) {
            return i;
        } else {
            i = i - j;
            itemstack.grow(j);
            itemstack.setAnimationsToGo(5);
            return i;
        }
    }

    public static ItemStack consumeOneItem(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                Item item = inventory.getStackInSlot(i).getItem();
                inventory.getStackInSlot(i).shrink(1);
                return new ItemStack(item);
            }
        }

        return ItemStack.EMPTY;
    }

    public static boolean isEmpty(IInventory inventory) {
        for (int i = 0; i < inventory.getSizeInventory(); i++) {
            if (!inventory.getStackInSlot(i).isEmpty()) {
                return false;
            }
        }
        return true;
    }
}
