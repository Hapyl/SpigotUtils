// Your package goes here. //

import java.util.*;

public final class Cooldown {

    /**
     * Simple class for creating cooldowns.
     */

    public static final Map<String, Cooldown> storage = new HashMap<>();
    private static final String PATH = "%s:%s";

    private UUID uuid;
    private String id;
    private long startedAt;
    private int timeSec;

    public Cooldown(UUID uuid, String id, int timeSec) {
        this.uuid = uuid;
        this.id = id;
        this.startedAt = System.currentTimeMillis();
        this.timeSec = timeSec;
        storage.put(String.format(PATH, this.uuid, this.id), this);
    }

    public static Cooldown getCD(UUID uuid, String id) {
        return storage.getOrDefault(String.format(PATH, uuid, id), null);
    }

    public static boolean inCooldown(UUID uuid, String id) {
        final Cooldown cd = getCD(uuid, id);
        if (cd == null) return false;
        if (cd.getTimeLeft() >= 0) return true;
        return cd.stop();
    }

    public boolean stop() {
        storage.remove(String.format(PATH, this.uuid, this.id));
        return false;
    }

    public int getTimeLeft() {
        return (int) (this.timeSec - ((System.currentTimeMillis() - this.startedAt) / 1000));
    }

    public static int getTimeLeft(UUID uuid, String id) {
        return getCD(uuid, id) == null ? -1 : getCD(uuid, id).getTimeLeft();
    }

}
