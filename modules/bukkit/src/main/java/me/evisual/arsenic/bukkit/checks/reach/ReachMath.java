package me.evisual.arsenic.bukkit.checks.reach;

public final class ReachMath {
    private ReachMath() {
    }

    public static double rayIntersectAabb(Vec3 origin, Vec3 direction, Aabb box) {
        double tMin = 0.0;
        double tMax = Double.MAX_VALUE;

        double[] o = {origin.x, origin.y, origin.z};
        double[] d = {direction.x, direction.y, direction.z};
        double[] min = {box.minX, box.minY, box.minZ};
        double[] max = {box.maxX, box.maxY, box.maxZ};

        for (int i = 0; i < 3; i++) {
            if (Math.abs(d[i]) < 1.0e-9) {
                if (o[i] < min[i] || o[i] > max[i]) {
                    return -1.0;
                }
            } else {
                double inv = 1.0 / d[i];
                double t1 = (min[i] - o[i]) * inv;
                double t2 = (max[i] - o[i]) * inv;
                if (t1 > t2) {
                    double tmp = t1;
                    t1 = t2;
                    t2 = tmp;
                }
                tMin = Math.max(tMin, t1);
                tMax = Math.min(tMax, t2);
                if (tMin > tMax) {
                    return -1.0;
                }
            }
        }

        return tMin;
    }

    public static final class Vec3 {
        public final double x;
        public final double y;
        public final double z;

        public Vec3(double x, double y, double z) {
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3 normalize() {
            double len = Math.sqrt(x * x + y * y + z * z);
            if (len <= 0.0) {
                return new Vec3(0, 0, 0);
            }
            return new Vec3(x / len, y / len, z / len);
        }
    }

    public static final class Aabb {
        public final double minX;
        public final double minY;
        public final double minZ;
        public final double maxX;
        public final double maxY;
        public final double maxZ;

        public Aabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
            this.minX = minX;
            this.minY = minY;
            this.minZ = minZ;
            this.maxX = maxX;
            this.maxY = maxY;
            this.maxZ = maxZ;
        }
    }
}
