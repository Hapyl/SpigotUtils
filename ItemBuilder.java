package your.path.should.be.here;

import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.*;
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
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.PatternSyntaxException;

/**
 * This class used for building complex ItemStack easier.
 *
 * @author hapyl
 * @version 3.0
 * <p>
 * Updates: > 3.0: Rework nbt methods, now using persistent data container for less code and more stability. RIP offlection
 */

public final class ItemBuilder implements Listener {

	private static final String                 PLUGIN_ID_PATH    = "ItemBuilderId";
	public static        Map<String, ItemStack> holder            = new HashMap<>();
	public static final  Set<ItemBuilder>       executableStorage = new HashSet<>();

	private ItemStack         item;
	private ItemMeta          meta;
	private int               cd;
	private Predicate<Player> predicate;
	private String            id, error;
	private Set<ItemAction> functions = new HashSet<>();

	private static Plugin plugin;

	private ItemBuilder() {

	}

	// use this for event registration
	public ItemBuilder(org.bukkit.plugin.Plugin javaPlugin) {
		plugin = javaPlugin;
		javaPlugin.getServer().getPluginManager().registerEvents(this, plugin);
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
	 * Creates an ItemStack from existing ItemStack.
	 *
	 * @param stack - ItemStack.
	 */
	public ItemBuilder(ItemStack stack) {
		this.item = stack.clone();
		this.meta = stack.getItemMeta().clone();
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
	 * Creates an ItemStack from existing ItemStack and registers with ID.
	 *
	 * @param stack - ItemStack.
	 * @param id    - Unique Id.
	 */
	public ItemBuilder(ItemStack stack, String id) {

		if (holder.containsKey(id)) {
			throw new ItemBuilderException(String
					.format("ItemStack with id '%s' already registered! Use 'toItemStack' if you with clone the item.", id));
		}

		this.item = stack;
		this.meta = stack.getItemMeta();
		this.id = id;
	}

	/**
	 * Applies certain action if predicate is true.
	 *
	 * @param predicate - Boolean predicate.
	 * @param action    - Action to apply.
	 */
	public ItemBuilder predicate(boolean predicate, Consumer<ItemBuilder> action) {
		if (predicate) {
			action.accept(this);
		}
		return this;
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

	public static void setName(ItemStack item, String name) {
		final ItemMeta meta = item.getItemMeta();
		meta.setDisplayName(colorize(name));
		item.setItemMeta(meta);
	}

	public static void setLore(ItemStack item, String lore) {
		final ItemMeta meta = item.getItemMeta();
		meta.setLore(Collections.singletonList(lore));
		item.setItemMeta(meta);
	}

	public ItemBuilder setItemMeta(ItemMeta meta) {
		this.meta = meta;
		return this;
	}

	public ItemBuilder clone() {
		if (!this.id.isEmpty()) {
			throw new UnsupportedOperationException("Clone does not support ID's!");
		}
		return new ItemBuilder(this.item).setItemMeta(this.meta);
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
	 * Adds cooldown to the item, requires ClickEvent on the item to work.
	 *
	 * @param ticks - Ticks of cooldown.
	 * @return - ItemBuilder.
	 */
	public ItemBuilder withCooldown(int ticks) {
		withCooldown(ticks, null);
		return this;
	}

	/**
	 * Adds cooldown to the item, requires ClickEvent on the item to work.
	 *
	 * @param ticks     - Ticks of cooldown.
	 * @param predicate - Predicate of player, if test failed, click event will not fire.
	 * @return ItemBuilder.
	 */
	public ItemBuilder withCooldown(int ticks, Predicate<Player> predicate) {
		withCooldown(ticks, predicate, "&cCannot use that!");
		return this;
	}

	/**
	 * Adds cooldown to the item, requires ClickEvent on the item to work.
	 *
	 * @param ticks        - Ticks of cooldown.
	 * @param predicate    - Predicate of player, if test failed, click event will not fire.
	 * @param errorMessage - A string message that will be send to the player whenever predicate if failed.
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
		final ItemMeta iMeta = item.getItemMeta();
		if (iMeta == null) {
			return "null";
		}
		return iMeta.getPersistentDataContainer().get(new NamespacedKey(plugin, PLUGIN_ID_PATH), PersistentDataType.STRING);
	}

	/**
	 * Check if item has id.
	 *
	 * @param item - Item.
	 * @param id   - Id.
	 * @return true if item has id.
	 */
	public static boolean itemHasID(ItemStack item, String id) {
		return itemHasID(item) && getItemID(item).equalsIgnoreCase(id.toLowerCase());
	}

	public static boolean itemContainsId(ItemStack item, String id) {
		return itemHasID(item) && getItemID(item).contains(id.toLowerCase());
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
	 * Adds an nbt value just like 'custom_id' with custom path and value.
	 *
	 * @param path  - Path
	 * @param value - Value
	 */
	public ItemBuilder addNbt(String path, Object value) {
		if (value instanceof String) {
			this.setPersistentData(path, PersistentDataType.STRING, (String) value);
		}
		if (value instanceof Byte) {
			this.setPersistentData(path, PersistentDataType.BYTE, (byte) value);
		}
		if (value instanceof Short) {
			this.setPersistentData(path, PersistentDataType.SHORT, (short) value);
		}
		if (value instanceof Integer) {
			this.setPersistentData(path, PersistentDataType.INTEGER, (int) value);
		}
		if (value instanceof Long) {
			this.setPersistentData(path, PersistentDataType.LONG, (long) value);
		}
		if (value instanceof Float) {
			this.setPersistentData(path, PersistentDataType.FLOAT, (float) value);
		}
		if (value instanceof Double) {
			this.setPersistentData(path, PersistentDataType.DOUBLE, (double) value);
		}
		return this;
	}

	public <T> T getNbt(String path, PersistentDataType<T, T> value) {
		return this.getPersistentData(path, value);
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
	 * Sets the smart lore; Smart is lore is splitting automatically after certain amount of characters.
	 *
	 * @param lore      - The lore
	 * @param separator - Amount of characters to split after; will not split if word isn't finished.
	 */
	public ItemBuilder setSmartLore(String lore, final int separator) {
		this.meta.setLore(splitAfter(lore, separator));
		return this;
	}

	public ItemBuilder setLore(List<String> lore) {
		this.meta.setLore(lore);
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
		this.addSmartLore(lore, "&7", splitAfter);
		return this;
	}

	public ItemBuilder addSmartLore(String lore, String prefixText) {
		this.addSmartLore(lore, prefixText, 30);
		return this;
	}

	public ItemBuilder addSmartLore(String lore) {
		addSmartLore(lore, 30);
		return this;
	}

	public ItemBuilder setSmartLore(String lore, String prefixColor) {
		this.setSmartLore(lore, prefixColor, 30);
		return this;
	}

	public ItemBuilder setSmartLore(String lore, String prefixColor, int splitAfter) {
		this.meta.setLore(splitAfter(prefixColor, lore, splitAfter));
		return this;
	}

	public ItemBuilder addSmartLore(String lore, String prefixText, int splitAfter) {
		List<String> metaLore = this.meta.getLore() != null ? this.meta.getLore() : Lists.newArrayList();
		metaLore.addAll(splitAfter(prefixText, lore, splitAfter));
		this.meta.setLore(metaLore);
		return this;
	}

	public ItemBuilder setLore(int line, String lore) {
		List<String> oldLore = this.meta.getLore() == null ? Lists.newArrayList() : this.meta.getLore();
		oldLore.set(line, colorize(lore));
		this.meta.setLore(oldLore);
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
		List<String> metaLore = this.meta.getLore() != null ? this.meta.getLore() : Lists.newArrayList();
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

	public ItemBuilder addLore() {
		return this.addLore("");
	}

	/**
	 * Sets the lore of the item with custom separator character. Keep in mind that certain characters cannot be used as separator, in that case an
	 * error message will be sent.
	 *
	 * @param lore      - The lore
	 * @param separator - Split character
	 */
	public ItemBuilder setLore(final String lore, final String separator) {
		try {
			this.meta.setLore(Arrays.asList(colorize(lore).split(separator)));
		}
		catch (PatternSyntaxException ex) {
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
	 * Applies default setting to the Item, such as: - Binding Curse - Makes the item unbreakable - Hides all flags.
	 */
	public ItemBuilder applyDefaultSettings() {
		return applyDefaultSettings(true);
	}

	public ItemBuilder applyDefaultSettings(boolean applyCurse) {
		if (applyCurse) this.meta.addEnchant(Enchantment.BINDING_CURSE, 1, true);
		this.meta.setUnbreakable(true);
		this.meta.addItemFlags(ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_PLACED_ON,
				ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
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
	 * Sets the potion meta of the item. Item must be POTION, SPLASH_POTION or LINGERING_POTION for this to work.
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
	 * Sets the leather armor color to the given color. Item must be leather armor for this to work.
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
	 * Sets the head texture of the item. Item must be PLAYER_HEAD in order for this to work.
	 *
	 * @param base64 - Base64 value of the texture. Use minecraft-heads to get head textures.
	 */
	public ItemBuilder setHeadTexture(String base64) {

		GameProfile profile = new GameProfile(UUID.randomUUID(), "");
		profile.getProperties().put("textures", new Property("textures", base64));

		try {
			Field f = this.meta.getClass().getDeclaredField("profile");
			f.setAccessible(true);
			f.set(this.meta, profile);
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}

		return this;
	}

	/**
	 * Sets the skull owner of the item. Item must be PLAYER_HEAD in order for this to work.
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
	 * Sets pure damage of the item. Pure damage will ignore already existing damage of the item and override it. Enchantments will still affect the
	 * item damage.
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
		this.meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DESTROYS,
				ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
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
	 * This will just apply item meta, without adding custom id's and etc.
	 */
	public ItemStack toItemStack() {
		this.item.setItemMeta(this.meta);
		return this.item;
	}

	/**
	 * Builds the item and returns ItemStack.
	 *
	 * @return Final, shiny ItemStack.
	 */
	public ItemStack build() {
		if (this.id != null) {

			setPersistentData(PLUGIN_ID_PATH, PersistentDataType.STRING, this.id);

			if (holder.containsKey(this.id)) {
				throw new ItemBuilderException("This item's ID already registered! Use 'toItemStack()' if you want to clone this item!");
			}

			holder.put(this.id, this.item);
			if (!this.functions.isEmpty()) {
				executableStorage.add(this);
			}
		}

		// This executes if there no Id and function added.
		else if (!this.functions.isEmpty()) {
			throw new ItemBuilderException("ID is required to use this.");
		}

		this.item.setItemMeta(this.meta);
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
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
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

	private static List<String> splitAfter(String linePrefix, String text, int maxChars) {

		List<String> list = new ArrayList<>();
		String line = "";
		int counter = 0;

		for (int i = 0; i < text.length(); i++) {
			char c = text.charAt(i);
			final boolean checkLast = c == text.charAt(text.length() - 1);
			line = line.concat(c + "");
			counter++;
			// manual split
			if (c == '_' && text.charAt(i + 1) == '_') {
				list.add(colorize(linePrefix + line.substring(0, line.length() - 1).trim()));
				line = "";
				counter = 0;
				i++;
				continue;
			}
			if (counter >= maxChars || i == text.length() - 1) {
				if (c == ' ' || checkLast) {
					list.add(colorize(linePrefix + line.trim()));
					line = "";
					counter = 0;
				}
			}
		}

		return list;
	}

	public static List<String> splitAfter(String text, int max) {
		return splitAfter("&7", text, max);
	}

	private static String colorize(String s) {
		return ChatColor.translateAlternateColorCodes('&', s);
	}

	// this used for addClickEvent
	private static class ItemAction {

		private Set<Action>      actions = new HashSet<>();
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

	@EventHandler()
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

								player.setCooldown(builder.item.getType(), builder.cd);

							}
							func.execute(player);
						}
					}
				}
			});
			hash.clear();
		}
	}


	public <T> ItemBuilder setPersistentData(String path, PersistentDataType<T, T> type, T value) {
		try {
			this.meta.getPersistentDataContainer().set(new NamespacedKey(plugin, path), type, value);
		}
		catch (IllegalArgumentException er) {
			Bukkit.broadcastMessage(ChatColor.RED + "An error occurred whilst trying to perform this action. Check the console!");
			throw new ItemBuilderException
					("Plugin call before plugin initiated. Make sure to register ItemBuilder BEFORE you register commands, events etc!");
		}
		return this;
	}

	public <T> boolean hasPersistentData(String path, PersistentDataType<T, T> type) {
		return this.meta.getPersistentDataContainer().has(new NamespacedKey(plugin, path), type);
	}

	public <T> T getPersistentData(String path, PersistentDataType<T, T> type) {
		return this.meta.getPersistentDataContainer().get(new NamespacedKey(plugin, path), type);
	}

	/**
	 * Adds an enchanting glint without enchants.
	 */
	public ItemBuilder glow() {
		this.addEnchant(Enchantment.LUCK, 1);
		this.hideFlag(ItemFlag.HIDE_ENCHANTS);
		return this;
	}


	public static ItemBuilder fromItemStack(ItemStack stack) {
		return new ItemBuilder(stack);
	}

	private static class ItemBuilderException extends RuntimeException {
		private ItemBuilderException() {
			super();
		}

		private ItemBuilderException(String args) {
			super(args);
		}
	}

}
