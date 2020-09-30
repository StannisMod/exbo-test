package ru.quarter.zc;

import net.minecraft.entity.ai.EntityAINearestAttackableTarget;
import net.minecraft.entity.ai.EntityAIZombieAttack;
import net.minecraft.entity.monster.EntityZombie;
import ru.quarter.zc.repack.gloomyfolken.hooklib.asm.Hook;

public class Hooks {

    @Hook(injectOnExit = true)
    public static void initEntityAI(EntityZombie instance) {
        instance.tasks.taskEntries.removeIf(task -> task.action instanceof EntityAIZombieAttack);
        instance.targetTasks.taskEntries.removeIf(task -> task.action instanceof EntityAINearestAttackableTarget);
        instance.tasks.addTask(10, new EntityAIHarvestFarmland(instance, 1.0F));
    }

    @Hook
    public static boolean shouldBurnInDay(EntityZombie instance) {
        return false;
    }
}
