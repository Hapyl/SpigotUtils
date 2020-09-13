// Your package here //

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

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.PatternSyntaxException;

/**
 * This class used for building complex ItemStack easier.
 *
 * @author hapyl
 * @version 2.1
 */

public final class ItemBuilder implements Listener {

    /**
     * Click event and Id System requires NBTEditor class!
     * If you don't want them, just remove these methods and comment errors.
     * https://github.com/Hapyl/SpigotUtils/blob/master/NBTEditor.java
     * 
     * Also, don't forget to register events for this class!
     */

    public static Map<String, ItemStack> holder = new HashMap<>();
    public static final Set<ItemBuilder> executableStorage = new HashSet<>();

    private ItemStack item;
    private ItemMeta meta;
    private String id;
    private Set<ItemAction> functions = new HashSet<>();

    public ItemBuilder() {
        // for the event registration
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
     * Requires NBTEditor to work.
     * https://github.com/Hapyl/SpigotUtils/blob/master/NBTEditor.java
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
     * Requires NBTEditor to work.
     * https://github.com/Hapyl/SpigotUtils/blob/master/NBTEditor.java
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
        return NBTEditor.getString(item, "custom_id");
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
    public ItemBuilder applyDefaultSetting() {
        this.meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
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
            this.item = NBTEditor.set(item, this.id, "custom_id");
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

    public String getName() {
        return this.meta.getDisplayName();
    }

    @Nullable
    public List<String> getLore() {
        return this.meta.getLore();
    }

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

    public int getAmount() {
        return this.item.getAmount();
    }

    public Map<Enchantment, Integer> getEnchants() {
        return this.meta.getEnchants();
    }

    public boolean isUnbreakable() {
        return this.meta.isUnbreakable();
    }

    public int getRepairCost() {
        return ((Repairable) this.meta).getRepairCost();
    }

    public Color getLeatherColor() {
        return ((LeatherArmorMeta) this.meta).getColor();
    }

    @Nullable
    public String getHeadTexture() {
        try {
            return (String) this.meta.getClass().getDeclaredField("profile").get(this.meta);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Set<ItemFlag> getFlags() {
        return this.meta.getItemFlags();
    }

    public Multimap<Attribute, AttributeModifier> getAttributes() {
        return this.meta.getAttributeModifiers();
    }

    public double getPureDamage() {
        double last = 0;
        for (AttributeModifier t : getAttributes().get(Attribute.GENERIC_ATTACK_DAMAGE)) {
            last = t.getAmount();
        }
        return last;
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
    private void handleClick(PlayerInteractEvent ev) {

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
                        if (func.hasAction(action)) func.execute(player);
                    }
                }
            });
            hash.clear();
        }
    }

}
