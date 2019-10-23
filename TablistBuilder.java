import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;

public class TablistBuilder {

    private String header;
    private String footer;
    private Object packet;

    /**
     * Creates a Tablist Packet with header and footer.
     * @param header Header (Above nicknames) Supports '&' as color char!
     * @param footer Footer (Below nicknames) Also supports '&' as color char!
     */
    public TablistBuilder(String header, String footer) {
        this.header = header;
        this.footer = footer;
        build(true);
    }

    /**
     * Sets header of the current Tablist Packet.
     * @param s String to set (You not gonna believe me, but this one is also supports '&'!)
     */
    public TablistBuilder setHeader(String s) {
        this.header = s;
        build(false);
        return this;
    }

    /**
     * Sets footer of the current Tablist Packet.
     * @param s String
     */
    public TablistBuilder setFooter(String s) {
        this.footer = s;
        build(false);
        return this;
    }

    /**
     * Shows Tablist to specific player.
     * @param player who to show.
     */
    public void show(Player player) {
        sendPacket(player, this.packet);
    }

    /**
     * Shows Tablist to all online players.
     */
    public void show() {
        Bukkit.getOnlinePlayers().forEach(this::show);
    }

    /**
     * Some random piece of code that does absolutely nothing.
     */
    private void build(boolean a) {
        try {

            if (a) {
                this.packet = getNetClass("PacketPlayOutPlayerListHeaderFooter").getDeclaredConstructors()[0].newInstance();
            }

            this.packet.getClass().getDeclaredField("header").set(this.packet, stringToIChat(this.header));
            this.packet.getClass().getDeclaredField("footer").set(this.packet, stringToIChat(this.footer));

        } catch (NoSuchFieldException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Reflection Helpers
     */

    private String ver() {
        String a = Bukkit.getServer().getClass().getPackage().getName();
        return a.substring(a.lastIndexOf(".") + 1);
    }

    private Class<?> getNetClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + ver() + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Object stringToIChat(String s) {
        try {
            return getNetClass("IChatBaseComponent$ChatSerializer").getMethod("a", String.class)
                    .invoke(getNetClass("IChatBaseComponent$ChatSerializer"), "{\"text\":\"" + ChatColor.translateAlternateColorCodes('&', s) + "\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendPacket(Player player, Object packet) {
        try {
            Object craftPlayer = player.getClass().getMethod("getHandle").invoke(player);
            Object connection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
            connection.getClass().getMethod("sendPacket", getNetClass("Packet")).invoke(connection, packet);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            e.printStackTrace();
        }
    }

}
