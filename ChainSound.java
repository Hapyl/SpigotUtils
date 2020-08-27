@@ Your package goes here!

import com.sun.scenario.effect.light.SpotLight;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public final class ChainSound {

    private Plugin main;
    private double xShift, yShift, zShift;
    private List<BukkitSound> sounds = new ArrayList<>();
    private SoundCategory category = SoundCategory.MASTER;
    private Set<Player> listeners = new HashSet<>();
    private boolean playing = false, global = false;

    /**
     * The one and only constructor of the class.
     * <p>
     * ChainSound is used to play sounds one after the other!
     * To do so, follow these steps:
     * <p>
     * **** Create a ChainSound
     * ChainSound chain = new ChainSound(@MainClass);
     * <p>
     * **** Add sounds to queue
     * chain
     * .append(Sound.BLOCK_NOTE_BLOCK_PLING, 1f, 2)
     * .append(Sound.BLOCK_NOTE_BLOCK_PLING, 1.5f, 2)
     * .append(Sound.BLOCK_NOTE_BLOCK_PLING, 2f, 5);
     * <p>
     * **** Add listeners of the sound
     * chain.addListener(@Player)
     * <p>
     * **** And finally play the sound!
     * chain.play()
     *
     * @param mainClass your JavaPlugin class.
     */
    public ChainSound(Plugin mainClass) {
        this.main = mainClass;
    }

    /**
     * Adds a sound to the chain.
     *
     * @param sound BukkitSound to add
     * @return self
     */
    public ChainSound append(BukkitSound sound) {
        sounds.add(sound);
        return this;
    }

    /**
     * Alternative methods to add a sound to the chain.
     *
     * @param sound sound to add
     * @param pitch pitch of the sound
     * @param delay delay after previous sound
     * @return self
     */
    public ChainSound append(Sound sound, float pitch, int delay) {
        return this.append(new BukkitSound(sound, pitch, delay));
    }

    /**
     * Shifts the location of the sound relative to the listener (player)
     * By default shift is 0
     *
     * @param x shift by that amount in x
     * @param y shift by that amount in y
     * @param z shift by that amount in z
     * @return self
     */
    public ChainSound shiftLocation(double x, double y, double z) {
        this.xShift = x;
        this.yShift = y;
        this.zShift = z;
        return this;
    }

    /**
     * Sets the playing category.
     * Default category is MASTER
     *
     * @param category new category
     * @return self
     */
    public ChainSound setCategory(SoundCategory category) {
        this.category = category;
        return this;
    }

    /**
     * Adds a listener (player)
     *
     * @param player the listener
     */
    public void addListener(final Player player) {
        this.listeners.add(player);
    }

    /**
     * Adds a listener (player)
     *
     * @param players collection of player
     */
    public void addListener(final Collection<Player> players) {
        this.listeners.addAll(players);
    }

    /**
     * Adds a listener (player)
     *
     * @param players array of player
     */
    public void addListener(final Player... players) {
        this.listeners.addAll(Arrays.asList(players));
    }

    /**
     * This removes all listeners and instead playing sound
     * for every online player.
     */
    public ChainSound everyoneIsListener() {
        this.listeners.clear();
        this.global = true;
        return this;
    }


    /**
     * @return true if sounds is playing right now, false if not.
     */
    public boolean getStatus() {
        return this.playing;
    }

    /**
     * Builds and plays the sound, must be used last.
     */
    public void play() {
        this.build();
    }

    // This is the main functions, it calculates the time and plays the sound.
    private void build() {

        this.playing = true;
        final Map<Integer, BukkitSound> soundQueue = new HashMap<>();
        int totalTicks = 0;

        for (BukkitSound sound : this.sounds) {
            totalTicks += sound.getDelay();
            soundQueue.put(totalTicks, sound);
        }

        final int total = totalTicks;
        final ChainSound reference = this;

        new BukkitRunnable() {

            int passed = 0;

            @Override
            public void run() {
                if (passed <= total) {
                    final BukkitSound currentSound = soundQueue.getOrDefault(passed, null);

                    // if global enabled, reiterate players.
                    if (reference.global) {
                        reference.listeners.clear();
                        listeners.addAll(Bukkit.getOnlinePlayers());
                    }

                    if (currentSound != null) {
                        reference.listeners.iterator().forEachRemaining(player -> {
                            // this checks is player left during playing.
                            if (player != null) {
                                final Location location = getShiftedLocation(player);
                                player.playSound(location, currentSound.getSound(), reference.category, 10f, currentSound.pitch);
                            }
                        });
                    }
                    passed++;
                } else {
                    reference.playing = false;
                    this.cancel();
                }
            }

        }.runTaskTimer(this.main, 0, 0);
    }

    private Location getShiftedLocation(final Player player) {
        return player.getLocation().clone().add(this.xShift, this.yShift, this.zShift);
    }

    /**
     * This class is used to store sounds.
     * You can either use it, or another .append method.
     */
    public static class BukkitSound {

        private Sound sound;
        private int delay;
        private float pitch;

        public BukkitSound(Sound sound, float pitch, int delay) {

            Validate("Delay must be greater than 0 and cannot be higher than 200.", delay < 0, delay > 200);
            Validate("Pitch cannot be more than 2 and less than 0.", pitch < 0.0d, pitch > 2.0d);

            this.sound = sound;
            this.pitch = pitch;
            this.delay = delay;

        }

        // Throws an exception if one or more booleans true
        private void Validate(String msg, boolean... b) {
            boolean me = false;
            for (boolean bool : b) {
                me = bool;
            }
            if (me) throw new ChainSoundException(msg);
        }

        public Sound getSound() {
            return sound;
        }

        public int getDelay() {
            return delay;
        }
    }

    private static class ChainSoundException extends RuntimeException {

        ChainSoundException(String a) {
            super(a);
        }
    }

}
