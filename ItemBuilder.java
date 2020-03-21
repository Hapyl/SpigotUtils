// Your package here //

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.*;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

public final class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    /**
     * Constructor for ItemBuilder.
     * <p>
     * Example:
     * new ItemBuilder(Material.STONE).setName("&7Stone").addLore("&7This stone is stone.____&cYes, it is.").build(); @ Creates an stone with name and lore.
     *
     * @param material of item to create.
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    /**
     * Constructor for ItemBuilder with existed ItemStack.
     */
    public ItemBuilder(ItemStack stack) {
        this.item = stack;
        this.meta = stack.getItemMeta();
    }

    /**
     * Sets amount of item.
     *
     * @param amount Integer.
     */
    public ItemBuilder setAmount(int amount) {
        this.item.setAmount(amount);
        return this;
    }

    /**
     * Sets "smart" lore. Splits automatically after certain amount of worlds.
     *
     * @param lore      Lore
     * @param separator Amount of chars to split after.
     */
    public ItemBuilder setSmartLore(String lore, final int separator) {
        this.meta.setLore(splitAfter(lore, separator));
        return this;
    }

    /**
     * Add another smart lore.
     */
    public ItemBuilder addSmartLore(String lore, final int splitAfter) {
        final List<String> list = this.meta.hasLore() ? meta.getLore() : Lists.newArrayList();
        list.addAll(splitAfter(lore, splitAfter));
        this.meta.setLore(list);
        return this;
    }
    
    /**
     * Sets lore for the item.
     *
     * @param lore String. (Split lines using "__", also supports '&' as color code.)
     */
    public ItemBuilder setLore(String lore) {
        this.setLore(lore, "__");
        return this;
    }

    /**
     * Adds lore to existed item.
     *
     * @param lore Lore to add
     */
    public ItemBuilder addLore(final String lore) {
        List<String> metaLore = this.meta.hasLore() ? this.meta.getLore() : Lists.newArrayList();
        for (String value : lore.split("__")) metaLore.add(colorize(value));
        this.meta.setLore(metaLore);
        return this;
    }

    /**
     * Sets lore with custom separator. String will be split after separator.
     *
     * @param lore      Lore to add.
     * @param separator <-
     *                  [IMPORTANT] Don't use Java's regex chars like % or * etc, might throw exception!
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
     * Removes lore.
     */
    public ItemBuilder removeLore() {
        if (this.meta.getLore() != null)
            this.meta.setLore(null);
        return this;
    }

    /**
     * Remove specific line from the lore.
     *
     * @param line line to remove.
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
     * Sets name of the item.
     *
     * @param name String. (Supports '&' as color code.)
     */
    public ItemBuilder setName(String name) {
        this.meta.setDisplayName(colorize(name));
        return this;
    }

    /**
     * Adds an enchant to the item.
     * More than one enchants can be added.
     *
     * @param ench Enchantment
     * @param lvl  Level of the enchantment.
     */
    public ItemBuilder addEnchant(Enchantment ench, int lvl) {
        this.meta.addEnchant(ench, lvl, true);
        return this;
    }

    /**
     * Makes item unbreakable.
     */
    public ItemBuilder setUnbreakable() {
        this.meta.setUnbreakable(true);
        return this;
    }

    /**
     * Sets item repair cost. High value makes item unrepairable.
     * (Sometimes may not work)
     *
     * @param valueInLevels value
     */
    public ItemBuilder setRepairCost(int valueInLevels) {

        Repairable r = (Repairable) this.meta;
        r.setRepairCost(valueInLevels);
        return this;

    }

    /**
     * [!] Works only if item type is a potion. [!]
     * Adds potion meta to potion.
     * Supports only 1 potion effect. Will update it later to support multiple.
     *
     * @param type     Effect type.
     * @param lvl      Effect level.
     * @param duration Duration in ticks.
     * @param color    Color or the potion.
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
     * [!] Works only if item type is leather armor. [!]
     * Sets leather armor color.
     *
     * @param color Color of the armor.
     */
    public ItemBuilder setLeatherArmorColor(Color color) {
        final Material m = this.item.getType();
        if (m == Material.LEATHER_BOOTS || m == Material.LEATHER_CHESTPLATE || m == Material.LEATHER_LEGGINGS || m == Material.LEATHER_HELMET) {

            LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
            meta.setColor(color);
            return this;
        }
        return null;

    }

    /**
     * [!] Works only if item type is player head. [!]
     * New method using reflection. Use this one
     * if setBase64() is not working.
     *
     * @param base64 base key.
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
     * [!] Works only if item type is player head. [!]
     * Sets skull owner.
     *
     * @param owner name of the player.
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
     * Adds attribute to the item.
     *
     * @param a         Attribute to add.
     * @param amount    Amount in double.
     * @param operation Operation.
     * @param slot      Slot.
     */
    public ItemBuilder addAttribute(Attribute a, double amount, AttributeModifier.Operation operation, EquipmentSlot slot) {
        this.meta.addAttributeModifier(a, new AttributeModifier(UUID.randomUUID(), a.toString(), amount, operation, slot));
        return this;
    }

    /**
     * Hide certain flag on the item, supports multiple.
     *
     * @param flag Flag[s] to add.
     */
    public ItemBuilder hideFlag(ItemFlag... flag) {
        this.meta.addItemFlags(flag);
        return this;
    }

    /**
     * Hides all flag on the item.
     */
    public ItemBuilder hideFlags() {
        this.meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_PLACED_ON, ItemFlag.HIDE_DESTROYS, ItemFlag.HIDE_POTION_EFFECTS, ItemFlag.HIDE_UNBREAKABLE);
        return this;
    }

    /**
     * Sets durability of item.
     *
     * @param dura Durability
     */
    public ItemBuilder setDurability(int dura) {
        Damageable meta = (Damageable) this.meta;
        meta.setDamage(dura);
        return this;
    }

    /**
     * Build the item.
     * Must be executed as last argument.
     *
     * @return Final item.
     */
    public ItemStack build() {
        this.item.setItemMeta(this.meta);
        return item;
    }

    /**
     * Helpers.
     */
    private String colorize(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static List<String> splitAfter(String clr, String text, int max) {
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
        return list;
    }

    public static List<String> splitAfter(String text, int max) {
        return splitAfter("&7", text, max);
    }

}
