package com.github.nhaeutilities.handler;

import java.util.ArrayList;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

import com.github.nhaeutilities.item.ItemBookTutorial;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.registry.GameRegistry;

public final class TutorialBookHandler {

    public static final String TAG_TUTORIAL = "nhaeutilities_tutorial";
    public static final String EXT_PROP_KEY = "NHAEUtilitiesTutorialBook";
    public static final String TUTORIAL_KEY = "nhaeutilities.tutorial";

    public static Item itemBookTutorial;

    private TutorialBookHandler() {}

    public static void init() {
        itemBookTutorial = new ItemBookTutorial().setUnlocalizedName("nhaeutilities.tutorial_book")
            .setTextureName("minecraft:book_written")
            .setMaxStackSize(1);
        GameRegistry.registerItem(itemBookTutorial, "tutorial_book");

        TutorialBookHandler handler = new TutorialBookHandler();
        FMLCommonHandler.instance()
            .bus()
            .register(handler);
        MinecraftForge.EVENT_BUS.register(handler);
    }

    @SubscribeEvent
    public void onEntityConstructing(EntityEvent.EntityConstructing event) {
        if (event.entity instanceof EntityPlayer && event.entity.getExtendedProperties(EXT_PROP_KEY) == null) {
            event.entity.registerExtendedProperties(EXT_PROP_KEY, new TutorialProp());
        }
    }

    @SubscribeEvent
    public void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        EntityPlayer player = event.player;
        if (player.getExtendedProperties(EXT_PROP_KEY) == null) {
            return;
        }
        TutorialProp prop = (TutorialProp) player.getExtendedProperties(EXT_PROP_KEY);
        if (prop.received) {
            return;
        }
        prop.received = true;

        ItemStack bookStack = createServerBook();
        bookStack.stackTagCompound.setString(TAG_TUTORIAL, "true");

        EntityItem entityItem = player.dropPlayerItemWithRandomChoice(bookStack, false);
        entityItem.delayBeforeCanPickup = 0;
        entityItem.func_145797_a(player.getCommandSenderName());
    }

    @SubscribeEvent
    public void onPlayerInteract(PlayerInteractEvent event) {
        EntityPlayer player = event.entityPlayer;
        if (player == null) return;
        ItemStack held = player.getHeldItem();
        if (held == null || !(held.getItem() instanceof ItemBookTutorial)) return;
        if (held.stackTagCompound == null || !held.stackTagCompound.hasKey(TAG_TUTORIAL)) return;

        event.setCanceled(true);
        if (event.world.isRemote) {
            player.displayGUIBook(createClientBook());
        }
    }

    private static ItemStack createServerBook() {
        return buildBook(itemBookTutorial, TUTORIAL_KEY, "NHAEUtilities", new String[0]);
    }

    private static ItemStack createClientBook() {
        int pageCount;
        try {
            pageCount = Integer.parseInt(StatCollector.translateToLocalFormatted(TUTORIAL_KEY + ".pages"));
        } catch (NumberFormatException e) {
            return createServerBook();
        }

        ArrayList<String> pages = new ArrayList<>();
        for (int i = 0; i < pageCount; i++) {
            String text = StatCollector.translateToLocalFormatted(TUTORIAL_KEY + ".pages." + i)
                .replace("\\n", "\n");
            pages.add(text);
        }

        return buildBook(Items.written_book, TUTORIAL_KEY, "NHAEUtilities", pages.toArray(new String[0]));
    }

    private static ItemStack buildBook(Item item, String titleKey, String author, String[] pages) {
        ItemStack stack = new ItemStack(item, 1);
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("title", titleKey);
        nbt.setString("author", author);

        NBTTagList pageList = new NBTTagList();
        for (int i = 0; i < pages.length; i++) {
            if (i >= 48) break;
            if (pages[i].length() < 256) {
                pageList.appendTag(new NBTTagString(pages[i]));
            }
        }
        nbt.setTag("pages", pageList);
        stack.setTagCompound(nbt);
        return stack;
    }

    public static class TutorialProp implements IExtendedEntityProperties {

        boolean received;

        @Override
        public void saveNBTData(NBTTagCompound compound) {
            NBTTagCompound propData = new NBTTagCompound();
            propData.setBoolean("received", received);
            compound.setTag(EXT_PROP_KEY, propData);
        }

        @Override
        public void loadNBTData(NBTTagCompound compound) {
            NBTTagCompound propData = compound.getCompoundTag(EXT_PROP_KEY);
            received = propData.getBoolean("received");
        }

        @Override
        public void init(Entity entity, World world) {}
    }
}
