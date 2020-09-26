you package goes here

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.hapyl.classesfight.GameManager;
import ru.hapyl.classesfight.PlayerDatabase;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

/**
 * This class used for building complex ItemStack easier.
 *
 * @author hapyl
 * @version 2.2
 */

public final class ItemBuilder implements Listener {

/** ----------[ Examples ]----------
 *
 * // Creates an Iron Hoe with Green name and gray lore.
 *  final ItemStack hoe =
 *          new ItemBuilder(Material.IRON_HOE)
 *                  .setName("&aMy great hoe!")
 *                  .setLore("&7This is a legendary hoe.")
 *                  .build();
 *
 * // Creates a Diamond Hoe with Orange name click event for RIGHT CLICKING AIR, and sends the message whenever clicked.
 *  final ItemStack coolerHoe =
 *          new ItemBuilder(Material.DIAMOND_HOE)
 *                  .setName("&6The cooler hoe!").setLore("")
 *                  .addClickEvent(player -> player.sendMessage("You just clicked the cooler hoe!"), Action.RIGHT_CLICK_AIR)
 *                  .build();
 *
 * // Creates a Netherite Hoe with Purple & Bold name and click event that sends message but with 60 ticks of cooldown, and sends error message whenever player has cooldown.
 *  final ItemStack evenCoolerHoe =
 *          new ItemBuilder(Material.NETHERITE_HOE)
 *                  .setName("&5&lEVEN COOLER HOE")
 *                  .setLore("&5The coolest hoe of the coolest hoes...")
 *                  .withCooldown(60, player -> player.hasCooldown(Material.NETHERITE_HOE), "Wait for cooldown to end!")
 *                  .addClickEvent(player -> player.sendMessage("You clicked even cooler hoe!"))
 *                  .build();
 *
 *
 *                  */


    /**
     * Don't forget to register events for this class!
     * Don't forget to register events for this class!
     * Don't forget to register events for this class!
     */

    public static Map<String, ItemStack> holder = new HashMap<>();
    public static final Set<ItemBuilder> executableStorage = new HashSet<>();

    private ItemStack item;
    private ItemMeta meta;
    private int cd;
    private Predicate<Player> predicate;
    private String id, error;
    private Set<ItemAction> functions = new HashSet<>();

    private ItemBuilder() {
        // use constructor down below to register events
    }

    // use this for event registration
    public ItemBuilder(org.bukkit.plugin.Plugin javaPlugin) {
        javaPlugin.getServer().getPluginManager().registerEvents(this, javaPlugin);
    }

    /**
     * Creates an ItemStack from material.
     *
     * @param material - Material.
     */
    public ItemBuilder(Material material) {
        this(new ItemStack(material));
    }

    /**
     * Creates an ItemStack from material and registers with ID.
     *
     * @param material - Material.
     * @param id       - Unique Id.
     */
    public ItemBuilder(Material material, String id) {
        this(new ItemStack(material), id);
    }

    /**
     * Creates an ItemStack from existing ItemStack.
     *
     * @param stack - ItemStack.
     */
    public ItemBuilder(ItemStack stack) {
        this.item = stack;
        this.meta = stack.getItemMeta();
    }

    /**
     * Creates an ItemStack from existing ItemStack and registers with ID.
     *
     * @param stack - ItemStack.
     * @param id    - Unique Id.
     */
    public ItemBuilder(ItemStack stack, String id) {

        if (holder.containsKey(id)) {
            Bukkit.getLogger().warning(String.format("Item with id '%s' already registered!", id));
            return;
        }

        this.item = stack;
        this.meta = stack.getItemMeta();
        this.id = id;
    }

    /**
     * Static method for easier player head creation.
     *
     * @param texture - Base64 texture of the skins.
     * @return - ItemBuilder.
     */
    public static ItemBuilder playerHead(String texture) {
        return new ItemBuilder(Material.PLAYER_HEAD).setHeadTexture(texture);
    }

    /**
     * Static method for easier leather armor creation.
     *
     * @param color - Color of the armor.
     * @return - ItemBuilder.
     */
    public static ItemBuilder leatherHat(Color color) {
        return new ItemBuilder(Material.LEATHER_HELMET).setLeatherArmorColor(color);
    }

    /**
     * Static method for easier leather armor creation.
     *
     * @param color - Color of the armor.
     * @return - ItemBuilder.
     */
    public static ItemBuilder leatherTunic(Color color) {
        return new ItemBuilder(Material.LEATHER_CHESTPLATE).setLeatherArmorColor(color);
    }

    /**
     * Static method for easier leather armor creation.
     *
     * @param color - Color of the armor.
     * @return - ItemBuilder.
     */
    public static ItemBuilder leatherPants(Color color) {
        return new ItemBuilder(Material.LEATHER_LEGGINGS).setLeatherArmorColor(color);
    }

    /**
     * Static method for easier leather armor creation.
     *
     * @param color - Color of the armor.
     * @return - ItemBuilder.
     */
    public static ItemBuilder leatherBoots(Color color) {
        return new ItemBuilder(Material.LEATHER_BOOTS).setLeatherArmorColor(color);
    }

    /**
     * Adds cooldown to the item, requires
     * ClickEvent on the item to work.
     *
     * @param ticks - Ticks of cooldown.
     * @return - ItemBuilder.
     */
    public ItemBuilder withCooldown(int ticks) {
        withCooldown(ticks, null);
        return this;
    }

    /**
     * Adds cooldown to the item, requires
     * ClickEvent on the item to work.
     *
     * @param ticks     - Ticks of cooldown.
     * @param predicate - Predicate of player, if test
     *                  failed, click event will not fire.
     * @return ItemBuilder.
     */
    public ItemBuilder withCooldown(int ticks, Predicate<Player> predicate) {
        withCooldown(ticks, predicate, "");
        return this;
    }

    /**
     * Adds cooldown to the item, requires
     * ClickEvent on the item to work.
     *
     * @param ticks        - Ticks of cooldown.
     * @param predicate    - Predicate of player, if test
     *                     failed, click event will not fire.
     * @param errorMessage - A string message that will
     *                     be send to the player whenever
     *                     predicate if failed.
     * @return ItemBuilder.
     */
    public ItemBuilder withCooldown(int ticks, Predicate<Player> predicate, String errorMessage) {
        this.predicate = predicate;
        this.cd = ticks;
        this.error = errorMessage;
        return this;
    }

    /**
     * Removes click event from the Item.
     */
    public ItemBuilder removeClickEvent() {
        this.functions.clear();
        return this;
    }

    /**
     * Returns Item by it's ID.
     *
     * @param id - ID
     * @return ItemStack
     */
    @Nullable
    public static ItemStack getItemByID(String id) {
        if (holder.containsKey(id)) {
            return holder.get(id);
        }
        return null;
    }

    /**
     * Broadcasts all registered Ids, used for debugging.
     */
    public static void broadcastRegisteredIDs() {
        Bukkit.getLogger().info("[ItemBuilder] Registered Custom Items:");
        System.out.println(holder.keySet());
    }

    /**
     * Returns item id or null.
     *
     * @param item - ItemStack
     * @return String
     */
    @Nullable
    public static String getItemID(ItemStack item) {
        return Editor.getString(item, "custom_id");
    }

    /**
     * Check if item has id.
     *
     * @param item - Item.
     * @param id   - Id.
     * @return true if item has id.
     */
    public static boolean itemHasID(ItemStack item, String id) {
        return itemHasID(item) && getItemID(item).equals(id.toLowerCase());
    }

    /**
     * Adds click event to the item.
     *
     * @param consumer - Runnable.
     * @param act      - Actions that runnable is bind to.
     */
    public ItemBuilder addClickEvent(Consumer<Player> consumer, Action... act) {
        if (act.length < 1) throw new IndexOutOfBoundsException("This requires at least 1 action.");
        this.functions.add(new ItemAction(consumer, act));
        return this;
    }

    /**
     * Adds click event to the item.
     *
     * @param consumer - Runnable.
     */
    public ItemBuilder addClickEvent(Consumer<Player> consumer) {
        this.addClickEvent(consumer, Action.RIGHT_CLICK_BLOCK, Action.RIGHT_CLICK_AIR);
        return this;
    }

    /**
     * Checks if item has Id.
     *
     * @param item - ItemStack.
     * @return true if so.
     */
    public static boolean itemHasID(ItemStack item) {
        return getItemID(item) != null;
    }

    /**
     * Sets the item amount.
     *
     * @param amount - Amount.
     */
    public ItemBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    /**
     * Sets the smart lore; Smart is lore is
     * splitting automatically after certain
     * amount of characters.
     *
     * @param lore      - The lore
     * @param separator - Amount of characters to split after;
     *                  will not split if word isn't finished.
     */
    public ItemBuilder setSmartLore(String lore, final int separator) {
        this.meta.setLore(splitAfter(lore, separator));
        return this;
    }

    /**
     * Sets the smart lore with default amount of split characters (30).
     *
     * @param lore - The lore.
     */
    public ItemBuilder setSmartLore(String lore) {
        this.meta.setLore(splitAfter(lore, 30));
        return this;
    }

    /**
     * Adds smart lore.
     *
     * @param lore       - The lore.
     * @param splitAfter - Amount of characters to split after.
     */
    public ItemBuilder addSmartLore(String lore, final int splitAfter) {
        final List<String> list = this.meta.hasLore() ? meta.getLore() : Lists.newArrayList();
        list.addAll(splitAfter(lore, splitAfter));
        this.meta.setLore(list);
        return this;
    }

    /**
     * Sets the lore of the item; Use '__' to split line.
     *
     * @param lore - The lore.
     */
    public ItemBuilder setLore(String lore) {
        this.setLore(lore, "__");
        return this;
    }

    /**
     * Adds the lore to the item.
     *
     * @param lore            - The lore.
     * @param afterSplitColor - Default color to put after '__' split.
     */
    public ItemBuilder addLore(final String lore, ChatColor afterSplitColor) {
        List<String> metaLore = this.meta.hasLore() ? this.meta.getLore() : Lists.newArrayList();
        for (String value : lore.split("__")) metaLore.add(afterSplitColor + colorize(value));
        this.meta.setLore(metaLore);
        return this;
    }

    /**
     * Adds lore to the item.
     *
     * @param lore - The lore.
     */
    public ItemBuilder addLore(final String lore) {
        return this.addLore(lore, ChatColor.DARK_PURPLE);
    }

    /**
     * Sets the lore of the item with custom separator character.
     * Keep in mind that certain characters cannot be used as
     * separator, in that case an error message will be sent.
     *
     * @param lore      - The lore
     * @param separator - Split character
     */
    public ItemBuilder setLore(final String lore, final String separator) {
        try {
            this.meta.setLore(Arrays.asList(colorize(lore).split(separator)));
        } catch (PatternSyntaxException ex) {
            Bukkit.getConsoleSender().sendMessage(colorize("&4[ERROR] &cChar &e" + separator + " &cused as separator for lore!"));
        }
        return this;
    }

    /**
     * Removes all lore from the Item.
     */
    public ItemBuilder removeLore() {
        if (this.meta.getLore() != null)
            this.meta.setLore(null);
        return this;
    }

    /**
     * Remove lore at given line.
     *
     * @param line - Line to remove lore at.
     */
    public ItemBuilder removeLoreLine(int line) {

        if (this.meta.getLore() == null) throw new NullPointerException("ItemMeta doesn't have any lore!");
        if (line > this.meta.getLore().size())
            throw new IndexOutOfBoundsException("ItemMeta has only " + this.meta.getLore().size() + " lines! Given " + line);
        List<String> old = this.meta.getLore();
        old.remove(line);
        this.meta.setLore(old);
        return this;

    }

    /**
     * Applies default setting to the Item, such as:
     * - Binding Curse
     * - Makes the item unbreakable
     * - Hides all flags.
     */
    public ItemBuilder applyDefaultSettings() {
        return applyDefaultSettings(true);
    }

    public ItemBuilder applyDefaultSettings(boolean applyCurse) {
        if (applyCurse) this.meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
        this.meta.setUnbreakable(true);
        this.meta.addItemFlags(ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    /**
     * Sets the display name of the item.
     *
     * @param name - Name to set.
     */
    public ItemBuilder setName(String name) {
        this.meta.setDisplayName(colorize(name));
        return this;
    }

    /**
     * Adds an enchant to the item.
     *
     * @param ench - Enchantment.
     * @param lvl  - Level of the enchantment.
     */
    public ItemBuilder addEnchant(Enchantment ench, int lvl) {
        this.meta.addEnchant(ench, lvl, true);
        return this;
    }

    /**
     * Makes the item unbreakable.
     */
    public ItemBuilder setUnbreakable() {
        this.meta.setUnbreakable(true);
        return this;
    }

    /**
     * Sets the value of unbreakability to the given one.
     *
     * @param v - The value.
     */
    public ItemBuilder setUnbreakable(boolean v) {
        this.meta.setUnbreakable(v);
        return this;
    }

    /**
     * Sets the repair anvil cost for the Item.
     *
     * @param valueInLevels - Value in level.
     */
    public ItemBuilder setRepairCost(int valueInLevels) {
        Repairable r = (Repairable) this.meta;
        r.setRepairCost(valueInLevels);
        return this;
    }

    /**
     * Sets the potion meta of the item.
     * Item must be POTION, SPLASH_POTION or LINGERING_POTION
     * for this to work.
     *
     * @param type     - Type of the effect.
     * @param lvl      - Level of the effect.
     * @param duration - Duration of the effect in ticks.
     * @param color    - Color of the poton.
     */
    public ItemBuilder setPotionMeta(PotionEffectType type, int lvl, int duration, Color color) {
        Material m = this.item.getType();
        if (m == Material.POTION || m == Material.SPLASH_POTION || m == Material.LINGERING_POTION) {
            PotionMeta meta = (PotionMeta) this.meta;
            meta.addCustomEffect(new PotionEffect(type, duration, lvl), false);
            meta.setColor(color);
            return this;
        }
        return null;
    }

    /**
     * Sets the leather armor color to the given color.
     * Item must be leather armor for this to work.
     *
     * @param color - Color of the armor.
     */
    public ItemBuilder setLeatherArmorColor(Color color) {
        final Material m = this.item.getType();
        if (m == Material.LEATHER_BOOTS || m == Material.LEATHER_CHESTPLATE || m == Material.LEATHER_LEGGINGS || m == Material.LEATHER_HELMET) {
            LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
            meta.setColor(color);
            this.item.setItemMeta(meta);
            return this;
        }
        return this;
    }

    /**
     * Sets the head texture of the item.
     * Item must be PLAYER_HEAD in order
     * for this to work.
     *
     * @param base64 - Base64 value of the texture.
     *               Use minecraft-heads to get head textures.
     */
    public ItemBuilder setHeadTexture(String base64) {

        GameProfile profile = new GameProfile(UUID.randomUUID(), "");
        profile.getProperties().put("textures", new Property("textures", base64));

        try {
            Field f = this.meta.getClass().getDeclaredField("profile");
            f.setAccessible(true);
            f.set(this.meta, profile);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        return this;
    }

    /**
     * Sets the skull owner of the item.
     * Item must be PLAYER_HEAD in order
     * for this to work.
     *
     * @param owner - Name of the owner.
     */
    public ItemBuilder setSkullOwner(String owner) {

        if (this.item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) this.meta;
            meta.setOwner(owner);
            return this;
        }
        return null;
    }

    /**
     * Sets pure damage of the item.
     * Pure damage will ignore already existing damage
     * of the item and override it. Enchantments will
     * still affect the item damage.
     *
     * @param damage - Damage in half hearts.
     */
    public ItemBuilder setPureDamage(double damage) {
        this.addAttribute(Attribute.GENERIC_ATTACK_DAMAGE, damage, AttributeModifier.Operation.ADD_NUMBER, EquipmentSlot.HAND);
        return this;
    }

    /**
     * Adds an Attribute to the Item.
     *
     * @param a         - Attribute to add.
     * @param amount    - Amount of the attribute.
     * @param operation - Operation of the attribute.
     * @param slot      - Working slot for the attribute.
     */
    public ItemBuilder addAttribute(Attribute a, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        this.meta.addAttributeModifier(a, new AttributeModifier(UUID.randomUUID(), a.toString(), amount, operation, slot));
        return this;
    }

    /**
     * Hides certain flags from the Item.
     *
     * @param flag - Flags to hide.
     */
    public ItemBuilder hideFlag(ItemFlag... flag) {
        this.meta.addItemFlags(flag);
        return this;
    }

    /**
     * Shows certain flags on the Item.
     *
     * @param flag - Flags to show.
     */
    public ItemBuilder showFlag(ItemFlag... flag) {
        this.meta.removeItemFlags(flag);
        return this;
    }

    /**
     * Hides all the flags from the Item.
     */
    public ItemBuilder hideFlags() {
        this.meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    public ItemBuilder clearName() {
        this.meta.setDisplayName("");
        return this;
    }

    /**
     * Sets the Item durability.
     *
     * @param dura - Durability.
     */
    public ItemBuilder setDurability(int dura) {
        Damageable meta = (Damageable) this.meta;
        meta.setDamage(dura);
        return this;
    }

    /**
     * Sets the Item type.
     *
     * @param icon - New material.
     */
    public ItemBuilder setType(Material icon) {
        this.item.setType(icon);
        return this;
    }

    /**
     * Clears all the storage, add to onDisable.
     */
    public static void clear() {
        holder.clear();
        executableStorage.clear();
    }

    /**
     * Builds the item and returns ItemStack.
     *
     * @return Final, shiny ItemStack.
     */
    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        if (this.id != null) {
            this.item = Editor.setItemTag(item, this.id, "custom_id");
            holder.put(this.id, this.item);
            if (!this.functions.isEmpty()) {
                executableStorage.add(this);
            }
        }

        // This executes if there no Id and function added.
        else if (!this.functions.isEmpty()) throw new IllegalArgumentException("ID is required to use this.");
        return item;
    }

    // getters

    /**
     * Returns an item name or "" if none.
     *
     * @return - Item name.
     */
    public String getName() {
        return this.meta.getDisplayName();
    }

    /**
     * Returns an item lore as list or strings.
     *
     * @return - Item lore.
     */
    @Nullable
    public List<String> getLore() {
        return this.meta.getLore();
    }

    /**
     * Returns an item lore between lines.
     *
     * @param start - Start lore at line.
     * @param end   - Stop lore at line.
     * @return - Item lore.
     */
    @Nullable
    public List<String> getLore(int start, int end) {
        final List<String> hash = new ArrayList<>();
        final List<String> lore = this.getLore();
        if (lore == null || end > lore.size()) {
            Bukkit.getLogger().warning("There is either no lore or given more that there is lines.");
            return null;
        }
        for (int i = start; i < end; i++) {
            hash.add(lore.get(i));
        }
        return hash;
    }

    /**
     * Returns item amount.
     *
     * @return - Integer
     */
    public int getAmount() {
        return this.item.getAmount();
    }

    /**
     * Returns item enchantments.
     *
     * @return - Map of Enchantment and Integer.
     */
    public Map<Enchantment, Integer> getEnchants() {
        return this.meta.getEnchants();
    }

    /**
     * Returns true if item is unbreakable.
     *
     * @return - Boolean.
     */
    public boolean isUnbreakable() {
        return this.meta.isUnbreakable();
    }

    /**
     * Returns item repair cost.
     *
     * @return - Integer.
     */
    public int getRepairCost() {
        return ((Repairable) this.meta).getRepairCost();
    }

    /**
     * Returns item color if item is leather armor.
     *
     * @return - Color.
     */
    public Color getLeatherColor() {
        return ((LeatherArmorMeta) this.meta).getColor();
    }

    /**
     * Returns head texture of the item if it is player head.
     *
     * @return - String.
     */
    @Nullable
    public String getHeadTexture() {
        try {
            return (String) this.meta.getClass().getDeclaredField("profile").get(this.meta);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Returns item's flags.
     *
     * @return Set of ItemFlag.
     */
    public Set<ItemFlag> getFlags() {
        return this.meta.getItemFlags();
    }

    /**
     * Returns attributes of the item.
     *
     * @return Multimap of Attribute and Modifier.
     */
    public Multimap<Attribute, AttributeModifier> getAttributes() {
        return this.meta.getAttributeModifiers();
    }

    /**
     * Returns pure damage of the item.
     *
     * @return - Double.
     */
    public double getPureDamage() {
        double most = 0;
        for (AttributeModifier t : getAttributes().get(Attribute.GENERIC_ATTACK_DAMAGE)) {
            final double current = t.getAmount();
            most = Math.max(current, most);
        }
        return most;
    }

    private static List<String> splitAfter(String clr, String text, int max) {
        List<String> list = new ArrayList<>();
        String line = "";
        int counter = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            final boolean checkLast = c == text.charAt(text.length() - 1);
            line = line.concat(c + "");
            counter++;
            if (counter >= max || i == text.length() - 1) {
                if (c == ' ' || checkLast) {
                    list.add(ItemBuilder.colorize(clr + line.trim()));
                    line = "";
                    counter = 0;
                }
            }
        }

        // split for manual '__'
        // don't really work that well, need to redo method
        final List<String> strings = new ArrayList<>();
        for (String str : list) {
            final String[] splits = str.split("__");
            for (String s : splits) {
                strings.add(colorize(ChatColor.GRAY + s));
            }
        }

        return strings;
    }

    private static List<String> splitAfter(String text, int max) {
        return splitAfter("&7", text, max);
    }

    private static String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    // this used for addClickEvent
    private static class ItemAction {

        private Set<Action> actions = new HashSet<>();
        private Consumer<Player> consumer;

        ItemAction(Consumer<Player> p, Action... t) {
            actions.addAll(Arrays.asList(t));
            this.consumer = p;
        }

        public void execute(Player player) {
            this.consumer.accept(player);
        }

        public boolean hasAction(Action a) {
            return actions.contains(a);
        }

    }

    @EventHandler
    private static void handleClick(PlayerInteractEvent ev) {

        if (ev.getHand() == EquipmentSlot.OFF_HAND) return;

        final Player player = ev.getPlayer();
        final Action action = ev.getAction();
        final ItemStack item = player.getInventory().getItemInMainHand();

        Set<ItemBuilder> hash = new HashSet<>(executableStorage);

        if (!hash.isEmpty()) {
            hash.iterator().forEachRemaining(builder -> {
                if (builder.id.equals(getItemID(item))) {
                    final Set<ItemAction> functions = builder.functions;
                    for (ItemAction func : functions) {
                        if (func.hasAction(action)) {
                            // cooldown check
                            if (builder.cd > 0) {
                                if (builder.predicate != null && builder.predicate.test(player)) {
                                    if (!builder.error.isEmpty()) player.sendMessage(ChatColor.RED + builder.error);
                                    continue;
                                }
                                if (player.hasCooldown(builder.item.getType())) {
                                    continue;
                                }
                            }
                            func.execute(player);
                        }
                    }
                }
            });
            hash.clear();
        }
    }


    /**
     * This code is taken from BananaPuncher714's NBTEditor
     * with features that needed for IDs only. Get the
     * original class down below. There is could be
     * things that can be removed but im too lazy to
     * test every thing.
     * <p>
     * Original Class:
     * Github: https://github.com/BananaPuncher714/NBTEditor
     * Spigot: https://www.spigotmc.org/threads/269621/
     */

    private static final class Editor {

        private static final Map<String, Class<?>> classCache;
        private static final Map<String, Method> methodCache;
        private static final Map<Class<?>, Constructor<?>> constructorCache;
        private static final Map<Class<?>, Class<?>> NBTClasses;
        private static final Map<Class<?>, Field> NBTTagFieldCache;
        private static Field NBTListData;
        private static Field NBTCompoundMap;
        private static final String VERSION;
        private static final MinecraftVersion LOCAL_VERSION;

        static {
            VERSION = Bukkit.getServer().getClass().getPackage().getName().split("\\.")[3];
            LOCAL_VERSION = MinecraftVersion.get(VERSION);

            classCache = new HashMap<>();
            try {
                classCache.put("NBTBase", Class.forName("net.minecraft.server." + VERSION + "." + "NBTBase"));
                classCache.put("NBTTagCompound", Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagCompound"));
                classCache.put("NBTTagList", Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagList"));
                classCache.put("MojangsonParser", Class.forName("net.minecraft.server." + VERSION + "." + "MojangsonParser"));

                classCache.put("ItemStack", Class.forName("net.minecraft.server." + VERSION + "." + "ItemStack"));
                classCache.put("CraftItemStack", Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory." + "CraftItemStack"));
                classCache.put("CraftMetaSkull", Class.forName("org.bukkit.craftbukkit." + VERSION + ".inventory." + "CraftMetaSkull"));

                classCache.put("GameProfile", Class.forName("com.mojang.authlib.GameProfile"));
                classCache.put("Property", Class.forName("com.mojang.authlib.properties.Property"));
                classCache.put("PropertyMap", Class.forName("com.mojang.authlib.properties.PropertyMap"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            NBTClasses = new HashMap<>();
            try {
                NBTClasses.put(Byte.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagByte"));
                NBTClasses.put(Boolean.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagByte"));
                NBTClasses.put(String.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagString"));
                NBTClasses.put(Double.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagDouble"));
                NBTClasses.put(Integer.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagInt"));
                NBTClasses.put(Long.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagLong"));
                NBTClasses.put(Short.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagShort"));
                NBTClasses.put(Float.class, Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagFloat"));
                NBTClasses.put(Class.forName("[B"), Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagByteArray"));
                NBTClasses.put(Class.forName("[I"), Class.forName("net.minecraft.server." + VERSION + "." + "NBTTagIntArray"));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }

            methodCache = new HashMap<>();
            try {
                methodCache.put("get", getNMSClass("NBTTagCompound").getMethod("get", String.class));
                methodCache.put("set", getNMSClass("NBTTagCompound").getMethod("set", String.class, getNMSClass("NBTBase")));
                methodCache.put("hasKey", getNMSClass("NBTTagCompound").getMethod("hasKey", String.class));
                methodCache.put("setIndex", getNMSClass("NBTTagList").getMethod("a", int.class, getNMSClass("NBTBase")));
                if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_14)) {
                    methodCache.put("getTypeId", getNMSClass("NBTBase").getMethod("getTypeId"));
                    methodCache.put("add", getNMSClass("NBTTagList").getMethod("add", int.class, getNMSClass("NBTBase")));
                } else {
                    methodCache.put("add", getNMSClass("NBTTagList").getMethod("add", getNMSClass("NBTBase")));
                }
                methodCache.put("size", getNMSClass("NBTTagList").getMethod("size"));

                if (LOCAL_VERSION == MinecraftVersion.v1_8) {
                    methodCache.put("listRemove", getNMSClass("NBTTagList").getMethod("a", int.class));
                } else {
                    methodCache.put("listRemove", getNMSClass("NBTTagList").getMethod("remove", int.class));
                }
                methodCache.put("remove", getNMSClass("NBTTagCompound").getMethod("remove", String.class));

                if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_13)) {
                    methodCache.put("getKeys", getNMSClass("NBTTagCompound").getMethod("getKeys"));
                } else {
                    methodCache.put("getKeys", getNMSClass("NBTTagCompound").getMethod("c"));
                }

                methodCache.put("hasTag", getNMSClass("ItemStack").getMethod("hasTag"));
                methodCache.put("getTag", getNMSClass("ItemStack").getMethod("getTag"));
                methodCache.put("setTag", getNMSClass("ItemStack").getMethod("setTag", getNMSClass("NBTTagCompound")));
                methodCache.put("asNMSCopy", getNMSClass("CraftItemStack").getMethod("asNMSCopy", ItemStack.class));
                methodCache.put("asBukkitCopy", getNMSClass("CraftItemStack").getMethod("asBukkitCopy", getNMSClass("ItemStack")));

                methodCache.put("save", getNMSClass("ItemStack").getMethod("save", getNMSClass("NBTTagCompound")));

                if (LOCAL_VERSION.lessThanOrEqualTo(MinecraftVersion.v1_10)) {
                    methodCache.put("createStack", getNMSClass("ItemStack").getMethod("createStack", getNMSClass("NBTTagCompound")));
                } else if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_13)) {
                    methodCache.put("createStack", getNMSClass("ItemStack").getMethod("a", getNMSClass("NBTTagCompound")));
                }

                methodCache.put("values", getNMSClass("PropertyMap").getMethod("values"));
                methodCache.put("put", getNMSClass("PropertyMap").getMethod("put", Object.class, Object.class));

                methodCache.put("loadNBTTagCompound", getNMSClass("MojangsonParser").getMethod("parse", String.class));
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                methodCache.put("getTileTag", getNMSClass("TileEntity").getMethod("save", getNMSClass("NBTTagCompound")));
            } catch (NoSuchMethodException exception) {
                try {
                    methodCache.put("getTileTag", getNMSClass("TileEntity").getMethod("b", getNMSClass("NBTTagCompound")));
                } catch (Exception exception2) {
                    exception2.printStackTrace();
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }

            try {
                methodCache.put("setProfile", getNMSClass("CraftMetaSkull").getDeclaredMethod("setProfile", getNMSClass("GameProfile")));
                methodCache.get("setProfile").setAccessible(true);
            } catch (NoSuchMethodException exception) {

            }

            constructorCache = new HashMap<>();
            try {

                constructorCache.put(getNBTTag(Byte.class), getNBTTag(Byte.class).getDeclaredConstructor(byte.class));
                constructorCache.put(getNBTTag(Boolean.class), getNBTTag(Boolean.class).getDeclaredConstructor(byte.class));
                constructorCache.put(getNBTTag(String.class), getNBTTag(String.class).getDeclaredConstructor(String.class));
                constructorCache.put(getNBTTag(Double.class), getNBTTag(Double.class).getDeclaredConstructor(double.class));
                constructorCache.put(getNBTTag(Integer.class), getNBTTag(Integer.class).getDeclaredConstructor(int.class));
                constructorCache.put(getNBTTag(Long.class), getNBTTag(Long.class).getDeclaredConstructor(long.class));
                constructorCache.put(getNBTTag(Float.class), getNBTTag(Float.class).getDeclaredConstructor(float.class));
                constructorCache.put(getNBTTag(Short.class), getNBTTag(Short.class).getDeclaredConstructor(short.class));
                constructorCache.put(getNBTTag(Class.forName("[B")), getNBTTag(Class.forName("[B")).getDeclaredConstructor(Class.forName("[B")));
                constructorCache.put(getNBTTag(Class.forName("[I")), getNBTTag(Class.forName("[I")).getDeclaredConstructor(Class.forName("[I")));


                for (Constructor<?> cons : constructorCache.values()) {
                    cons.setAccessible(true);
                }

                constructorCache.put(getNMSClass("BlockPosition"), getNMSClass("BlockPosition").getConstructor(int.class, int.class, int.class));

                constructorCache.put(getNMSClass("GameProfile"), getNMSClass("GameProfile").getConstructor(UUID.class, String.class));
                constructorCache.put(getNMSClass("Property"), getNMSClass("Property").getConstructor(String.class, String.class));

                if (LOCAL_VERSION == MinecraftVersion.v1_11 || LOCAL_VERSION == MinecraftVersion.v1_12) {
                    constructorCache.put(getNMSClass("ItemStack"), getNMSClass("ItemStack").getConstructor(getNMSClass("NBTTagCompound")));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            NBTTagFieldCache = new HashMap<>();
            try {
                for (Class<?> clazz : NBTClasses.values()) {
                    Field data = clazz.getDeclaredField("data");
                    data.setAccessible(true);
                    NBTTagFieldCache.put(clazz, data);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                NBTListData = getNMSClass("NBTTagList").getDeclaredField("list");
                NBTListData.setAccessible(true);
                NBTCompoundMap = getNMSClass("NBTTagCompound").getDeclaredField("map");
                NBTCompoundMap.setAccessible(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private static Class<?> getNBTTag(Class<?> primitiveType) {
            if (NBTClasses.containsKey(primitiveType))
                return NBTClasses.get(primitiveType);
            return primitiveType;
        }

        private static Object getNBTVar(Object object) {
            if (object == null) {
                return null;
            }
            Class<?> clazz = object.getClass();
            try {
                if (NBTTagFieldCache.containsKey(clazz)) {
                    return NBTTagFieldCache.get(clazz).get(object);
                }
            } catch (Exception exception) {
                exception.printStackTrace();
            }
            return null;
        }

        private static Method getMethod(String name) {
            return methodCache.getOrDefault(name, null);
        }

        private static Constructor<?> getConstructor(Class<?> clazz) {
            return constructorCache.getOrDefault(clazz, null);
        }

        private static Class<?> getNMSClass(String name) {

            if (classCache.containsKey(name)) {
                return classCache.get(name);
            }

            try {
                return Class.forName("net.minecraft.server." + VERSION + "." + name);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }
        }

        public static String getVersion() {
            return VERSION;
        }

        public static MinecraftVersion getMinecraftVersion() {
            return LOCAL_VERSION;
        }

        private static Object getItemTag(ItemStack item, Object... keys) {
            try {
                return getTag(getCompound(item), keys);
            } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                e.printStackTrace();
                return null;
            }
        }

        private static Object getCompound(ItemStack item) {
            if (item == null) {
                return null;
            }
            try {

                Object stack, tag;
                stack = getMethod("asNMSCopy").invoke(null, item);

                if (getMethod("hasTag").invoke(stack).equals(true)) {
                    tag = getMethod("getTag").invoke(stack);
                } else {
                    tag = getNMSClass("NBTTagCompound").newInstance();
                }

                return tag;
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        }

        private static ItemStack setItemTag(ItemStack item, Object value, Object... keys) {
            if (item == null) {
                return null;
            }
            try {

                Object stack = getMethod("asNMSCopy").invoke(null, item);
                Object tag;

                if (getMethod("hasTag").invoke(stack).equals(true)) {
                    tag = getMethod("getTag").invoke(stack);
                } else {
                    tag = getNMSClass("NBTTagCompound").newInstance();
                }

                if (keys.length == 0 && value instanceof NBTCompound) {
                    tag = ((NBTCompound) value).tag;
                } else {
                    setTag(tag, value, keys);
                }

                getMethod("setTag").invoke(stack, tag);
                return (ItemStack) getMethod("asBukkitCopy").invoke(null, stack);
            } catch (Exception exception) {
                exception.printStackTrace();
                return null;
            }
        }

        private static Object getValue(Object object, Object... keys) {

            if (object instanceof ItemStack) {
                return getItemTag((ItemStack) object, keys);
            } else if (object instanceof NBTCompound) {
                try {
                    return getTag(((NBTCompound) object).tag, keys);
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                throw new IllegalArgumentException("Object provided must be of type ItemStack, Entity, Block, or NBTCompound!");
            }
        }

        public static String getString(Object object, Object... keys) {
            Object result = getValue(object, keys);
            return result instanceof String ? (String) result : null;
        }

        private static void setTag(Object tag, Object value, Object... keys) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            Object notCompound;

            if (value != null) {
                if (value instanceof NBTCompound) {
                    notCompound = ((NBTCompound) value).tag;
                } else if (getNMSClass("NBTTagList").isInstance(value) || getNMSClass("NBTTagCompound").isInstance(value)) {
                    notCompound = value;
                } else {
                    if (value instanceof Boolean) {
                        value = (byte) ((Boolean) value ? 1 : 0);
                    }
                    notCompound = getConstructor(getNBTTag(value.getClass())).newInstance(value);
                }
            } else {
                notCompound = null;
            }

            Object compound = tag;
            for (int index = 0; index < keys.length - 1; index++) {
                Object key = keys[index];
                Object oldCompound = compound;
                if (key instanceof Integer) {
                    compound = ((List<?>) NBTListData.get(compound)).get((int) key);
                } else if (key != null) {
                    compound = getMethod("get").invoke(compound, (String) key);
                }
                if (compound == null || key == null) {
                    if (keys[index + 1] == null || keys[index + 1] instanceof Integer) {
                        compound = getNMSClass("NBTTagList").newInstance();
                    } else {
                        compound = getNMSClass("NBTTagCompound").newInstance();
                    }
                    if (oldCompound.getClass().getSimpleName().equals("NBTTagList")) {
                        if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_14)) {
                            getMethod("add").invoke(oldCompound, getMethod("size").invoke(oldCompound), compound);
                        } else {
                            getMethod("add").invoke(oldCompound, compound);
                        }
                    } else {
                        getMethod("set").invoke(oldCompound, (String) key, compound);
                    }
                }
            }
            if (keys.length > 0) {
                Object lastKey = keys[keys.length - 1];
                if (lastKey == null) {
                    if (LOCAL_VERSION.greaterThanOrEqualTo(MinecraftVersion.v1_14)) {
                        getMethod("add").invoke(compound, getMethod("size").invoke(compound), notCompound);
                    } else {
                        getMethod("add").invoke(compound, notCompound);
                    }
                } else if (lastKey instanceof Integer) {
                    if (notCompound == null) {
                        getMethod("listRemove").invoke(compound, (int) lastKey);
                    } else {
                        getMethod("setIndex").invoke(compound, (int) lastKey, notCompound);
                    }
                } else {
                    if (notCompound == null) {
                        getMethod("remove").invoke(compound, (String) lastKey);
                    } else {
                        getMethod("set").invoke(compound, (String) lastKey, notCompound);
                    }
                }
            } else {
                if (notCompound != null) {
                }
            }
        }

        private static NBTCompound getNBTTag(Object tag, Object... keys) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            Object compound = tag;

            for (Object key : keys) {
                if (compound == null) {
                    return null;
                } else if (getNMSClass("NBTTagCompound").isInstance(compound)) {
                    compound = getMethod("get").invoke(compound, (String) key);
                } else if (getNMSClass("NBTTagList").isInstance(compound)) {
                    compound = ((List<?>) NBTListData.get(compound)).get((int) key);
                }
            }
            return new NBTCompound(compound);
        }

        private static Object getTag(Object tag, Object... keys) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if (keys.length == 0) {
                return getTags(tag);
            }

            Object notCompound = tag;

            for (Object key : keys) {
                if (notCompound == null) {
                    return null;
                } else if (getNMSClass("NBTTagCompound").isInstance(notCompound)) {
                    notCompound = getMethod("get").invoke(notCompound, (String) key);
                } else if (getNMSClass("NBTTagList").isInstance(notCompound)) {
                    notCompound = ((List<?>) NBTListData.get(notCompound)).get((int) key);
                } else {
                    return getNBTVar(notCompound);
                }
            }
            if (notCompound == null) {
                return null;
            } else if (getNMSClass("NBTTagList").isInstance(notCompound)) {
                return getTags(notCompound);
            } else if (getNMSClass("NBTTagCompound").isInstance(notCompound)) {
                return getTags(notCompound);
            } else {
                return getNBTVar(notCompound);
            }
        }

        @SuppressWarnings("unchecked")
        private static Object getTags(Object tag) {
            Map<Object, Object> tags = new HashMap<>();
            try {
                if (getNMSClass("NBTTagCompound").isInstance(tag)) {
                    Map<String, Object> tagCompound = (Map<String, Object>) NBTCompoundMap.get(tag);
                    for (String key : tagCompound.keySet()) {
                        Object value = tagCompound.get(key);
                        if (getNMSClass("NBTTagEnd").isInstance(value)) {
                            continue;
                        }
                        tags.put(key, getTag(value));
                    }
                } else if (getNMSClass("NBTTagList").isInstance(tag)) {
                    List<Object> tagList = (List<Object>) NBTListData.get(tag);
                    for (int index = 0; index < tagList.size(); index++) {
                        Object value = tagList.get(index);
                        if (getNMSClass("NBTTagEnd").isInstance(value)) {
                            continue;
                        }
                        tags.put(index, getTag(value));
                    }
                } else {
                    return getNBTVar(tag);
                }
                return tags;
            } catch (Exception e) {
                e.printStackTrace();
                return tags;
            }
        }

        public static final class NBTCompound {

            protected final Object tag;

            protected NBTCompound(@Nonnull Object tag) {
                this.tag = tag;
            }

            public void set(Object value, Object... keys) {
                try {
                    setTag(tag, value, keys);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            public String toJson() {
                return tag.toString();
            }

            public static NBTCompound fromJson(String json) {
                try {
                    return new NBTCompound(getMethod("loadNBTTagCompound").invoke(null, json));
                } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    e.printStackTrace();
                    return null;
                }
            }

            @Override
            public String toString() {
                return tag.toString();
            }

            @Override
            public int hashCode() {
                return tag.hashCode();
            }

            @Override
            public boolean equals(Object obj) {
                if (this == obj)
                    return true;
                if (obj == null)
                    return false;
                if (getClass() != obj.getClass())
                    return false;
                NBTCompound other = (NBTCompound) obj;
                if (tag == null) {
                    if (other.tag != null)
                        return false;
                } else if (!tag.equals(other.tag))
                    return false;
                return true;
            }
        }

        public enum MinecraftVersion {
            v1_8("1_8", 0),
            v1_9("1_9", 1),
            v1_10("1_10", 2),
            v1_11("1_11", 3),
            v1_12("1_12", 4),
            v1_13("1_13", 5),
            v1_14("1_14", 6),
            v1_15("1_15", 7),
            v1_16("1_16", 8),
            v1_17("1_17", 9),
            v1_18("1_18", 10),
            v1_19("1_19", 11);

            private int order;
            private String key;

            MinecraftVersion(String key, int v) {
                this.key = key;
                order = v;
            }

            public boolean greaterThanOrEqualTo(MinecraftVersion other) {
                return order >= other.order;
            }

            public boolean lessThanOrEqualTo(MinecraftVersion other) {
                return order <= other.order;
            }

            public static MinecraftVersion get(String v) {
                for (MinecraftVersion k : MinecraftVersion.values()) {
                    if (v.contains(k.key)) {
                        return k;
                    }
                }
                return null;
            }
        }
    }


}
