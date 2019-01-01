/*******************************************************************************
 * The MIT License (MIT)
 * 
 * Copyright (C) 2014-2018 Sam Bassett (aka Lothrazar)
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 ******************************************************************************/
package com.lothrazar.cyclicmagic.item.equipbauble;

import com.lothrazar.cyclicmagic.IContent;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.data.IHasRecipeAndRepair;
import com.lothrazar.cyclicmagic.guide.GuideCategory;
import com.lothrazar.cyclicmagic.item.core.BaseCharm;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilEntity;
import com.lothrazar.cyclicmagic.util.UtilParticle;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;

import java.util.List;

public class ItemCharmRepel extends BaseCharm implements IHasRecipeAndRepair, IContent {

  private static final int durability = 4096;
  private static final int TICK_DELAY = 10;
  private static final double CHANCE_DAMAGE = 0.25;
  private static final ItemStack craftItem = new ItemStack(Blocks.END_ROD);

  private static int ticks = 0;
  private boolean canPush = true;

  public ItemCharmRepel() {
    super(durability);
  }

  @Override
  public void register() {
    ItemRegistry.register(this, "charm_repel", GuideCategory.ITEMBAUBLES);
    ModCyclic.instance.events.register(this);
    LootTableRegistry.registerLoot(this);
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("RepelCharm", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
  }

  @Override
  public void onTick(ItemStack stack, EntityPlayer player) {
    if (!this.canTick(stack)) {
      return;
    }
    World world = player.getEntityWorld();
    AxisAlignedBB playerBubble = player.getEntityBoundingBox().grow(3, 0,3);
    if (canPush) {
      canPush = false;
      List<Entity> nearby = world.getEntitiesWithinAABBExcludingEntity(player, playerBubble);
      Vec3d playerVec3d = player.getPositionVector();
      for (Entity entity : nearby) {
        if (entity instanceof EntityLiving) {
          DamageSource thornDamage = DamageSource.causeThornsDamage(entity);
          if (((EntityLiving) entity).hurtTime == 0 && player.canEntityBeSeen(entity)) {
            Vec3d entityVec3d = entity.getPositionVector();
            float angle = UtilEntity.yawDegreesBetweenPoints(playerVec3d.x,playerVec3d.y,playerVec3d.z, entityVec3d.x, entityVec3d.y, entityVec3d.z);
            entity.setVelocity(0,0,0);
            entity.addVelocity((double)(MathHelper.sin(angle * 0.017453292F)), 0.1D, (double)(MathHelper.cos(angle * 0.017453292F)));
            entity.attackEntityFrom(thornDamage, 0);
            UtilParticle.spawnParticle(world, EnumParticleTypes.DAMAGE_INDICATOR, entity.getPosition());
            if (world.rand.nextDouble() < CHANCE_DAMAGE) {
              stack.damageItem(1, player);
            }
          }
        }
      }
    }

    if (ticks > TICK_DELAY) {
      ticks = 0;
      canPush = true;

      BlockPos bp1 = new BlockPos(playerBubble.minX, playerBubble.minY + 1, playerBubble.minZ);
      BlockPos bp2 = new BlockPos(playerBubble.maxX, playerBubble.minY + 1, playerBubble.minZ);
      BlockPos bp3 = new BlockPos(playerBubble.minX, playerBubble.minY + 1, playerBubble.maxZ);
      BlockPos bp4 = new BlockPos(playerBubble.maxX, playerBubble.minY + 1, playerBubble.maxZ);
      UtilParticle.spawnParticleBeam(world, EnumParticleTypes.REDSTONE, bp1, bp2, 1);
      UtilParticle.spawnParticleBeam(world, EnumParticleTypes.REDSTONE, bp1, bp3, 1);
      UtilParticle.spawnParticleBeam(world, EnumParticleTypes.REDSTONE, bp2, bp4, 1);
      UtilParticle.spawnParticleBeam(world, EnumParticleTypes.REDSTONE, bp3, bp4, 1);

    }
    ticks++;
  }

  @Override
  public boolean getIsRepairable(ItemStack par1ItemStack, ItemStack par2ItemStack)
  {
    return par2ItemStack.getItem() == craftItem.getItem();
  }

  @Override
  public IRecipe addRecipeAndRepair() {
    return super.addRecipe(craftItem);
  }
}
