package ru.quarter.zc;

import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntityZombie;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import ru.quarter.zc.repack.gloomyfolken.hooklib.asm.Hook;
import ru.quarter.zc.repack.gloomyfolken.hooklib.asm.ReturnCondition;

public class Hooks {

    @Hook(injectOnExit = true)
    public static void initEntityAI(EntityZombie instance) {
        instance.tasks.taskEntries.removeIf(task -> task.action instanceof EntityAIZombieAttack);
        instance.targetTasks.taskEntries.removeIf(task -> task.action instanceof EntityAINearestAttackableTarget);
        instance.tasks.addTask(3, new EntityAIOpenDoor(instance, false));
        instance.tasks.addTask(2, new EntityAIHarvestFarmland(instance, 1.0F));
        instance.setCanPickUpLoot(true);
    }

    @Hook(returnCondition = ReturnCondition.ALWAYS)
    public static boolean shouldBurnInDay(EntityZombie instance) {
        return false;
    }

    @Hook(createMethod = true)
    public static void updateEquipmentIfNeeded(EntityZombie zombie, EntityItem itemEntity) {
        ItemStack stack1 = zombie.getHeldItemMainhand();
        ItemStack stack2 = itemEntity.getItem();
        if (stack1.isEmpty()) {
            zombie.setHeldItem(EnumHand.MAIN_HAND, stack2.copy());
            return;
        }
        if (zombie.canPickUpLoot() && ItemStack.areItemsEqual(stack1, stack2) && ItemStack.areItemStackTagsEqual(stack1, stack2)) {
            int growQuantity = Math.min(64 - stack1.getCount(), stack2.getCount());
            stack1.grow(growQuantity);
            stack2.shrink(growQuantity);
        }
    }
}
