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
import java.util.Arrays;
import java.util.UUID;

public class ItemBuilder {

    private ItemStack item;
    private ItemMeta meta;

    /**
     * Constructor for ItemBuilder.
     *
     * Example:
     *  new ItemBuilder(Material.STONE).setName("&7Stone").addLore("&7This stone is stone.____&cYes, it is.").build(); @ Creates an stone with name and lore.
     *
     * @param material of item to create.
     */
    public ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
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
     * Sets lore for the item.
     *
     * @param lore String. (Split lines using "__", also supports '&' as color code.)
     */
    public ItemBuilder addLore(String lore) {
        this.meta.setLore(Arrays.asList(f(lore).split("__")));
        return this;
    }

    /**
     * Sets name of the item.
     *
     * @param name String. (Supports '&' as color code.)
     */
    public ItemBuilder setName(String name) {
        this.meta.setDisplayName(f(name));
        return this;
    }

    /**
     * Adds an enchant to the item.
     * More than one enchants can be added.
     * @param ench Enchantment
     * @param lvl Level of the enchantment.
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
     * @param type Effect type.
     * @param lvl Effect level.
     * @param duration Duration in ticks.
     * @param color Color or the potion.
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
     * @param color Color of the armor.
     */
    public ItemBuilder setLeatherArmorColor(Color color) {

        Material m = this.item.getType();

        if (m == Material.LEATHER_BOOTS || m == Material.LEATHER_CHESTPLATE || m == Material.LEATHER_LEGGINGS || m == Material.LEATHER_HELMET) {

            LeatherArmorMeta meta = (LeatherArmorMeta) this.meta;
            meta.setColor(color);
            return this;
        }

        return null;

    }

    /**
     * [!] Works only if item type is player head. [!]
     * Allows to create custom head wih base64 value.
     * You can get base64 value in minecraft-heads site, 2nd value from the end. The long random character value String.
     *
     * @param base64 base key.
     */
    @Deprecated
    public ItemBuilder setBase64(String base64) {

        if (this.item.getType() == Material.PLAYER_HEAD) {
            UUID hash = new UUID(base64.hashCode(), base64.hashCode());
            Bukkit.getUnsafe().modifyItemStack(this.item, "{SkullOwner:{Id:\"" + hash + "\",Properties:{textures:[{Value:\"" + base64 + "\"}]}}}");
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
    public Item setHeadTexture(String base64) {

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
     * @param a Attribute to add.
     * @param amount Amount in double.
     * @param operation Operation.
     * @param slot Slot.
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
     * (DEPRECATED)
     * Sets damage of the item.
     * @param dura Dura
     */
    @Deprecated
    public ItemBuilder setDamage(short dura) {
        this.item.setDurability(dura);
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
     * Ignore this.
     */
    private String f(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

}
