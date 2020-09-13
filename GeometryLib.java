
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public final class GeometryLib {

    /**
     * This class allows to draw different geometry objects using particles.
     *
     * @author hapyl
     * @version 1.0
     */

    /**
     * Draws a sphere using certain particles for certain player.
     *
     * @param center          - Init center location of the sphere.
     * @param rings           - Amount of vertical 'rings' that sphere will have. Move rings - better quality but less performance.
     * @param radius          - Radius of the sphere.
     * @param forPlayer       - Particle that will be shown to the player.
     * @param forEveryoneElse - Particle that will be shown to everyone but player.
     * @param player          - The player.
     */

    public static void drawSphere(Location center, double rings, double radius, ITinyParticle forPlayer, ITinyParticle forEveryoneElse, Player player) {
        for (double i = 0; i < Math.PI; i += Math.PI / rings) {
            double r = Math.sin(i) * radius;
            double y = Math.cos(i) * radius;
            for (double j = 0; j < Math.PI * 2; j += Math.PI / (rings / 2)) {
                double x = r * Math.sin(j);
                double z = r * Math.cos(j);
                center.add(x, y, z);

                // Draw Particles
                if (forEveryoneElse == null || player == null) {
                    forPlayer.draw(center);
                } else {
                    Bukkit.getOnlinePlayers().iterator().forEachRemaining(e -> {
                        if (e == player) forPlayer.draw(center, player);
                        else forEveryoneElse.draw(center, e);
                    });
                }
                center.subtract(x, y, z);
            }
        }
    }

    public static void drawSphere(Location center, double rings, double radius, ITinyParticle particle) {
        drawSphere(center, rings, radius, particle, null, null);
    }

    public static void drawCircle(Location center, double quality, double radius, ITinyParticle particle, Player player) {
        for (double i = 0; i < quality; i += 1) {
            double x = (radius * Math.sin(i));
            double z = (radius * Math.cos(i));
            center.add(x, 0, z);

            if (player == null) particle.draw(center);
            else particle.draw(center, player);

            center.subtract(x, 0, z);
        }
    }

    public static void drawCircle(Location center, double quality, double radius, ITinyParticle particle) {
        drawCircle(center, quality, radius, particle, null);
    }

    public static void drawLine(Location from, Location to, double step, ITinyParticle particle, Player player) {

        if (!from.getWorld().equals(to.getWorld()))
            throw new IllegalArgumentException("Unable to draw particles in different worlds.");

        final double distance = from.distance(to);
        final Vector vector = to.toVector().subtract(from.toVector()).normalize().multiply(step);

        for (double i = 0; i < distance; i += step) {
            from.add(vector);
            if (player == null) particle.draw(from);
            else particle.draw(from, player);
        }

    }

    public static void drawLine(Location from, Location to, double step, ITinyParticle particle) {
        drawLine(from, to, step, particle, null);
    }

    /* ********************************************************** */

    private interface ITinyParticle {
        void draw(Location location);

        void draw(Location loc, Player player);
    }

    public static class TinyParticle implements ITinyParticle {

        private Particle particle;
        private double offX, offY, offZ, speed;
        private int amount;

        public TinyParticle(Particle particle, int amount, double offX, double offY, double offZ, double speed) {
            this.particle = particle;
            this.amount = amount;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
            this.speed = speed;
        }

        public TinyParticle(Particle particle) {
            this(particle, 1, 0, 0, 0, 0);
        }

        @Override
        public void draw(Location at) {
            at.getWorld().spawnParticle(this.particle, at, this.amount, this.offX, this.offY, this.offZ, this.speed);
        }

        @Override
        public void draw(Location at, Player player) {
            player.spawnParticle(this.particle, at, this.amount, this.offX, this.offY, this.offZ, this.speed);
        }

    }

    public static class TinyReddust implements ITinyParticle {

        private double offX, offY, offZ, speed;
        private int amount;
        private Color color;

        public TinyReddust(Color color, int amount, double offX, double offY, double offZ, double speed) {
            this.color = color;
            this.amount = amount;
            this.offX = offX;
            this.offY = offY;
            this.offZ = offZ;
            this.speed = speed;
        }

        @Override
        public void draw(Location at) {
            at.getWorld().spawnParticle(Particle.REDSTONE, at, this.amount, this.offX, this.offY, this.offZ, this.speed, new Particle.DustOptions(this.color, this.amount));
        }

        @Override
        public void draw(Location at, Player player) {
            player.spawnParticle(Particle.REDSTONE, at, this.amount, this.offX, this.offY, this.offZ, this.speed, new Particle.DustOptions(this.color, this.amount));
        }

    }


}
