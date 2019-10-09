import net.minecraft.server.v1_14_R1.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_14_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

public class EntityBuilder {

    private static EntityLiving entity;

    /**
     * Constructor for Entity.
     * <p>
     * Example:
     *  new EntityBuilder(new EntityArmorStand(EntityBuilder.getWorld(player), 0.0, 0.0, 0.0)).setCustomName("&c&lRED NAME").create(player)
     *  @ Creates Armor Stand with name on 0,0,0 coordinates for player.
     *  @ Only player that entity created for can see this entity.
     *
     * @param en NMSEntityLiving.
     */

    public EntityBuilder(EntityLiving en) {
        entity = en;
    }

    /**
     * @return Current EntityLiving.
     */
    public static EntityLiving get() {
        return entity;
    }

    /**
     * Create entity for player.
     * [!] Must be executed as last argument [!]
     *
     * @param player BukkitPlayer.
     */
    public EntityBuilder create(Player player) {
        sendPacket((CraftPlayer) player, new PacketPlayOutSpawnEntityLiving(entity));
        return this;
    }

    /**
     * Returns World that LivingEntity needs to build;
     * @param player Player who world will be returned.
     * @return World.
     */
    public static World getWorld(Player player) {
        return ((CraftPlayer) player).getHandle().getWorld();
    }

    /**
     * Create entity for player on specific location.
     *
     * @param player BukkitPlayer.
     * @param loc    BukkitLocation.
     */
    public EntityBuilder create(Player player, Location loc) {
        entity.setLocation(loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        sendPacket((CraftPlayer) player, new PacketPlayOutSpawnEntityLiving(entity));
        return this;
    }

    /**
     * Sets custom name of entity by String. (Supports '&' as chat color.)
     *
     * @param name Name
     */
    public EntityBuilder setCustomName(String name) {
        entity.setCustomName(n(name));
        entity.setCustomNameVisible(true);
        return this;
    }

    /**
     * Removes entity for BukkitPlayer.
     *
     * @param bukkitPlayer Player.
     */
    public EntityBuilder remove(Player bukkitPlayer) {
        sendPacket((CraftPlayer) bukkitPlayer, new PacketPlayOutEntityDestroy(entity.getId()));
        return this;
    }

    /**
     * Convert String to IChat {"text"}.
     *
     * @param s String to convert.
     * @return IChat.
     */
    private static IChatBaseComponent n(String s) {
        return IChatBaseComponent.ChatSerializer.a("{\"text\":\"" + ChatColor.translateAlternateColorCodes('&', s) + "\"}");
    }

    /**
     * Sends a packet to the CraftPlayer.
     *
     * @param a CraftPlayer.
     * @param b Packet.
     */
    private static void sendPacket(CraftPlayer a, Packet b) {
        a.getHandle().playerConnection.sendPacket(b);
    }

}
