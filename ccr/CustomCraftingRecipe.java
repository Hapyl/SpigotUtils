
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.WordUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.bukkit.ChatColor.*;

public final class CustomCraftingRecipe implements Listener, Runnable {

    /**
     * This value represents delay between recipe check in ticks. (20 ticks is 1 second)
     * Lower - Faster but more lagier.
     * Higher - Slower but less lagier.
     * [10 is optimal value, but you can change it in any time. (RESTART REQUIRED)]
     */

    public static long RUNNABLE_TICK_CHANGEABLE = 10L; // default 10

    // Block that represents crafting block, set to 'null' to disable.
    public static Material CRAFTING_BLOCK = Material.SMITHING_TABLE;
    public static final Set<CustomCraftingRecipe> storage = Sets.newHashSet();

    private String key, message, permission, sMsg = "&aYou've successfully crafted the %s&a!", fMsg = "&cYou didn't succeed crafting the %s&c!";
    private Set<CraftShape> shapes = Sets.newLinkedHashSet();
    private ItemStack result, previewResult;
    private ItemStack[] currentShape;
    private Sound sound;
    private float pitch;
    private int exp = -1;
    private double chance;
    private boolean notifyPerm, removeExp, previewOnly = false, soundOnlySuccess = false, allowBulk = false;

    /**
     * [!] THIS STEP IS REQUIRED [!]
     * Constructor for registering events and runnable.
     * Just pass the main class, and everything will be
     * done for you.
     * [!] THIS STEP IS REQUIRED [!]
     * 'new CustomCraftingRecipe(this);' in your onEnable;
     */
    public CustomCraftingRecipe(Plugin main) {
        main.getServer().getPluginManager().registerEvents(this, main);
        main.getServer().getPluginManager().registerEvents(new CraftGUI(), main);
        Bukkit.getScheduler().scheduleSyncRepeatingTask(main, this, 0L, RUNNABLE_TICK_CHANGEABLE);
    }

    /**
     * Add this after registration recipes, to see what was registered.
     */
    public static void debugMessage() {
        final Set<String> keys = Sets.newLinkedHashSet();
        storage.forEach(K -> keys.add(K.key));
        Bukkit.getLogger().info(String.format("[CustomCrafting] Registered recipes: '%s'", keys));
    }

    /**
     * Constructor for creating a recipe.
     * Examples:
     * For examples see 'customrecipes.example' file
     * <p>
     * [!] Don't forget to .create() the recipe!
     *
     * @param key    - ID key for recipe name, must be not in use.
     * @param shape  - ItemStack[9] of items, that represents shape of the recipe. Shape must be [9] elements, with 'null' as nothing.
     *               (Use 'ISB' (or similar builder) for easier item building.)
     * @param result - Result of the recipe.
     */
    @Deprecated(/* This method might be confusing to use, instead use with 'ItemStack[]' one*/)
    public CustomCraftingRecipe(String key, CraftShape shape, ItemStack result) {
        if (isAlreadyRegistered(key)) throw new NullPointerException(String.format("Key '%s' already registered!", key));

        this.shapes.add(shape);
        this.result = result;
        this.key = key;

        /** Debug Info moved into {@link #debugMessage} */

    }

    /**
     * You can use this as replacement to 'CraftShape' one since shapeless not
     * implemented anyways.
     *
     * @param key         - ID key for recipe name, must be not in use.
     * @param ingredients - ItemStack[9] of items, that represents shape of the recipe. Shape must be [9] elements, with 'null' as nothing.
     *                    (Use 'ISB' (or similar builder) for easier item building.)
     * @param result      - Result of the recipe.
     */
    public CustomCraftingRecipe(String key, ItemStack[] ingredients, ItemStack result) {
        if (isAlreadyRegistered(key)) throw new NullPointerException(String.format("Key '%s' already registered!", key));

        this.shapes.add(asShaped(ingredients));
        this.result = result;
        this.key = key;
    }

    public CustomCraftingRecipe addAlias(CraftShape anotherShape) {
        if (this.shapes.contains(anotherShape)) throw new NullPointerException("Already exist.");
        else this.shapes.add(anotherShape);
        return this;
    }

    /**
     * The message will be send to the player when an item crafted.
     *
     * @param msg - Message to send.
     */
    public CustomCraftingRecipe withMessage(String msg) {
        this.message = msg;
        return this;
    }

    /**
     * Chance of creating an item
     *
     * @param chance - Chance of success. Max 100.0
     */
    public CustomCraftingRecipe withChance(double chance) {
        withChance(chance, this.sMsg, this.fMsg);
        return this;
    }

    /**
     * Advanced chance of creating item.
     *
     * @param chance         - Chance of success. Max 100.0
     * @param successMessage - Message when item successfully crafted. {@link #sMsg} for default message;
     * @param failMessage    - Message when item failed to craft. {@link #fMsg} for default message;
     */
    public CustomCraftingRecipe withChance(double chance, String successMessage, String failMessage) {
        this.chance = Math.min(chance, 100.0);
        this.sMsg = successMessage;
        this.fMsg = failMessage;
        return this;
    }

    /**
     * Plays a sound when item has been crafted.
     *
     * @param sound         - Sound to play.
     * @param pitch         - Pitch of the sound. Min 0, max 2.
     * @param onlyOnSuccess - If true, sound will only be played if item succeed to craft (Works only if chance enabled)
     */
    public CustomCraftingRecipe withSound(Sound sound, float pitch, boolean onlyOnSuccess) {
        this.sound = sound;
        this.pitch = pitch;
        this.soundOnlySuccess = onlyOnSuccess;
        return this;
    }

    /**
     * Sets permission requirement for item recipe.
     *
     * @param permissionNode - String of a permission node.
     * @param notify         - Notify the player, that they need a permission.
     */
    public CustomCraftingRecipe withPermission(String permissionNode, boolean notify) {
        this.permission = permissionNode;
        this.notifyPerm = notify;
        return this;
    }

    /**
     * Sets EXP lvl as requirement.
     *
     * @param expLvl           - LVLs of Experience.
     * @param removeAfterCraft - If true, the Lvl will be removed from player.
     */
    public CustomCraftingRecipe withExpRequired(int expLvl, boolean removeAfterCraft) {
        this.exp = expLvl;
        this.removeExp = removeAfterCraft;
        return this;
    }

    /**
     * If checked, chance and other requirements will be hidden from an item lore.
     * ..todo Implement per requirement showing/hiding.
     */
    public CustomCraftingRecipe hideItemRequirementsOnLore() {
        this.previewOnly = true;
        return this;
    }

    /**
     * If checked, players' will be able to craft an item in bulk (Shift Click).
     */
    public CustomCraftingRecipe allowBulk() {
        this.allowBulk = true;
        return this;
    }

    /**
     * Creates a recipe.
     * [!] Must be executed to create a recipe. [!]
     */
    public void create() {
        previewItem(this);
        storage.add(this);
    }

    /**
     * By default 'Smithing Table' is the Crafting Table block.
     * Change {@link #CRAFTING_BLOCK} to 'null' to disable.
     */
    @EventHandler
    private void openCrafting(PlayerInteractEvent ev) {

        if (CRAFTING_BLOCK == null) return;

        final Block clicked = ev.getClickedBlock();
        final Player player = ev.getPlayer();

        if (ev.getHand() == EquipmentSlot.HAND && ev.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (clicked != null && clicked.getType() == CRAFTING_BLOCK) {
                ev.setCancelled(true);
                new CraftGUI(player);
            }
        }
    }

    /**
     * Helper for easier creation.
     */

    /**
     * Use instead of 'new CraftShape(CraftShape.CraftShapeType.SHAPED, ItemStack[]...)'
     *
     * @param ingr - ItemStack[9] of ingredients.
     */
    public static CraftShape asShaped(ItemStack[] ingr) {
        return new CraftShape(CraftShape.CraftShapeType.SHAPED, ingr);
    }

    /**
     * Use instead of new 'ItemStack(Material.NAME)'
     * (Import static this class for easier access)
     *
     * @param mat - Material to create.
     */
    @Deprecated(/* Use ISB instead */)
    public static ItemStack toStack(Material mat) {
        return new ItemStack(mat);
    }

    /**
     * Use instead of new 'ItemStack(Material.NAME)'
     * (Import static this class for easier access)
     *
     * @param mat    - Material to create.
     * @param amount - Amount of item.
     */
    @Deprecated(/* Use ISB instead */)
    public static ItemStack toStack(Material mat, int amount) {
        final ItemStack stack = toStack(mat);
        stack.setAmount(amount);
        return stack;
    }

    /**
     * Basic ItemStack builder.
     */
    public static class ISB {

        private ItemStack item;
        private ItemMeta meta;

        public ISB(Material mat) {
            this.item = new ItemStack(mat);
            this.meta = this.item.getItemMeta();
        }

        public ISB setName(final String displayName) {
            this.meta.setDisplayName(CustomCraftingRecipe.format(displayName));
            return this;
        }

        public ISB setAmount(final int amount) {
            this.item.setAmount(amount);
            return this;
        }

        public ISB setLore(final String... lore) {
            final List<String> temp = Lists.newArrayList();
            for (String value : lore) temp.add(GRAY + format(value));
            this.meta.setLore(temp);
            return this;
        }

        public ISB hideFlag(ItemFlag... flag) {
            this.meta.addItemFlags(flag);
            return this;
        }

        public ISB hideFlags() {
            for (ItemFlag value : ItemFlag.values()) this.meta.addItemFlags(value);
            return this;
        }

        public ISB makeUnbreakable() {
            this.meta.setUnbreakable(true);
            return this;
        }

        public ISB addEnchant(Enchantment ench, int lvl) {
            this.meta.addEnchant(ench, lvl, true);
            return this;
        }

        public ItemStack build() {
            this.item.setItemMeta(this.meta);
            return this.item;
        }
    }

    /**
     * Everything down below is code needed for Recipes to work.
     * It's not recommended to change it, since you can break it.
     * So,
     * [DO NOT TOUCH ANY OF CODE BELOW]
     * [DO NOT TOUCH ANY OF CODE BELOW]
     * [DO NOT TOUCH ANY OF CODE BELOW],
     * Thanks.
     */

    protected static boolean theSame(ItemStack item, ItemStack anotherItem) {
        if (item == null && anotherItem == null) return true;
        else if (item != null && anotherItem != null) {
            if (item.hasItemMeta() || anotherItem.hasItemMeta()) {
                return anotherItem.getItemMeta().equals(item.getItemMeta());
            } else return item.getType() == anotherItem.getType() && anotherItem.getAmount() >= item.getAmount();
        } else return false;
    }

    private final String CURRENT_COLOR = LIGHT_PURPLE
            .toString();

    private void previewItem(CustomCraftingRecipe recipe) {

        recipe.previewResult = recipe.result.clone();
        final ItemMeta meta = recipe.previewResult.getItemMeta();

        List<String> tempLore = Lists.newArrayList();
        if (recipe.previewResult.hasItemMeta() && recipe.previewResult.getItemMeta().hasLore())
            tempLore = recipe.previewResult.getItemMeta().getLore();

        tempLore.addAll(Arrays.asList(
                "", format("&8&m                                   "), "",
                format("&7This is preview of the item"),
                format("&7you're crafting. Click to craft.")
        ));

        if (recipe.allowBulk) tempLore.add(format("&7Shift click to craft in bulk."));
        meta.setLore(tempLore);

        if (recipe.previewOnly) {
            recipe.previewResult.setItemMeta(meta);
            return;
        }

        if (recipe.chance > 0) {
            tempLore.addAll(Arrays.asList("", format("$colorThis item has &e" + recipe.chance + "% $colorof craft success.").replace("$color", CURRENT_COLOR)));
            meta.setLore(tempLore);
        }

        final List<String> temp = Lists.newArrayList();

        if (recipe.exp > 0) temp.add(colorize("&e%s $color$Experience LVL%s", recipe.exp, recipe.exp > 1 ? "s" : ""));
        if (recipe.permission != null) temp.add(colorize("$color$Permission Node &e'%s'$color$", recipe.permission));

        if (!temp.isEmpty()) {
            tempLore.addAll(Arrays.asList("", format("&7To craft this item, you must have:")));
            tempLore.addAll(dotify(temp));
            meta.setLore(tempLore);
        }

        recipe.previewResult.setItemMeta(meta);

    }

    private String colorize(String input, Object... format) {
        return String.format(format(input.replace("$color$", CURRENT_COLOR)), format);
    }

    private List<String> dotify(final List<String> input) {
        final List<String> temp = Lists.newArrayList();
        for (String string : input) temp.add(string.equals(input.get(input.size() - 1)) ? string + "." : string + ",");
        return temp;
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getOpenInventory().getTitle().equalsIgnoreCase(CraftGUI.GUI_NAME)) {
                final InventoryView craft = player.getOpenInventory();
                recipesIterator:
                for (CustomCraftingRecipe recipe : storage) {
                    for (CraftShape shape : recipe.shapes) {
                        if (shape.type == CraftShape.CraftShapeType.SHAPED) {
                            if (isValidRecipe(craft, shape.ingredients)) {
                                recipe.currentShape = shape.ingredients;
                                craft.setItem(CraftGUI.RESULT_SLOT, recipe.previewResult);
                                player.updateInventory();
                                break recipesIterator;
                            } else {
                                craft.setItem(CraftGUI.RESULT_SLOT, CraftGUI.NO_RECIPE_ITEM);
                                player.updateInventory();
                            }
                        } else throw new NotImplementedException("Shapeless crafting not implemented yet!");
                    }
                }
            }
        }
    }

    @EventHandler
    private void handleItemClick(InventoryClickEvent ev) {
        if (ev.getView().getTitle().equalsIgnoreCase(CraftGUI.GUI_NAME)) {

            final Player player = (Player) ev.getWhoClicked();
            final Inventory inv = ev.getClickedInventory();
            final ItemStack clickedItem = ev.getCurrentItem();

            if (clickedItem != null && isResultItem(clickedItem) && ev.getRawSlot() == CraftGUI.RESULT_SLOT) {
                for (CustomCraftingRecipe recipe : storage) {
                    if (recipe.previewResult.isSimilar(clickedItem)) {
                        ev.setCancelled(true);

                        if (recipe.currentShape == null) continue;

                        /* Important Checks First */

                        // Check if recipe is still a thing.
                        if (!isValidRecipe(ev.getView(), recipe.currentShape)) {
                            player.sendMessage(colorize("&cSomething went wrong, unable to craft %s.", recipe.getResultName()));
                            return;
                        }

                        // Permission check
                        if (recipe.permission != null) {
                            if (!player.hasPermission(recipe.permission)) {
                                if (recipe.notifyPerm)
                                    player.sendMessage(colorize("&cYou are missions '%s' permission to craft this item!", recipe.permission));
                                return;
                            }
                        }

                        // Exp check
                        if (recipe.exp > 0) {
                            if (player.getLevel() >= recipe.exp) {
                                if (recipe.removeExp) player.setLevel(player.getLevel() - recipe.exp);
                            } else {
                                player.sendMessage(colorize("&cYou don't have enough levels to craft it! &7(&c%s&7/&e%s&7)", player.getLevel(), recipe.exp));
                                return;
                            }
                        }

                        // Chance check
                        boolean dontGive = false;
                        if (recipe.chance > 0) {
                            final double choice = new Random().nextDouble() * 100;
                            if (choice > recipe.chance) {
                                if (!recipe.fMsg.isEmpty()) player.sendMessage(colorize(recipe.fMsg, recipe.getResultName()));
                                dontGive = true;
                            } else if (!recipe.sMsg.isEmpty()) player.sendMessage(colorize(recipe.sMsg, recipe.getResultName()));
                        }

                        /* End of Important Checks */

                        // Message check
                        if (recipe.message != null) {
                            player.sendMessage(format(recipe.message));
                        }

                        // Sound check
                        if (recipe.sound != null) {
                            if (recipe.soundOnlySuccess && !dontGive)
                                player.playSound(player.getLocation(), recipe.sound, SoundCategory.MASTER, 2, recipe.pitch);
                        }

                        // Ingredients and Result worker.
                        for (CraftShape shape : recipe.shapes) {
                            if (shape.ingredients == recipe.currentShape)
                                craft(player, inv, CraftGUI.getItems(inv), recipe, dontGive, ev.getClick() == ClickType.SHIFT_RIGHT || ev.getClick() == ClickType.SHIFT_LEFT);
                        }
                    }
                }
            }
        }
    }

    private void craft(final Player player, Inventory inv, ItemStack[] ITEMS, CustomCraftingRecipe recipe, boolean dontGive, boolean bulk) {
        boolean yes = false;
        for (int i = 0; i < 9; i++) {
            final ItemStack item = ITEMS[i];
            if (item != null) {
                item.setAmount(item.getAmount() - recipe.currentShape[i].getAmount());
                inv.setItem(CraftGUI.SLOTS[i], item);
                if (recipe.allowBulk && bulk && item.getAmount() >= recipe.currentShape[i].getAmount()) yes = true;
            }
        }
        inv.setItem(CraftGUI.RESULT_SLOT, null);
        if (!dontGive) giveOrDrop(player, recipe.result);
        if (yes) craft(player, inv, ITEMS, recipe, dontGive, true);
    }

    private boolean isValidRecipe(InventoryView inventory, ItemStack[] ingredients) {
        return CraftGUI.craftGUIMatches(inventory.getTopInventory(), ingredients);
    }

    private String getResultName() {
        if (this.result.hasItemMeta() && this.result.getItemMeta().hasDisplayName()) return this.result.getItemMeta().getDisplayName();
        else return WordUtils.capitalize(this.result.getType().name().replaceAll("_", "").toLowerCase());
    }

    private boolean isResultItem(ItemStack item) {
        return item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().contains(GRAY + "This is preview of the item");
    }

    @Override
    public String toString() {
        return String.format("%s - Shape[%s]\nResult - %s", this.key, ""/*todo*/, this.result);
    }

    public static void giveOrDrop(Player player, ItemStack item) {
        if (item == null) return;
        if (player.getInventory().firstEmpty() == -1) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
            player.sendMessage(RED + "The item was dropped on the ground since you don't have any inventory space.");
        } else player.getInventory().addItem(item);
    }

    private boolean isAlreadyRegistered(String key) {
        for (CustomCraftingRecipe saved : storage) {
            if (saved.key.equals(key)) return true;
        }
        return false;
    }

    public static class CraftGUI implements Listener {

        private static final String GUI_NAME = "Smithing Table";
        private static final ItemStack BLOCK_ITEM = new ISB(Material.BLACK_STAINED_GLASS_PANE).setName("&0").build();
        private static final ItemStack NO_RECIPE_ITEM = new ISB(Material.BARRIER).setName("&cNo Recipe Found")
                .setLore("&7Seems like there is no", "&7recipe found. Make sure", "&7all the items are correct.").build();

        public final Sound GUI_OPEN_SOUND = Sound.ENTITY_IRON_GOLEM_ATTACK;
        public final float PITCH = 0;

        public static final int[] SLOTS = {10, 11, 12, 19, 20, 21, 28, 29, 30};
        public static final int RESULT_SLOT = 24;

        public CraftGUI() {
            // Constructor for event registration.
        }

        public CraftGUI(Player player, boolean sound) {
            player.openInventory(createInventory());
            if (sound) player.playSound(player.getLocation(), GUI_OPEN_SOUND, SoundCategory.BLOCKS, 1, PITCH);
        }

        public CraftGUI(Player player) {
            new CraftGUI(player, true);
        }

        public Inventory createInventory() {
            final Inventory inv = Bukkit.createInventory(null, 9 * 5, GUI_NAME);
            for (int i = 0; i <= 44; i++) {
                if (isCraftingSlot(i)) continue;
                if (i == RESULT_SLOT) inv.setItem(i, NO_RECIPE_ITEM);
                else inv.setItem(i, BLOCK_ITEM);
            }
            return inv;
        }

        public static boolean craftGUIMatches(Inventory gui, ItemStack[] a) {
            for (int i = 0; i < 9; i++) {
                if (!theSame(a[i], gui.getItem(SLOTS[i]))) return false;
            }
            return true;
        }

        public static ItemStack[] getItems(Inventory inv) {
            final ItemStack[] items = new ItemStack[9];
            for (int i = 0; i < 9; i++) {
                items[i] = inv.getItem(CraftGUI.SLOTS[i]);
            }
            return items;
        }

        private boolean isCraftingSlot(int a) {
            for (int slot : SLOTS) if (a == slot) return true;
            return false;
        }

        @EventHandler(priority = EventPriority.HIGHEST)
        private void stopStealing(InventoryClickEvent ev) {
            if (ev.getView().getTitle().equalsIgnoreCase(GUI_NAME)) {
                if (ev.getRawSlot() == RESULT_SLOT && ev.getClick() == ClickType.SHIFT_LEFT || ev.getClick() == ClickType.SHIFT_RIGHT)
                    ev.setCancelled(true);
                if (!isCraftingSlot(ev.getRawSlot()) && ev.getRawSlot() <= 44) ev.setCancelled(true);
            }
        }

        @EventHandler
        private void stopStealing(InventoryDragEvent ev) {
            if (ev.getView().getTitle().equalsIgnoreCase(GUI_NAME)) {
                for (Integer i : ev.getNewItems().keySet()) {
                    if (!isCraftingSlot(i)) ev.setCancelled(true);
                }
            }
        }

        @EventHandler
        private void giveItemsBack(InventoryCloseEvent ev) {
            if (ev.getView().getTitle().equalsIgnoreCase(GUI_NAME)) {
                final HumanEntity player = ev.getPlayer();
                for (int i = 0; i < 9; i++) {
                    final ItemStack item = ev.getView().getItem(SLOTS[i]);
                    if (item != null) {
                        CustomCraftingRecipe.giveOrDrop((Player) player, item);
                    }
                }
            }
        }
    } // End of the class.

    public static class CraftShape {

        private CraftShapeType type;
        private ItemStack[] ingredients;

        CraftShape(CraftShapeType type, ItemStack[] ingredients) {
            if (ingredients.length == 0) throw new ArrayIndexOutOfBoundsException("There must be at least 0 ingredient!");
            if (ingredients.length > 9) throw new ArrayIndexOutOfBoundsException("Maximum ingredients length is 9!");
            this.type = type;
            this.ingredients = ingredients;
        }

        public enum CraftShapeType {SHAPED, SHAPELESS}

    }

    protected static String format(String value) {
        return ChatColor.translateAlternateColorCodes('&', value);
    }

}
