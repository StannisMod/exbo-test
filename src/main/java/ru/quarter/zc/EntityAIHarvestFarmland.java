package ru.quarter.zc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.Village;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.World;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class EntityAIHarvestFarmland extends EntityAIMoveToBlock {

    enum Task {
        NONE,
        HARVEST,
        THROW,
        TO_FARM,
        TO_DOOR
    }

    /** Villager that is harvesting */
    private final EntityCreature entity;
    private Task currentTask = Task.NONE;
    private List<BlockPos> doors;
    private Iterator<BlockPos> doorIterator;

    public EntityAIHarvestFarmland(EntityCreature entityIn, double speedIn) {
        super(entityIn, speedIn, 32);
        this.entity = entityIn;
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        Village village = this.entity.world.getVillageCollection().getNearestVillage(new BlockPos(this.entity), 128);
        if (village == null) {
            return false;
        }

        if (this.runDelay <= 0)
        {
            if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.entity.world, this.entity)) {
                return false;
            }

            this.currentTask = Task.TO_FARM;
            creature.setCustomNameTag("TO_FARM");
            creature.setAlwaysRenderNameTag(true);
            creature.setCanPickUpLoot(true);

        }

        if (doors == null) {
            this.doors = village.getVillageDoorInfoList()
                    .stream().map(VillageDoorInfo::getDoorBlockPos).collect(Collectors.toList());
            this.doorIterator = doors.iterator();
        }

        return super.shouldExecute();
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean shouldContinueExecuting() {
        return this.currentTask.ordinal() > 0 && super.shouldContinueExecuting();
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void updateTask()
    {
        super.updateTask();
        this.entity.getLookHelper().setLookPosition((double)this.destinationBlock.getX() + 0.5D, (double)(this.destinationBlock.getY() + 1), (double)this.destinationBlock.getZ() + 0.5D, 10.0F, (float)this.entity.getVerticalFaceSpeed());

        if (this.creature.getDistanceSqToCenter(this.destinationBlock) < 1.0) {
            System.out.println("In action area");
            if (this.currentTask.ordinal() <= 2) {
                World world = this.entity.world;
                BlockPos blockpos = this.destinationBlock.up();
                IBlockState iblockstate = world.getBlockState(blockpos);
                Block block = iblockstate.getBlock();

                this.runDelay = 10;

                if (this.currentTask == Task.HARVEST && block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(iblockstate)) {
                    world.destroyBlock(blockpos, true);
                    this.searchForDestination();
                    return;
                } else if (this.currentTask == Task.THROW) {
                    if (entity.getHeldItemMainhand().isEmpty()) {
                        this.currentTask = Task.TO_FARM;
                        creature.setCustomNameTag("TO_FARM");
                        return;
                    }
                    entity.dropItem(entity.getHeldItemMainhand().getItem(), 1);
                    entity.getHeldItemMainhand().shrink(1);
                    this.currentTask = Task.TO_DOOR;
                    creature.setCustomNameTag("TO_DOOR");
                    return;
                }

                if (this.currentTask == Task.HARVEST) {
                    this.currentTask = Task.TO_DOOR;
                    creature.setCustomNameTag("TO_DOOR");
                } else {
                    this.currentTask = Task.TO_FARM;
                    creature.setCustomNameTag("TO_FARM");
                }
            }

            if (this.currentTask == Task.TO_DOOR) {
                this.currentTask = Task.THROW;
                creature.setCustomNameTag("THROW");
            }

            if (this.currentTask == Task.TO_FARM) {
                this.currentTask = Task.HARVEST;
                creature.setCustomNameTag("HARVEST");
            }
        }
    }

    /**
     * Return true to set given position as destination
     */
    protected boolean shouldMoveTo(World worldIn, @Nonnull BlockPos pos) {
        Block block = worldIn.getBlockState(pos).getBlock();

        if (block instanceof BlockDoor) {
            return this.currentTask == Task.TO_DOOR;
        }

        if (block == Blocks.FARMLAND) {
            pos = pos.up();
            IBlockState iblockstate = worldIn.getBlockState(pos);
            block = iblockstate.getBlock();

            return block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(iblockstate)
                    && (this.currentTask == Task.TO_FARM || this.currentTask == Task.HARVEST);
        }

        return false;
    }

    @Override
    public boolean searchForDestination() {
        BlockPos blockpos = new BlockPos(this.creature);

        if (this.currentTask == Task.HARVEST) {
            for (int i = -1; i < 2; i++) {
                for (int j = -1; j < 2; j++) {
                    if (i == 0 && j == 0) {
                        continue;
                    }
                    BlockPos blockpos1 = blockpos.add(i, 0, j);
                    if (this.creature.isWithinHomeDistanceFromPosition(blockpos1) && this.shouldMoveTo(this.creature.world, blockpos1)) {
                        this.destinationBlock = blockpos1;
                        return true;
                    }
                }
            }
        }

        if (this.currentTask == Task.TO_DOOR) {
            if (!doorIterator.hasNext()) {
                doorIterator = doors.iterator();
            }
            this.destinationBlock = doorIterator.next();
            return true;
        }

        for (int k = 0; k <= 1; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < this.searchLength; ++l) {
                for (int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for (int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        BlockPos blockpos1 = blockpos.add(i1, k - 1, j1);
                        if (this.creature.isWithinHomeDistanceFromPosition(blockpos1) && this.shouldMoveTo(this.creature.world, blockpos1)) {
                            this.destinationBlock = blockpos1;
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
