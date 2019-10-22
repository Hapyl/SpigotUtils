import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.InvocationTargetException;
import java.util.Timer;
import java.util.TimerTask;

public class BlockBreakAnimation {

    private Block block;
    private int stage;

    /**
     * Allows to shows certain player animation of breaking
     * a block without break the block! (WOW)
     *
     * Example:
     *  new BlockBreakAnimation(block, 2).play(player); @ Play animation on block with 2nd stage for player
     *
     * @param block BukkitBlock to break.
     * @param stage Stage it's just an animation.. take i think? Min 0, Max 9
     */
    public BlockBreakAnimation(Block block, int stage) {
        this.block = block;
        this.stage = stage;
    }

    /**
     * Plays animation on the block in stage for current player.
     * @param player to display to.
     */
    public void play(final Player player) {
        try {
            Object packet = getNetClass("PacketPlayOutBlockBreakAnimation").getDeclaredConstructors()[1].newInstance(0, getBlockPos(), this.stage);
            sendPacket(player, packet);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * Playing full animation on the block for current player.
     * Overrides stage of animation.
     * @param player to display to.
     * @param delay between animations in Minecraft ticks.
     */
    public void playFullAnimation(final Player player, int delay /* in Minecraft ticks */) {
        new Timer().schedule(new TimerTask() {
            int l = 0;

            @Override
            public void run() {
                stage = l;
                play(player);
                if (l++ > 9) {
                    this.cancel();
                }
            }
        }, 0, delay * 50);
    }

    /**
     * Playing full animation on the block for current player.
     * Overrides stage of animation.
     * @param player to display to.
     * @param millis between animations in millis.
     */
    public void playFullAnimation(final Player player, long millis /* in millis */) {
        new Timer().schedule(new TimerTask() {
            int l = 0;

            @Override
            public void run() {
                stage = l;
                play(player);
                if (l++ > 9) {
                    this.cancel();
                }
            }
        }, 0, millis);
    }

    /**
     * Everything down below it's just a reflection helpers.
     */

    private Object getBlockPos() {
        Location loc = this.block.getLocation();
        try {
            return getNetClass("BlockPosition").getDeclaredConstructor(double.class, double.class, double.class).newInstance(loc.getX(), loc.getY(), loc.getZ());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
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

    private static void sendPacket(Player player, Object packet) {
        try {
            Object getHandle = player.getClass().getMethod("getHandle").invoke(player);
            Object c = getHandle.getClass().getField("playerConnection").get(getHandle);
            c.getClass().getMethod("sendPacket", getNetClass("Packet")).invoke(c, packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
