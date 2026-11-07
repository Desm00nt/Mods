package org.sprautestudio.spraute_engine.build;

public final class BlockQuake {
    public static final double DEFAULT_RISE = 0.3;
    private static final double LEAST_RISE = 0.05;
    private static final double MOST_OF_TIME = 0.6;
    private static final double LATE_STEPS = 1.5;
    public static final double DEFAULT_TILT = 10.0;
    private static final double SHAKES_PER_SECOND = 14.0;

    private BlockQuake() {
    }

    private static double unit(long seed, int salt) {
        long h = (seed + salt) * -7046029254386353131L;
        h ^= h >>> 29;
        h *= -4658895280553007687L;
        return (h >>> 40) / 1.6777216E7;
    }

    public static double[] shake(long seed, double power, double seconds, double age) {
        if (!(seconds <= 0.0) && !(age >= seconds)) {
            double strength = power * (1.0 - Math.max(0.0, age) / seconds);
            double a = age * 14.0 * 2.0 * Math.PI;
            double[] out = new double[3];

            for (int i = 0; i < 3; i++) {
                long h = (seed + i + 1L) * -7046029254386353131L;
                double phase = (h >>> 40) / 1.6777216E7 * 2.0 * Math.PI;
                double speed = 0.8 + (h >>> 20 & 255L) / 255.0 * 0.4;
                out[i] = strength * Math.sin(a * speed + phase);
            }

            return out;
        } else {
            return new double[3];
        }
    }

    public record Wave(double radius, double height, double step, double rise, double tilt) {
        public static BlockQuake.Wave of(double radius, double height, double seconds, double step, double rise, double tilt) {
            double r = Math.max(0.0, radius);
            double air = Math.max(0.05, rise > 0.0 ? rise : 0.3);
            if (step > 0.0) {
                return new BlockQuake.Wave(r, Math.max(0.0, height), step, air, tilt);
            } else {
                double time = Math.max(0.05, seconds);
                if (rise <= 0.0) {
                    air = Math.min(air, time * 0.6);
                }

                air = Math.min(air, time);
                return new BlockQuake.Wave(r, Math.max(0.0, height), r > 0.0 ? (time - air) / r : 0.0, air, tilt);
            }
        }

        public double seconds() {
            return this.radius * this.step + 1.5 * this.step + this.rise;
        }

        private double through(double distance, long seed, double age) {
            if (!(this.radius <= 0.0) && !(distance > this.radius)) {
                double start = distance * this.step + BlockQuake.unit(seed, 3) * 1.5 * this.step;
                double t = (age - start) / this.rise;
                return !(t <= 0.0) && !(t >= 1.0) ? t : 0.0;
            } else {
                return 0.0;
            }
        }

        public double lift(double distance, long seed, double age) {
            double t = this.through(distance, seed, age);
            return t == 0.0 ? 0.0 : this.height * Math.sqrt(1.0 - distance / this.radius) * Math.sin(Math.PI * t);
        }

        public double lean(double distance, long seed, double age) {
            double t = this.through(distance, seed, age);
            return t == 0.0 ? 0.0 : this.tilt * (0.7 + 0.6 * BlockQuake.unit(seed, 5)) * Math.sin(Math.PI * t);
        }
    }
}
