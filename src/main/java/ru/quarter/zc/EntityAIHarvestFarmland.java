package ru.quarter.zc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigateGround;
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

    private final EntityCreature entity;
    private Task currentTask = Task.TO_FARM;
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

            //this.setCurrentTask(Task.TO_FARM);
            creature.setAlwaysRenderNameTag(true);
            creature.setCanPickUpLoot(true);
            ((PathNavigateGround) creature.getNavigator()).setEnterDoors(true);

        }

        if (doors == null) {
            this.doors = village.getVillageDoorInfoList()
                    .stream().map(VillageDoorInfo::getDoorBlockPos).collect(Collectors.toList());
            this.doorIterator = doors.iterator();
        }

        //creature.setCustomNameTag(this.currentTask.name() + "[" + runDelay + "]");

        if (this.runDelay > 0)
        {
            --this.runDelay;
            return false;
        }
        else
        {
            this.runDelay = 20 + this.creature.getRNG().nextInt(20);
            return this.searchForDestination();
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean shouldContinueExecuting() {
        return this.timeoutCounter >= -this.maxStayTicks && this.timeoutCounter <= 1200 && this.shouldMoveTo(this.creature.world, this.destinationBlock);
    }

    private void setCurrentTask(Task task) {
        this.currentTask = task;
        creature.setCustomNameTag(task.name());
    }

    private void setTarget(BlockPos pos) {
        destinationBlock = pos;
        //creature.getNavigator().tryMoveToXYZ(pos.getX(), pos.getY(), pos.getZ(), movementSpeed);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void updateTask() {
        if (!creature.world.isRemote) {
            super.updateTask();
            this.entity.getLookHelper().setLookPosition((double) this.destinationBlock.getX() + 0.5D, (double) (this.destinationBlock.getY() + 1), (double) this.destinationBlock.getZ() + 0.5D, 10.0F, (float) this.entity.getVerticalFaceSpeed());

            if (this.creature.getDistanceSqToCenter(this.destinationBlock.up()) <= 4.0D) {
                System.out.println("In action area");
                if (this.currentTask.ordinal() <= 2) {
                    World world = this.entity.world;
                    BlockPos blockpos = this.destinationBlock.up();
                    IBlockState iblockstate = world.getBlockState(blockpos);
                    Block block = iblockstate.getBlock();

                    this.runDelay = 10;

                    if (this.currentTask == Task.HARVEST && block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(iblockstate)) {
                        world.destroyBlock(blockpos, true);
                        if (searchForDestination()) {
                            return;
                        }
                    } else if (this.currentTask == Task.THROW) {
                        if (entity.getHeldItemMainhand().isEmpty()) {
                            this.setCurrentTask(Task.TO_FARM);
                            return;
                        }
                        entity.entityDropItem(new ItemStack(entity.getHeldItemMainhand().getItem()), 0.5F);
                        entity.getHeldItemMainhand().shrink(1);
                        this.setCurrentTask(Task.TO_DOOR);
                        nextDoor();
                        return;
                    }

                    if (this.currentTask == Task.HARVEST) {
                        this.setCurrentTask(Task.TO_DOOR);
                    } else {
                        this.setCurrentTask(Task.TO_FARM);
                    }
                }

                System.out.println("1");

                if (this.currentTask == Task.TO_DOOR) {
                    System.out.println("2");
                    ((BlockDoor) creature.world.getBlockState(destinationBlock.up()).getBlock()).toggleDoor(creature.world, destinationBlock.up(), true);

                    if (entity.getHeldItemMainhand().isEmpty()) {
                        this.setCurrentTask(Task.TO_FARM);
                        return;
                    }
                    entity.entityDropItem(new ItemStack(entity.getHeldItemMainhand().getItem()), 0.5F);
                    entity.getHeldItemMainhand().shrink(1);

                    this.setCurrentTask(Task.THROW);
                }

                if (this.currentTask == Task.TO_FARM) {
                    this.setCurrentTask(Task.HARVEST);
                }
            } else if (this.currentTask == Task.TO_DOOR) {
                //System.out.println(destinationBlock.toString() + " | " + new BlockPos(creature).toString());
                //setTarget(destinationBlock);
                //creature.getMoveHelper().setMoveTo(destinationBlock.getX(), destinationBlock.getY(), destinationBlock.getZ(), 1.0D);
            }
        }
    }

    private void nextDoor() {
        if (!doorIterator.hasNext()) {
            doorIterator = doors.iterator();
        }
        this.destinationBlock = doorIterator.next();
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
                        setTarget(blockpos1);
                        return true;
                    }
                }
            }
            this.setCurrentTask(Task.TO_DOOR);
        }

        if (this.currentTask == Task.TO_DOOR) {
            nextDoor();
            return true;
        }

        // Direct copy from EntityAIMoveToBlock
        for (int k = 0; k <= 1; k = k > 0 ? -k : 1 - k) {
            for (int l = 0; l < this.searchLength; ++l) {
                for (int i1 = 0; i1 <= l; i1 = i1 > 0 ? -i1 : 1 - i1) {
                    for (int j1 = i1 < l && i1 > -l ? l : 0; j1 <= l; j1 = j1 > 0 ? -j1 : 1 - j1) {
                        BlockPos blockpos1 = blockpos.add(i1, k - 1, j1);
                        if (this.creature.isWithinHomeDistanceFromPosition(blockpos1) && this.shouldMoveTo(this.creature.world, blockpos1)) {
                            setTarget(blockpos1);
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }
}
