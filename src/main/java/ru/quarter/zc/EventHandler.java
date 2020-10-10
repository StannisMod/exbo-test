package ru.quarter.zc;

import net.minecraft.entity.Entity;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import ru.quarter.zc.capability.CapabilityInventory;

@Mod.EventBusSubscriber(modid = ZombieCarriers.MODID)
public class EventHandler {

    public static final ResourceLocation INVENTORY = new ResourceLocation(ZombieCarriers.MODID, "capability/inventory");

    @SubscribeEvent
    public void onCapabilityInject(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof EntityZombie) {
            event.addCapability(INVENTORY, new CapabilityInventory(9));
            //System.out.println("Injected!");
        }
    }
}
