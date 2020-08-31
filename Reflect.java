package ru.hapyl.classesfight.utils;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import net.minecraft.server.v1_16_R1.EnumChatFormat;
import net.minecraft.server.v1_16_R1.PacketPlayOutScoreboardTeam;
import net.minecraft.server.v1_16_R1.ScoreboardTeam;
import org.apache.commons.lang.reflect.FieldUtils;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.bukkit.craftbukkit.libs.it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Skeleton;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import ru.hapyl.classesfight.ClassesFight;
import ru.hapyl.classesfight.GarbageCollector;
import ru.hapyl.classesfight.ScoreboardManager;

import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.*;

public final class Reflect {

    public static String version() {
        final String name = Bukkit.getServer().getClass().getPackage().getName();
        return name.substring(name.lastIndexOf(".") + 1);
    }

    public static Class<?> getNetClass(String name) {
        try {
            return Class.forName("net.minecraft.server." + version() + "." + name);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getBlockPos(Block block) {
        Location loc = block.getLocation();
        try {
            return getNetClass("BlockPosition").getDeclaredConstructor(double.class, double.class, double.class).newInstance(loc.getX(), loc.getY(), loc.getZ());
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static Entity hideEntityFor(Entity entity, Player player) {
        try {
            sendPacket(player, getNetClass("PacketPlayOutEntityDestroy").getConstructor(int[].class).newInstance(new int[]{entity.getEntityId()}));
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
        }
        return entity;
    }

    public static Object getNetEntity(Entity bukkitEntity) {
        try {
            return bukkitEntity.getClass().getMethod("getHandle").invoke(bukkitEntity);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getNetWorld(World bukkitWorld) {
        try {
            return bukkitWorld.getClass().getMethod("getHandle").invoke(bukkitWorld);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Object getNetServer(Server bukkitServer) {
        try {
            return bukkitServer.getClass().getMethod("getServer").invoke(bukkitServer);
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static void applyGlowingFor(Location location, int time, Player... viewers) {

        Skeleton entity = location.getWorld().spawn(location, Skeleton.class, me -> {
            me.setAI(false);
            me.setInvulnerable(true);
            me.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 99999, 99));
            me.setGravity(false);
            me.setSilent(true);
            me.getEquipment().clear();
            me.getLocation().setYaw(location.getYaw());
            me.getLocation().setPitch(location.getPitch());
        });

        Set<Player> hash = new HashSet<>(Arrays.asList(viewers));

        Bukkit.getOnlinePlayers().iterator().forEachRemaining(player -> {
            if (!hash.contains(player)) hideEntityFor(entity, player);
        });

        entity.setGlowing(true);
        GarbageCollector.add(entity);
        GarbageCollector.add(Bukkit.getScheduler().runTaskLater(ClassesFight.getInstance(), entity::remove, time));

    }

    public static class Glowing implements Runnable {

        public static final Map<Glowing, Integer> glowingFor = new HashMap<>();

        private Entity entity;
        private final Set<Player> viewers = new HashSet<>();

        private Glowing() {

        }


        public static void schedule(Plugin mainPlugin) {
            Bukkit.getScheduler().scheduleSyncRepeatingTask(mainPlugin, new Glowing(), 0, 1);
        }

        public Glowing(Entity entity, int glowingDuration, ChatColor glowingColor, Player... players) {
            this.entity = entity;
            this.viewers.addAll(Arrays.asList(players));

            if (entity instanceof Player) {
                final Player player = (Player) entity;
                ScoreboardManager.getCurrentScore(player).joinTeam("team_" + glowingColor.getChar(), player);
            }

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
                        e.glow(false);
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
                final Int2ObjectMap<Object> copyEntries = new Int2ObjectOpenHashMap();

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

    }

    public static class NPC {

        private Object npc, packetInfo, packetSpawn, packetRotation;
        private Location location;
        private GameProfile profile;
        private String npcName;

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
                        .newInstance(getNetServer(Bukkit.getServer()),
                                netWorld,
                                profile,
                                getNetClass("PlayerInteractManager").getConstructor(netWorld.getClass()).newInstance(netWorld));

                final Class<?> enumPlayerInfo = getNetClass("PacketPlayOutPlayerInfo$EnumPlayerInfoAction");

                this.packetInfo = getNetClass("PacketPlayOutPlayerInfo").getConstructors()[0].newInstance(enumPlayerInfo.getField("ADD_PLAYER").get(npc), Collections.singletonList(npc));
                this.packetSpawn = getNetClass("PacketPlayOutNamedEntitySpawn").getConstructor(getNetClass("EntityHuman")).newInstance(npc);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        public void setHeadRotation(float yaw, Player... viewers) {
            try {
                this.packetRotation = getNetClass("PacketPlayOutEntityHeadRotation").getConstructor(getNetClass("Entity"), byte.class).newInstance(npc, (byte) (yaw * 256 / 360));
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
        }

        public void remove(Player... players) {
            try {
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
                HttpsURLConnection connection = (HttpsURLConnection) new URL(String.format("https://api.ashcon.app/mojang/v2/user/%s", skinOwner)).openConnection();
                if (connection.getResponseCode() == HttpsURLConnection.HTTP_OK) {
                    final List<String> lines = new ArrayList<>();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                    reader.lines().forEach(lines::add);

                    String reply = Arrays.toString(lines.toArray());

                    int indexOfValue = reply.indexOf("\"value\": \"");
                    int indexOfSignature = reply.indexOf("\"signature\": \"");
                    String skin = reply.substring(indexOfValue + 10, reply.indexOf("\"", indexOfValue + 10));
                    String signature = reply.substring(indexOfSignature + 14, reply.indexOf("\"", indexOfSignature + 14));

                    this.profile.getProperties().put("textures", new Property("textures", skin, signature));

                    // client (2nd layer skin support)
                    final Object dataWatcher = this.npc.getClass().getMethod("getDataWatcher").invoke(this.npc);
                    final Class<?> dataWatcherRegistry = getNetClass("DataWatcherRegistry");

                    dataWatcher.getClass().getMethod("set", getNetClass("DataWatcherObject"), Object.class)
                            .invoke(dataWatcher, getNetClass("DataWatcherObject").getConstructor(int.class, getNetClass("DataWatcherSerializer"))
                                    .newInstance(15, dataWatcherRegistry.getField("a").get(dataWatcherRegistry)), (byte) 127);

                    sendPacket(getNetClass("PacketPlayOutEntityMetadata").getConstructor(int.class, getNetClass("DataWatcher"), boolean.class)
                            .newInstance(this.getID(), dataWatcher, true));

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

    public static Object getCraftPlayer(Player player) {
        try {
            return player.getClass().getMethod("getHandle").invoke(player);
        } catch (IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
            e.printStackTrace();
            return player;
        }
    }

    public static int getPing(Player player) {

        try {
            return getCraftPlayer(player).getClass().getField("ping").getInt(getCraftPlayer(player));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
            return -1;
        }

    }
}
