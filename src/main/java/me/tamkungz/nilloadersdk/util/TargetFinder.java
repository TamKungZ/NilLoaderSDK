package me.tamkungz.nilloadersdk.util;

import me.tamkungz.nilloadersdk.helper.McHelper;
import me.tamkungz.remapping.SimpleRemap;

import java.util.List;

/**
 * TargetFinder — finds the entity currently under the crosshair.
 *
 * Extracted from crosshair target detection logic used in example projects.
 */
public final class TargetFinder {

    private TargetFinder() {}

    /**
     * Finds the player entity that the feeder is currently aiming at.
     *
     * @param feeder      the source entity (viewer)
     * @param maxDistance maximum distance in blocks
     * @param minDot      minimum dot product (0.0 = hemisphere, 1.0 = directly forward)
     * @param remap       SimpleRemap instance
     * @return the targeted entity, or null if none found
     */
    public static Object findCrosshairTarget(Object feeder, double maxDistance,
                                             double minDot, SimpleRemap remap) {
        try {
            Object world = McHelper.getWorldFromEntity(feeder, remap);
            if (world == null) return null;

            List<Object> players = McHelper.collectPlayers(world, feeder, remap);
            if (players.isEmpty()) return null;

            double[] feederPos = McHelper.getEntityPos(feeder);
            double[] look      = McHelper.getLookVec(feeder, remap);

            // Without position/look data we cannot safely claim anything is under the crosshair.
            if (feederPos == null || look == null) return null;

            double fx = feederPos[0], fy = feederPos[1], fz = feederPos[2];
            double lx = look[0],     ly = look[1],       lz = look[2];

            // normalize look vector (should already be normalized, but done again for safety)
            double mag = Math.sqrt(lx*lx + ly*ly + lz*lz);
            if (mag <= 1e-6 || Double.isNaN(mag) || Double.isInfinite(mag)) return null;
            lx /= mag; ly /= mag; lz /= mag;

            if (maxDistance <= 0.0 || Double.isNaN(maxDistance)) return null;
            if (minDot < -1.0) minDot = -1.0;
            if (minDot > 1.0) minDot = 1.0;

            Object best = null;
            double bestDot = minDot;
            double bestDist = Double.POSITIVE_INFINITY;

            for (Object obj : players) {
                if (obj == null) continue;
                double[] tp = McHelper.getEntityPos(obj);
                if (tp == null) continue;

                double dx = tp[0]-fx, dy = tp[1]-fy, dz = tp[2]-fz;
                double dist = Math.sqrt(dx*dx + dy*dy + dz*dz);

                if (dist <= 1e-4 || dist > maxDistance) continue;

                double dot = (dx/dist)*lx + (dy/dist)*ly + (dz/dist)*lz;
                if (dot < minDot) continue;

                // Prefer the candidate closest to the crosshair direction.
                // Distance is only the tie-breaker for equally aligned targets.
                if (best == null || dot > bestDot + 1e-9 || (Math.abs(dot - bestDot) <= 1e-9 && dist < bestDist)) {
                    best = obj;
                    bestDot = dot;
                    bestDist = dist;
                }
            }

            return best;

        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * Overload with default values (range = 6, minDot = 0.6).
     */
    public static Object findCrosshairTarget(Object feeder, SimpleRemap remap) {
        return findCrosshairTarget(feeder, 6.0, 0.60, remap);
    }

    /**
     * Checks whether the target entity is within range of the source entity.
     */
    public static boolean isInRange(Object from, Object to, double maxDistance) {
        if (maxDistance < 0.0 || Double.isNaN(maxDistance)) return false;
        double[] p1 = McHelper.getEntityPos(from);
        double[] p2 = McHelper.getEntityPos(to);
        if (p1 == null || p2 == null) return false;

        double dx = p2[0]-p1[0], dy = p2[1]-p1[1], dz = p2[2]-p1[2];
        return (dx*dx + dy*dy + dz*dz) <= maxDistance*maxDistance;
    }
}