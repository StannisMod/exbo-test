package ru.quarter.zc;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityCreature;
import net.minecraft.entity.ai.EntityAIMoveToBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.village.Village;
import net.minecraft.village.VillageDoorInfo;
import net.minecraft.world.World;
import ru.quarter.zc.capability.CapabilityInventory;

import javax.annotation.Nonnull;
import java.util.*;
import java.util.stream.Collectors;

public class EntityAIHarvestFarmland extends EntityAIMoveToBlock {

    enum Task {
        HARVEST,
        TO_FARM,
        TO_DOOR
    }

    private Task currentTask;
    private List<BlockPos> doors;
    private Iterator<BlockPos> doorIterator;
    private Iterator<BlockPos> farmland;//new TreeSet<>(POS_EQUALS_COMPARATOR);

    //public static final Comparator<BlockPos> POS_EQUALS_COMPARATOR = (o1, o2) -> o1.distanceSq(o2) == 0 ? 0 : 1;

    public EntityAIHarvestFarmland(EntityCreature entityIn, double speedIn) {
        super(entityIn, speedIn, 32);
        setCurrentTask(Task.TO_FARM);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute() {
        Village village = this.creature.world.getVillageCollection().getNearestVillage(new BlockPos(this.creature), 32);
        if (village == null) {
            return false;
        }

        if (this.runDelay <= 0) {
            if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.creature.world, this.creature)) {
                return false;
            }

            //this.setCurrentTask(Task.TO_FARM);
            creature.setAlwaysRenderNameTag(true);
            creature.setCanPickUpLoot(true);
            ((PathNavigateGround) creature.getNavigator()).setEnterDoors(true);
            ((PathNavigateGround) creature.getNavigator()).setBreakDoors(true);
        }

        if (doors == null) {
            this.doors = village.getVillageDoorInfoList()
                    .stream()
                    .map(VillageDoorInfo::getInsideBlockPos)
                    .collect(Collectors.toList());
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
            boolean found = this.searchForDestination();
            /*if (found) {
                System.out.println("Found target | State: " + currentTask.name() + " | Pos: " + destinationBlock.toString() + " | Target: " + creature.world.getBlockState(destinationBlock).getBlock().toString() + " | Up: " + creature.world.getBlockState(destinationBlock.up()).getBlock().toString());
            }*/
            if (!shouldContinueExecuting()) {
                return false;
            }
            return found;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    @Override
    public boolean shouldContinueExecuting() {
        return this.shouldMoveTo(this.creature.world, this.destinationBlock);
    }

    @Override
    public void startExecuting() {
        this.creature.getNavigator().tryMoveToXYZ((double)((float)this.destinationBlock.getX()) + 0.5D, (double)(this.destinationBlock.getY() + 1), (double)((float)this.destinationBlock.getZ()) + 0.5D, this.movementSpeed);
        this.timeoutCounter = 0;
        this.maxStayTicks = 0;
    }

    private void setCurrentTask(Task task) {
        this.currentTask = task;
        creature.setCustomNameTag(task.name());
    }

    private void setTarget(BlockPos pos) {
        destinationBlock = pos;
    }

    private double getDistanceToDestination() {
        return destinationBlock.getDistance((int) creature.posX, (int) creature.posY, (int) creature.posZ);
    }

    /**
     * Keep ticking a continuous task that has already been started
     */
    public void updateTask() {

            if (this.creature.getDistanceSqToCenter(this.destinationBlock.up()) > 1.0D) {
                this.isAboveDestination = false;

                if (creature.getNavigator().noPath()) {
                    System.out.println("Trying to move... | State: " + currentTask.name() + " | Pos: " + destinationBlock.toString() + " | Target: " + creature.world.getBlockState(destinationBlock).getBlock().toString() + " | Up: " + creature.world.getBlockState(destinationBlock.up()).getBlock().toString());
                    this.creature.getNavigator().tryMoveToXYZ((double)((float)this.destinationBlock.getX()) + 0.5D, (double)(this.destinationBlock.getY() + 1), (double)((float)this.destinationBlock.getZ()) + 0.5D, this.movementSpeed);
                }
            }
            else {
                this.isAboveDestination = true;
            }

            this.creature.getLookHelper().setLookPosition((double) this.destinationBlock.getX() + 0.5D, (double) (this.destinationBlock.getY() + 1), (double) this.destinationBlock.getZ() + 0.5D, 10.0F, (float) this.creature.getVerticalFaceSpeed());

            if (getDistanceToDestination() < 1.2F && this.currentTask == Task.TO_DOOR) {
                System.out.println("Throwing...");

                throwSingleItem();

                if (InventoryUtil.isEmpty(CapabilityInventory.get(creature))) {
                    this.setCurrentTask(Task.TO_FARM);
                    return;
                }

                nextDoor();
            }

            if (this.getIsAboveDestination()) {
                //System.out.println("In action area");
                if (this.currentTask == Task.HARVEST) {
                    World world = this.creature.world;
                    BlockPos up = this.destinationBlock.up();
                    IBlockState iblockstate = world.getBlockState(up);
                    Block block = iblockstate.getBlock();

                    if (block instanceof BlockCrops && ((BlockCrops) block).isMaxAge(iblockstate)) {
                        world.destroyBlock(up, true);
                        if (searchForDestination()) {
                            return;
                        }
                    }
                }

                if (this.currentTask == Task.TO_FARM) {
                    // Scanning for the whole farmland
                    Queue<BlockPos> queue = new ArrayDeque<>();
                    Set<BlockPos> visited = new HashSet<>();//new TreeSet<>(POS_EQUALS_COMPARATOR);
                    Set<BlockPos> result = new HashSet<>();
                    queue.add(destinationBlock);
                    System.out.println("=================== Started seeking ===================");
                    System.out.println(destinationBlock.toString());
                    while (!queue.isEmpty()) {
                        BlockPos target = queue.poll();
                        if (this.shouldMoveTo(this.creature.world, target)) {
                            result.add(target);
                            System.out.println("Added to farmland: " + target.toString());
                        }
                        for (int i = -1; i < 2; i++) {
                            for (int j = -1; j < 2; j++) {
                                if (i == 0 && j == 0) {
                                    continue;
                                }
                                BlockPos blockpos1 = target.add(i, 0, j);
                                if (this.creature.isWithinHomeDistanceFromPosition(blockpos1) && creature.world.getBlockState(blockpos1).getBlock() instanceof BlockFarmland) {//this.shouldMoveTo(this.creature.world, blockpos1)) {
                                    if (!visited.contains(blockpos1)) {
                                        queue.add(blockpos1);
                                        visited.add(blockpos1);
                                        System.out.println("Added to queue: " + blockpos1.toString());
                                    }
                                }
                            }
                        }
                    }
                    farmland = result.iterator();
                    System.out.println("Found: " + result.size() + " farmland blocks");
                    System.out.println("==================== Ended seeking ====================");
                    this.setCurrentTask(Task.HARVEST);
                }
            }
    }

    private void throwSingleItem() {
        double d0 = creature.posY - 0.30000001192092896D + (double)creature.getEyeHeight();

        ItemStack item = InventoryUtil.consumeOneItem(CapabilityInventory.get(creature));

        EntityItem entityitem = new EntityItem(creature.world, creature.posX, d0, creature.posZ, item);
        entityitem.setPickupDelay(200);
        float f2 = 0.3F;
        entityitem.motionX = (double)(-MathHelper.sin(creature.rotationYaw * 0.017453292F) * MathHelper.cos(creature.rotationPitch * 0.017453292F) * f2);
        entityitem.motionZ = (double)(MathHelper.cos(creature.rotationYaw * 0.017453292F) * MathHelper.cos(creature.rotationPitch * 0.017453292F) * f2);
        entityitem.motionY = (double)(-MathHelper.sin(creature.rotationPitch * 0.017453292F) * f2 + 0.1F);
        float f3 = creature.getRNG().nextFloat() * ((float)Math.PI * 2F);
        f2 = 0.02F * creature.getRNG().nextFloat();
        entityitem.motionX += Math.cos((double)f3) * (double)f2;
        entityitem.motionY += (double)((creature.getRNG().nextFloat() - creature.getRNG().nextFloat()) * 0.1F);
        entityitem.motionZ += Math.sin((double)f3) * (double)f2;

        creature.world.spawnEntity(entityitem);
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

        if (block instanceof BlockAir) {
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
            if (!farmland.hasNext()) {
                this.setCurrentTask(Task.TO_DOOR);
                nextDoor();
                return true;
            }
            destinationBlock = farmland.next();
            return true;
        }

        // If occasionally move to this, just skip
        if (this.currentTask == Task.TO_DOOR) {
            return true;
        }

        // Searching for WHAT??? (of course, for nearest farmlands)
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
