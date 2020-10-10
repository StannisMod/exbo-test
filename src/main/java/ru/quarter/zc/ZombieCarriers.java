package ru.quarter.zc;

import net.minecraft.inventory.IInventory;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import ru.quarter.zc.capability.CapabilityInventory;
import ru.quarter.zc.capability.Inventory;

@Mod(modid = ZombieCarriers.MODID, name = ZombieCarriers.MODNAME, version = ZombieCarriers.VERSION)
public class ZombieCarriers {

    public static final String MODID = "zc";
    public static final String MODNAME = "ZombieCarriers";
    public static final String VERSION = "1.0";

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {

    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        CapabilityManager.INSTANCE.register(IInventory.class, new CapabilityInventory.Storage(), Inventory::new);
        MinecraftForge.EVENT_BUS.register(new EventHandler());
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {

    }
}
