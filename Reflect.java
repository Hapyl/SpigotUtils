your package goes here

!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
THIS WILL NOT WORK ON PAPER BECAUSE IT DOESNT HAV
Int2ObjectMap CRAFTBUKKIT CLASS, USE SPIGOT
!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.omg.SendingContext.RunTime;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public final class Reflect {

    /**
     * Helpful class for doing reflections with some pre-made features.
     *
     * @author hapyl
     * @version 1.42
     */

    /**
     * @return Current version of CraftBukkit.
     */
    public static String version() {
        final String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    /**
     * @param name - Name of the target class.
     * @return 'net.minecraft.server' class if found, else null.
     */
    public static Class<?> getNetClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + version() + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param name - Name of the target class.
     * @return 'org.bukkit.craftbukkit class if found, else null.
     */
    public static Class<?> getCraftClass(String name) {
        try {
            return Class.forName("org.bukkit.craftbukkit." + version() + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param block - CraftBlock.
     * @return 'BlockPosition' class that requires for NMS iterations.
     */
    public static Object getBlockPosition(Block block) {
        Location loc = block.getLocation();
        try {
            return getNetClass("BlockPosition").getDeclaredConstructor(double.class, double.class, double.class).newInstance(loc.getX(), loc.getY(), loc.getZ());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Hides an entity for certain player.
     *
     * @param entity - Entity that will be hidden.
     * @param player - Who entity will be hidden for.
     * @return Entity that was hidden.
     */
    public static Entity hideEntityFor(Entity entity, Player player) {
        try {
            sendPacket(player, getNetClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{entity.getEntityId()}));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return entity;
    }

    /**
     * Hides an entity for certain player.
     *
     * @param entity - Entity that will be hidden.
     * @param player - Who entity will be hidden for.
     * @return Entity that was hidden.
     */
    public static Entity hideEntityFor(Entity entity, Player... player) {
        Entity e = null;
        for (Player t : player) {
            if (e == null) e = hideEntityFor(entity, t);
            else hideEntityFor(entity, t);
        }
        return e;
    }

    /**
     * @param craftEntity - CraftEntity.
     * @return NMS class of the CraftEntity.
     */
    public static Object getNetEntity(Entity craftEntity) {
        try {
            return craftEntity.getClass().getMethod("getHandle").invoke(craftEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param craftWorld - CraftWorld.
     * @return NMS class of the CraftWorld.
     */
    public static Object getNetWorld(World craftWorld) {
        try {
            return craftWorld.getClass().getMethod("getHandle").invoke(craftWorld);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param craftServer - CraftServer.
     * @return NMS class of the CraftWorld.
     */
    public static Object getNetServer(Server craftServer) {
        try {
            return craftServer.getClass().getMethod("getServer").invoke(craftServer);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    /**
     * This class allows to apply glowing for entities for certain players.
     * Glowing colors not yet implemented!
     */
    public static class Glowing implements Runnable {

        public static final Map<Glowing, Integer> glowingFor = new HashMap<>();

        private Entity entity;
        private final Set<Player> viewers = new HashSet<>();

        private Glowing() {
        }

        /**
         * This method is required for glowing to work.
         * Just invoke this method in you onEnable()
         *
         * @param mainPlugin - You main plugin.
         */
        public static void schedule(Plugin mainPlugin) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(mainPlugin, new Glowing(), 0, 1);
        }

        /**
         * Creates a glowing object.
         *
         * @param entity          - Entity that will glow.
         * @param glowingDuration - Glowing duration in ticks.
         * @param players         - Players who will see the glowing.
         */
        public Glowing(Entity entity, int glowingDuration, Player... players) {
            this.entity = entity;
            this.viewers.addAll(Arrays.asList(players));
            glowingFor.put(this, glowingDuration);
        }

        @Override
        public void run() {
            try {
                final Map<Glowing, Integer> hash = new HashMap<>(glowingFor);
                if (hash.isEmpty()) return;
                hash.forEach((e, i) -> {
                    if (i > 0) {
                        e.glow(true);
                        glowingFor.put(e, i - 1);
                    } else {
                        glowingFor.remove(e);
                    }
                });
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private void glow(boolean glow) {
            try {

                final Object netEntity = getNetEntity(this.entity);
                final Object dataWatcher = netEntity.getClass().getMethod("getDataWatcher").invoke(netEntity);
                final Object newDataWatcher = getNetClass("DataWatcher").getConstructor(getNetClass("Entity")).newInstance(netEntity);

                final Int2ObjectMap<Object> entries = (Int2ObjectMap<Object>) FieldUtils.readDeclaredField(dataWatcher, "entries", true);
                final Int2ObjectMap<Object> copyEntries = new Int2ObjectOpenHashMap<>();

                entries.forEach((integer, o) -> {
                    try {
                        copyEntries.put((int) integer, o.getClass().getMethod("d").invoke(o));
                    } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                });

                final Object item = entries.get(0);
                byte initBitMask = (byte) item.getClass().getMethod("b").invoke(item);
                byte bitMask = (byte) 6;

                item.getClass().getMethod("a", Object.class)
                        .invoke(item, (byte) (glow ? initBitMask | 1 << bitMask : initBitMask & ~(1 << bitMask)));

                FieldUtils.writeDeclaredField(newDataWatcher, "entries", copyEntries, true);

                for (Player p : this.viewers) {
                    sendPacket(p, getNetClass("PacketPlayOutEntityMetadata").getConstructor(int.class, getNetClass("DataWatcher"), boolean.class)
                            .newInstance((int) this.entity.getEntityId(), dataWatcher, true));
                }

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // TODO: 001. 09/01/2020 Implement colors
        private static void repatchTeam(Entity entity, GlowingColor color, boolean createOrAdd) {

            if (true) throw new NotImplementedException();

            try {

                Object packetScoreTeam = getNetClass("PacketPlayOutScoreboardTeam").newInstance();

                FieldUtils.writeDeclaredField(packetScoreTeam, "i", 0, true);
                FieldUtils.writeDeclaredField(packetScoreTeam, "i", 3, true);
                FieldUtils.writeDeclaredField(packetScoreTeam, "a", color.getNSMColor(), true);
                FieldUtils.writeDeclaredField(packetScoreTeam, "e", false, true);
                FieldUtils.writeDeclaredField(packetScoreTeam, "f", false, true);

                if (createOrAdd) {
                    FieldUtils.writeDeclaredField(packetScoreTeam, "g", color.getNSMColor());
                }

            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }

        public enum GlowingColor {

            BLACK('0'),
            DARK_BLUE('1'),
            DARK_GREEN('2'),
            DARK_AQUA('3'),
            DARK_RED('4'),
            DARK_PURPLE('5'),
            GOLD('6'),
            GRAY('7'),
            DARK_GRAY('8'),
            BLUE('9'),
            GREEN('a'),
            AQUA('b'),
            RED('c'),
            LIGHT_PURPLE('d'),
            YELLOW('e'),
            WHITE('f'),
            NULL('_');

            private char c;

            GlowingColor(char c) {
                this.c = c;

            }

            public Object getNSMColor() {
                try {
                    final Class<?> clazz = getNetClass("EnumChatFormat");
                    return FieldUtils.getDeclaredField(clazz, this.name()).get(clazz);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            }

        }

    } // end of glowing class

    /**
     * This class allows to create simple NMS NPCs.
     */
    public static class NPC {

        private Object npc, packetInfo, packetSpawn, packetRotation;
        private Location location;
        private GameProfile profile;
        private String skinCache;
        private String npcName;

        private boolean exist = false;

        public NPC(Location loc, String npcName) {
            try {

                this.location = loc;
                this.npcName = npcName;

                final UUID uuid = UUID.randomUUID();
                final Object netWorld = getNetWorld(loc.getWorld());
                this.profile = new GameProfile(uuid, npcName);

                this.npc = getNetClass("EntityPlayer").getConstructor(getNetClass("MinecraftServer"),
                        getNetClass("WorldServer"), GameProfile.class,
                        getNetClass("PlayerInteractManager"))
                        .newInstance(getNetServer(Bukkit.getServer()), netWorld, this.profile, getNetClass("PlayerInteractManager").getConstructor(netWorld.getClass()).newInstance(netWorld));

                final Class<?> enumPlayerInfo = getNetClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

                this.packetInfo = getNetClass("PacketPlayOutPlayerInfo").getConstructors()[0]
                        .newInstance(enumPlayerInfo.getField("ADD_PLAYER").get(this.npc), Collections.singletonList(this.npc));
                this.packetSpawn = getNetClass("PacketPlayOutNamedEntitySpawn").getConstructor(getNetClass("EntityHuman")).newInstance(npc);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void setHeadRotation(float yaw, Player... viewers) {
            try {
                this.packetRotation = getNetClass("PacketPlayOutEntityHeadRotation")
                        .getConstructor(getNetClass("Entity"), byte.class).newInstance(npc, (byte) (yaw * 256 / 360));
                sendPacket(this.packetRotation, viewers);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
            }
        }

        public void setLocation(Location loc, Player... viewers) {
            try {
                this.npc.getClass().getMethod("setLocation", double.class, double.class, double.class, float.class, float.class)
                        .invoke(this.npc, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
                sendPacket(getNetClass("PacketPlayOutEntityTeleport").getConstructor(getNetClass("Entity")).newInstance(this.npc), viewers);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void spawn(Player... players) {
            sendPacket(this.packetInfo, players);
            sendPacket(this.packetSpawn, players);
            setLocation(this.location, players);
            setHeadRotation(this.location.getYaw(), players);
            this.exist = true;
        }

        /**
         * @return health if alive ; -2 if not alive ; -1 if error
         */
        public float getHealth() {
            if (!this.exist) {
                return -2;
            }
            try {
                return (float) getNetClass("EntityPlayer").getMethod("getHealth").invoke(this.npc);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                return -1;
            }
        }

        public void remove(Player... players) {
            try {
                this.exist = false;
                sendPacket(getNetClass("PacketPlayOutPlayerInfo").getConstructors()[0]
                        .newInstance(getNetClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction").getField("REMOVE_PLAYER").get(this.npc),
                                Collections.singletonList(this.npc)), players);
                sendPacket(getNetClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{getID()}), players);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public int getID() {
            try {
                return (int) this.npc.getClass().getMethod("getId").invoke(this.npc);
            } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                e.printStackTrace();
                return -1;
            }
        }

        public void setSkin(String skinOwner, Player... viweres) {
            try {

                // added caching to reduce api calls
                if (this.skinCache == null) {
                    HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format("https://api.ashcon.app/mojang/v2/user/%s", skinOwner)).openConnection();
                    if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                        final List<String> lines = new ArrayList<>();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                        reader.lines().forEach(lines::add);
                        this.skinCache = Arrays.toString(lines.toArray());
                    }


                    int indexOfValue = this.skinCache.indexOf("\"value\": \"");
                    int indexOfSignature = this.skinCache.indexOf("\"signature\": \"");
                    String skin = this.skinCache.substring(indexOfValue + 10, this.skinCache.indexOf("\"", indexOfValue + 10));
                    String signature = this.skinCache.substring(indexOfSignature + 14, this.skinCache.indexOf("\"", indexOfSignature + 14));

                    this.profile.getProperties().put("textures", new Property("textures", skin, signature));

                    // client (2nd layer skin support)
                    final Object dataWatcher = this.npc.getClass().getMethod("getDataWatcher").invoke(this.npc);
                    final Class<?> dataWatcherRegistry = getNetClass("DataWatcherRegistry");

                    dataWatcher.getClass().getMethod("set", getNetClass("DataWatcherObject"), Object.class)
                            .invoke(dataWatcher, getNetClass("DataWatcherObject").getConstructor(int.class, getNetClass("DataWatcherSerializer"))
                                    .newInstance(15, dataWatcherRegistry.getField("a").get(dataWatcherRegistry)), 127);

                    sendPacket(getNetClass("PacketPlayOutEntityMetadata").getConstructor(int.class, getNetClass("DataWatcher"), boolean.class)
                            .newInstance(this.getID(), dataWatcher, true), viweres);

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void sendPacket(Object packet, Player... players) {
            for (Player player : players) {
                Reflect.sendPacket(player, packet);
            }
        }


    } // end of npc class

    /**
     * This class allows to create per player border,
     * useful for 'warning red screen' effect.
     */
    public static class Border {

        private Object border;
        private World world;
        private int size, warning;

        /**
         * Creates a border object.
         *
         * @param world - CraftWorld.
         *              Usage:
         *              <p>
         *              Reflect.Border border = new Reflect.Border(WORLD);
         *              border.setSize(1000).setWarningDistance(1000);
         *              border.applyChanges(PlAYER...)
         *              <p>
         *              /OR/
         *              new Reflect.Border(WORLD).setSize(1000).setWarningDistance(1000).applyChanges(PLAYER...);
         */
        public Border(World world) {

            this.world = world;
            this.size = 1000;
            this.warning = 0;
            this.init();
        }

        /**
         * Sets the size of the border.
         *
         * @param size - Size.
         */
        public Border setSize(int size) {
            this.size = size;
            return this;
        }

        /**
         * Sets the distance for the warning.
         *
         * @param distance - Distance.
         */
        public Border setWarningDistance(int distance) {
            this.warning = distance;
            return this;
        }

        /**
         * Applies all the changes via sending packets the viewers.
         *
         * @param viewers - Players that will see the effect.
         */
        public void applyChanges(Player... viewers) {

            if (viewers.length == 0) {
                throw new IllegalArgumentException("Viewers must have at least 1 player!");
            }

            try {
                for (Player player : viewers) {
                    // change size
                    MethodUtils.invokeMethod(this.border, "setSize", this.size);
                    throwPacket(BorderInfo.SET_SIZE, player, this.border);
                    // change warning
                    MethodUtils.invokeMethod(this.border, "setWarningDistance", this.warning);
                    throwPacket(BorderInfo.SET_WARNING_BLOCKS, player, this.border);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        @NotImplemented(comment = "Do not use this yet, doesn't work at the time.")
        public void transitionSizeBetween(double start, double finish, long time, Player... viewers) {

            if (true) {
                throw new NotImplementedException();
            }

            if (viewers.length == 0) {
                throw new IllegalArgumentException("Viewers must have at least 1 player!");
            }

            try {
                this.border.getClass().getMethod("transitionSizeBetween", double.class, double.class, long.class).invoke(this.border, start, finish, time);
                for (Player player : viewers) {
                    throwPacket(BorderInfo.SET_SIZE, player, border);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        private void init() {
            try {
                this.border = getNetClass("WorldBorder").getConstructor().newInstance();
                FieldUtils.writeDeclaredField(this.border, "world", getNetWorld(this.world));
                MethodUtils.invokeMethod(this.border, "setDamageAmount", 0);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        private static void throwPacket(BorderInfo info, Player player, Object worldBorder) {
            try {
                final Class<?> enumWorld = getNetClass("PacketPlayOutWorldBorder$EnumWorldBorderAction");
                final Object packet = getNetClass("PacketPlayOutWorldBorder")
                        .getConstructor(getNetClass("WorldBorder"), enumWorld)
                        .newInstance(worldBorder, enumWorld.getField(info.getStringName()).get(enumWorld));
                Reflect.sendPacket(player, packet);
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
                e.printStackTrace();
            }

        }

        private enum BorderInfo {

            SET_SIZE,
            SET_WARNING_BLOCKS;

            BorderInfo() {
            }

            private String getStringName() {
                return this.name();
            }

        }

    } // end of border class

    /**
     * Sends the packet to the player.
     *
     * @param player - Receiver of the packet.
     * @param packet - Object in a Packet form, must be instance of NMS 'Packet' class.
     */
    public static void sendPacket(Player player, Object packet) {
        final Object craftPlayer = getCraftPlayer(player);
        final Object playerConnection;
        try {
            playerConnection = craftPlayer.getClass().getField("playerConnection").get(craftPlayer);
            playerConnection.getClass().getMethod("sendPacket", getNetClass("Packet")).invoke(playerConnection, packet);
        } catch (IllegalAccessException | NoSuchFieldException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param player - CraftPlayer.
     * @return NSM class of the CraftPlayer.
     */
    public static Object getCraftPlayer(Player player) {
        try {
            return player.getClass().getMethod("getHandle").invoke(player);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return player;
        }
    }

    /**
     * @param player - CraftPlayer.
     * @return Player's current ping. Updates one per minute.
     */
    public static int getPing(Player player) {
        try {
            return getCraftPlayer(player).getClass().getField("ping").getInt(getCraftPlayer(player));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /*ignore this*/
    @Target({ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    private @interface NotImplemented {
        String comment();
    }

}
