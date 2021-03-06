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
package com.lothrazar.cyclicmagic.item.cyclicwand;

import java.util.List;
import org.lwjgl.input.Keyboard;
import com.lothrazar.cyclicmagic.IContent;
import com.lothrazar.cyclicmagic.ModCyclic;
import com.lothrazar.cyclicmagic.data.IHasRecipe;
import com.lothrazar.cyclicmagic.event.EventRender;
import com.lothrazar.cyclicmagic.event.EventRender.RenderLoc;
import com.lothrazar.cyclicmagic.registry.ItemRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry;
import com.lothrazar.cyclicmagic.registry.LootTableRegistry.ChestType;
import com.lothrazar.cyclicmagic.registry.RecipeRegistry;
import com.lothrazar.cyclicmagic.registry.SpellRegistry;
import com.lothrazar.cyclicmagic.spell.BaseSpellRange;
import com.lothrazar.cyclicmagic.spell.ISpell;
import com.lothrazar.cyclicmagic.util.Const;
import com.lothrazar.cyclicmagic.util.UtilChat;
import com.lothrazar.cyclicmagic.util.UtilNBT;
import com.lothrazar.cyclicmagic.util.UtilSpellCaster;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemCyclicWand extends Item implements IHasRecipe, IContent {

  private static final String NBT_SPELLCURRENT = "spell_id";
  private List<ISpell> spellbook;

  public ItemCyclicWand() {
    this.setMaxStackSize(1);
    this.setFull3D();
    this.setContainerItem(this);
  }

  public void setSpells(List<ISpell> spells) {
    this.spellbook = spells;
  }

  public List<ISpell> getSpells() {
    return this.spellbook;
  }

  @Override
  public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
    if (!slotChanged) {
      return false;// only item data has changed, so do notanimate
    }
    return super.shouldCauseReequipAnimation(oldStack, newStack, slotChanged);
  }

  @Override
  public void onCreated(ItemStack stack, World worldIn, EntityPlayer playerIn) {
    //Energy.rechargeBy(stack, Energy.START);
    Spells.setSpellCurrent(stack, SpellRegistry.getSpellbook(stack).get(0).getID());
    super.onCreated(stack, worldIn, playerIn);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public void addInformation(ItemStack stack, World playerIn, List<String> tooltip, net.minecraft.client.util.ITooltipFlag advanced) {
    ISpell spell = SpellRegistry.getSpellFromID(Spells.getSpellIDCurrent(stack));
    if (Keyboard.isCreated() && (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT))) {
      tooltip.add(TextFormatting.GREEN + spell.getName() + " "
          + "[" + UtilChat.lang(BuildType.getName(stack)) + "] ");
      tooltip.add(TextFormatting.DARK_GRAY + UtilChat.lang("item.cyclic_wand.tooltiprange") + BaseSpellRange.maxRange);
      tooltip.add(TextFormatting.DARK_GRAY + UtilChat.lang("item.cyclic_wand.shifting"));
    }
    else {
      tooltip.add(TextFormatting.DARK_GRAY + UtilChat.lang("item.shift"));
    }
    super.addInformation(stack, playerIn, tooltip, advanced);
  }

  @Override
  @SideOnly(Side.CLIENT)
  public EnumRarity getRarity(ItemStack par1ItemStack) {
    return EnumRarity.UNCOMMON;
  }

  @Override
  public EnumActionResult onItemUse(EntityPlayer playerIn, World worldIn, BlockPos pos,
      EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
    ItemStack stack = playerIn.getHeldItem(hand);
    // If onItemUse returns false onItemRightClick will be called.
    // http://www.minecraftforge.net/forum/index.php?topic=31966.0
    // so if this casts and succeeds, the right click is cancelled
    boolean success = UtilSpellCaster.tryCastCurrent(worldIn, playerIn, pos, side, stack, hand);
    return success ? EnumActionResult.SUCCESS : EnumActionResult.FAIL;
  }

  @Override
  public ActionResult<ItemStack> onItemRightClick(World worldIn, EntityPlayer playerIn, EnumHand hand) {
    ItemStack itemStackIn = playerIn.getHeldItem(hand);
    // so this only happens IF either onItemUse did not fire at all, or it
    // fired and casting failed
    //    boolean success = 
    UtilSpellCaster.tryCastCurrent(worldIn, playerIn, null, null, itemStackIn, hand);
    //    return success ? new ActionResult<ItemStack>(EnumActionResult.SUCCESS, itemStackIn)
    return new ActionResult<ItemStack>(EnumActionResult.FAIL, itemStackIn);
  }

  @Override
  public int getMaxItemUseDuration(ItemStack stack) {
    return 1; // Without this method, your inventory will NOT work!!!
  }

  public static class Spells {

    public static int getSpellIDCurrent(ItemStack stack) {
      // workaround for default spell being replace. and oncrafting not
      if (UtilNBT.getItemStackNBT(stack).hasKey(NBT_SPELLCURRENT) == false) {
        if (SpellRegistry.getSpellbook(stack).size() == 0) {
          return 0;
        }
        // what is default spell for that then?
        return SpellRegistry.getSpellbook(stack).get(0).getID();
      }
      int c = UtilNBT.getItemStackNBT(stack).getInteger(NBT_SPELLCURRENT);
      return c;
    }

    public static ISpell getSpellCurrent(ItemStack stack) {
      int idCurrent = getSpellIDCurrent(stack);
      ISpell s = SpellRegistry.getSpellFromID(idCurrent);
      return s;
    }

    public static void setSpellCurrent(ItemStack stack, int spell_id) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(stack);
      tags.setInteger(NBT_SPELLCURRENT, spell_id);
      stack.setTagCompound(tags);
    }
  }

  public enum BuildType {
    FIRST, ROTATE, RANDOM;

    private final static String NBT = "build";
    private final static String NBT_SLOT = "buildslot";
    private final static String NBT_SIZE = "buildsize";

    public static String getName(ItemStack wand) {
      try {
        NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
        return "button.build." + BuildType.values()[tags.getInteger(NBT)].toString().toLowerCase();
      }
      catch (Exception e) {
        return "button.build." + FIRST.toString().toLowerCase();
      }
    }

    public static int get(ItemStack wand) {
      if (wand.isEmpty()) {
        return 0;
      }
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      return tags.getInteger(NBT);
    }

    public static void toggle(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      int type = tags.getInteger(NBT);
      type++;
      if (type > RANDOM.ordinal()) {
        type = FIRST.ordinal();
      }
      tags.setInteger(NBT, type);
      wand.setTagCompound(tags);
      int slot = getSlot(wand);
      if (InventoryWand.getFromSlot(wand, slot).isEmpty() || InventoryWand.getToPlaceFromSlot(wand, slot) == null) {
        //try to move away from empty slot
        setNextSlot(wand);
      }
    }

    public static int getBuildSize(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      int s = tags.getInteger(NBT_SIZE);
      return s;
    }

    public static void setBuildSize(ItemStack wand, int size) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      tags.setInteger(NBT_SIZE, size);
      wand.setTagCompound(tags);
    }

    public static int getSlot(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      if (!tags.hasKey(NBT_SLOT)) {
        resetSlot(wand);
        return 0;
      }
      return tags.getInteger(NBT_SLOT);
    }

    public static void resetSlot(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      tags.setInteger(NBT_SLOT, 0);
    }

    public static void setNextSlot(ItemStack wand) {
      NBTTagCompound tags = UtilNBT.getItemStackNBT(wand);
      int prev = getSlot(wand);
      int next = InventoryWand.calculateSlotCurrent(wand);
      if (prev != next)
        tags.setInteger(NBT_SLOT, next);
    }
  }

  @Override
  public void register() {
    ItemRegistry.register(this, "cyclic_wand_build");
    SpellRegistry.register(this);
    ModCyclic.instance.events.register(this);
    LootTableRegistry.registerLoot(this, ChestType.ENDCITY, 15);
    LootTableRegistry.registerLoot(this, ChestType.GENERIC, 1);
    //      AchievementRegistry.registerItemAchievement(cyclic_wand_build);
    ModCyclic.TAB.setTabItemIfNull(this);
  }

  private boolean enabled;

  @Override
  public boolean enabled() {
    return enabled;
  }

  @Override
  public void syncConfig(Configuration config) {
    enabled = config.getBoolean("CyclicWand", Const.ConfigCategory.content, true, Const.ConfigCategory.contentDefaultText);
    String category = Const.ConfigCategory.items;
    //TODO: no good place to put this eh
    String renderLocation = config.getString("Scepter HUD", Const.ConfigCategory.items, RenderLoc.BOTTOMLEFT.toString().toLowerCase(), "Location of scepter Hud [topleft, topright, bottomleft, bottomright].  Used by both Exchange Scepters and Cyclic Build Scepter.  ");
    //fff...yeah probs better way to do this, like a loop.
    if (RenderLoc.TOPLEFT.name().toLowerCase().equals(renderLocation)) {
      EventRender.renderLocation = RenderLoc.TOPLEFT;
    }
    else if (RenderLoc.TOPRIGHT.name().toLowerCase().equals(renderLocation)) {
      EventRender.renderLocation = RenderLoc.TOPRIGHT;
    }
    else if (RenderLoc.BOTTOMLEFT.name().toLowerCase().equals(renderLocation)) {
      EventRender.renderLocation = RenderLoc.BOTTOMLEFT;
    }
    else if (RenderLoc.BOTTOMRIGHT.name().toLowerCase().equals(renderLocation)) {
      EventRender.renderLocation = RenderLoc.BOTTOMRIGHT;
    }
    else {
      EventRender.renderLocation = RenderLoc.BOTTOMLEFT;
    }
    SpellRegistry.doParticles = config.getBoolean("Build Scepter Particles", category, false, "Cyclic Scepter: Set to false to disable particles");
    category = Const.ConfigCategory.modpackMisc;
    BaseSpellRange.maxRange = config.getInt("Build Scepter Max Range", category, 64, 8, 128, "Cyclic Scepter: Maximum range for all spells");
  }

  @Override
  public IRecipe addRecipe() {
    return RecipeRegistry.addShapedRecipe(new ItemStack(this),
        "sds",
        " o ",
        "gog",
        'd', "gemDiamond",
        'g', "gemQuartz",
        'o', "obsidian",
        's', Blocks.BONE_BLOCK);
  }
}
