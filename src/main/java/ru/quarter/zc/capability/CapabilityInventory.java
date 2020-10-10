package ru.quarter.zc.capability;

import com.sun.istack.internal.Nullable;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;

import javax.annotation.Nonnull;

public class CapabilityInventory implements ICapabilitySerializable<NBTBase> {

    @CapabilityInject(IInventory.class)
    public static Capability<IInventory> CAPABILITY = null;

    private final IInventory instance;

    public CapabilityInventory(int size) {
        this("", size);
    }

    public CapabilityInventory(String title, int size) {
        this.instance = new Inventory(title, size);
    }

    public static IInventory get(ICapabilityProvider target) {
        return target.getCapability(CAPABILITY, null);
    }

    @Override
    public boolean hasCapability(@Nonnull Capability<?> capability, @Nullable EnumFacing facing) {
        return capability == CAPABILITY;
    }

    @Nullable
    @Override
    public <T> T getCapability(@Nonnull Capability<T> capability, @Nullable EnumFacing facing) {
        return hasCapability(capability, facing) ? CAPABILITY.<T> cast(this.instance) : null;
    }

    @Override
    public NBTBase serializeNBT() {
        return CAPABILITY.getStorage().writeNBT(CAPABILITY, this.instance, null);
    }

    @Override
    public void deserializeNBT(NBTBase nbt) {
        CAPABILITY.getStorage().readNBT(CAPABILITY, this.instance, null, nbt);
    }

    public static class Storage implements Capability.IStorage<IInventory> {

        @Override
        public NBTBase writeNBT(Capability<IInventory> capability, IInventory instance, EnumFacing side) {
            NBTTagList nbtTagList = new NBTTagList();
            int size = instance.getSizeInventory();
            for (int i = 0; i < size; i++) {
                ItemStack stack = instance.getStackInSlot(i);
                //if (!stack.isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                itemTag.setInteger("Slot", i);
                stack.writeToNBT(itemTag);
                nbtTagList.appendTag(itemTag);
                //}
            }
            return nbtTagList;
        }

        @Override
        public void readNBT(Capability<IInventory> capability, IInventory instance, EnumFacing side, NBTBase base) {
            NBTTagList tagList = (NBTTagList) base;
            /*if (tagList.tagCount() == 0) {
                return;
            }*/
            if (!(instance instanceof Inventory)) {
                throw new RuntimeException("Wrong capability implementation");
            }
            System.out.println("Read from NBT");
            Inventory inventory = (Inventory) instance;
            inventory.resize(tagList.tagCount());
            for (int i = 0; i < tagList.tagCount(); i++) {
                NBTTagCompound itemTags = tagList.getCompoundTagAt(i);
                int j = itemTags.getInteger("Slot");

                if (j >= 0 && j < instance.getSizeInventory()) {
                    inventory.setInventorySlotContents(j, new ItemStack(itemTags));
                }
            }
        }
    }
}
