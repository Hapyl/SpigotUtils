// Your package here //

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

public class HologramBuilder implements Listener {

    private Object entity;
    private int id;
    private Location loc;
    private String displayName;

    /**
     * Constructor for HologramBuilder.
     * Allows to create invisible ArmorStand with Custom Name using packets!
     * [!] Tested on 1.14.4 [!] May not work on old versions. But still should work [!]
     * <p>
     * Examples:
     * new HologramBuilder(player.getLocation(), "&cHello!").show(player); @ Creates Holo on player's location and showing it to player.
     *
     * @param loc         Location.
     * @param displayName Name of the holo. Supports '&' as color char.
     */
    public HologramBuilder(Location loc, String displayName) {
        this.loc = loc;
        this.displayName = displayName;
        build();
    }

    /**
     * Shows Holo to specific player.
     *
     * @param player who to show.
     */
    public void show(Player player) {
        try {
            sendPacket(player, getNetClass("PacketPlayOutSpawnEntityLiving").getConstructor(getNetClass("EntityLiving")).newInstance(this.entity));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Shows holo to specific player for certain amount of ticks.
     *
     * @param player - Player to show.
     * @param ticks  - Remove in.
     */
    public void showFor(Player player, int ticks) {
        final HologramBuilder holo = this;
        holo.show(player);
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                holo.hide(player);
            }
        }, ticks * 50);
    }

    /**
     * Hides Holo from specific player.
     *
     * @param player who to hide.
     */
    public HologramBuilder hide(Player player) {
        try {
            sendPacket(player, getNetClass("PacketPlayOutEntityDestroy").getConstructor(new Class<?>[]{int[].class}).newInstance(new int[]{this.id}));
            return this;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void setLocation(Location loc) {
        try {
            getNetClass("EntityArmorStand").getMethod("setLocation", double.class, double.class, double.class, float.class, float.class).invoke(this.entity, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }

    }

    public Object getHologram() {
        return this.entity;
    }

    /**
     * Main build method.
     * Creating ArmorStand, setting all stuff here.
     */
    private void build() {
        try {

            this.entity = getNetClass("EntityArmorStand").getConstructor(getNetClass("World"), double.class, double.class, double.class).newInstance(getNetWorld(this.loc.getWorld()), this.loc.getX(), this.loc.getY(), this.loc.getZ());
            this.id = (int) getNetClass("Entity").getMethod("getId").invoke(this.entity);

            Class<?> entityClass = getNetClass("Entity");
            Class<?> entityArmorStand = getNetClass("EntityArmorStand");

            entityArmorStand.getDeclaredMethod("setMarker", boolean.class).invoke(this.entity, true);
            entityArmorStand.getDeclaredMethod("setSmall", boolean.class).invoke(this.entity, true);

            entityClass.getDeclaredMethod("setInvisible", boolean.class).invoke(this.entity, true);
            entityClass.getDeclaredMethod("setCustomNameVisible", boolean.class).invoke(this.entity, true);
            entityClass.getDeclaredMethod("setCustomName", getNetClass("IChatBaseComponent")).invoke(this.entity, stringToIChat(this.displayName));

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public int getId(Object entity) {
        try {
            return (int) getNetClass("Entity").getMethod("getId").invoke(entity);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private static String version() {
        final String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    private static Class<?> getNetClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + version() + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object stringToIChat(String s) {
        try {
            return getNetClass("IChatBaseComponent$ChatSerializer").getMethod("a", String.class)
                    .invoke(getNetClass("IChatBaseComponent$ChatSerializer"), "{\"text\":\"" + ChatColor.translateAlternateColorCodes('&', s) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static Object getNetWorld(World bukkitWorld) {
        try {
            return bukkitWorld.getClass().getMethod("getHandle").invoke(bukkitWorld);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void sendPacket(Player player, Object packet) {
        try {
            Object getHandle = player.getClass().getMethod("getHandle").invoke(player);
            Object playerConnecton = getHandle.getClass().getField("playerConnection").get(getHandle);
            playerConnecton.getClass().getMethod("sendPacket", getNetClass("Packet")).invoke(playerConnecton, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
