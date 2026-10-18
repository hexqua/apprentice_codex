package jp.aquafactory.apprenticecodex.spell.shock;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/** 折れ線の中心位置を保ち、色の帯まで前後の区間で共有する描画用断面。 */
final class BoltRibbonGeometry {
    private static final double EPSILON = 1.0e-4;
    private static final double MITER_LIMIT = 2.0;

    private BoltRibbonGeometry() {}

    static List<Section> build(List<Vec3> path, Vec3 camera, double halfWidth) {
        var points = new ArrayList<Vec3>();
        for (var point : path) {
            if (points.isEmpty() || points.getLast().distanceToSqr(point) > EPSILON * EPSILON) {
                points.add(point);
            }
        }
        if (points.size() < 2) return List.of();

        var sides = new ArrayList<Vec3>();
        var normals = new ArrayList<Vec3>();
        var lengths = new ArrayList<Double>();
        for (int i = 1; i < points.size(); i++) {
            var start = points.get(i - 1);
            var end = points.get(i);
            var direction = end.subtract(start).normalize();
            var view = camera.subtract(start.add(end).scale(0.5));
            var side = direction.cross(view.normalize());
            if (side.lengthSqr() < EPSILON * EPSILON) {
                // 視線と区間が重なると幅方向を決められないため、前の区間を優先して安定させる。
                var reference = sides.isEmpty() ? new Vec3(0, 1, 0) : sides.getLast();
                side = reference.subtract(direction.scale(reference.dot(direction)));
                if (side.lengthSqr() < EPSILON * EPSILON) {
                    side = direction.cross(new Vec3(1, 0, 0));
                }
                if (side.lengthSqr() < EPSILON * EPSILON) {
                    side = direction.cross(new Vec3(0, 0, 1));
                }
            }
            side = side.normalize();
            sides.add(side);
            normals.add(side.cross(direction).normalize());
            lengths.add(start.distanceTo(end));
        }

        var sections = new ArrayList<Section>();
        double distance = 0;
        sections.add(section(points.getFirst(), sides.getFirst(), normals.getFirst(), distance));
        for (int i = 1; i < points.size() - 1; i++) {
            distance += lengths.get(i - 1);
            var point = points.get(i);
            var before = sides.get(i - 1);
            var after = sides.get(i);
            var normal = normals.get(i - 1).add(normals.get(i)).normalize();
            if (normal.lengthSqr() < EPSILON * EPSILON) normal = normals.get(i - 1);
            var bisector = before.add(after).normalize();
            var denominator = bisector.dot(before);
            double extension = denominator > EPSILON ? 1.0 / denominator : Double.POSITIVE_INFINITY;
            double shortLength = Math.min(lengths.get(i - 1), lengths.get(i));
            double limit = Math.clamp(shortLength / (2.0 * halfWidth), 1.0, MITER_LIMIT);
            if (extension <= limit) {
                sections.add(section(point, bisector.scale(extension), normal, distance));
            } else {
                // 鋭角は外側を面取りする。内側も短い区間を突き抜けない長さに制限する。
                if (bisector.lengthSqr() < EPSILON * EPSILON) bisector = before;
                double innerLength = Math.min(limit, shortLength / (2.0 * halfWidth));
                boolean outerRight = before.cross(after).dot(camera.subtract(point)) >= 0;
                var inner = bisector.scale(outerRight ? -innerLength : innerLength);
                sections.add(new Section(point, outerRight ? inner : before.scale(-1),
                        outerRight ? before : inner, normal, distance));
                sections.add(new Section(point, outerRight ? inner : after.scale(-1),
                        outerRight ? after : inner, normal, distance));
            }
        }
        distance += lengths.getLast();
        sections.add(section(points.getLast(), sides.getLast(), normals.getLast(), distance));
        return sections;
    }

    private static Section section(Vec3 center, Vec3 side, Vec3 normal, double distance) {
        return new Section(center, side.scale(-1), side, normal, distance);
    }

    // offsetは外層・内層で共有し、レイヤーごとに幅だけを掛ける。面取りの接続面も同じUVでつなぐ。
    record Section(Vec3 center, Vec3 leftOffset, Vec3 rightOffset, Vec3 normal, double distance) {
        Vec3 left(double halfWidth) {
            return center.add(leftOffset.scale(halfWidth));
        }

        Vec3 right(double halfWidth) {
            return center.add(rightOffset.scale(halfWidth));
        }
    }
}
