package com.ss.editor.util;

import static rlib.util.ClassUtils.unsafeCast;

import com.jme3.asset.AssetKey;
import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.material.Material;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

import rlib.util.StringUtils;
import rlib.util.array.Array;

/**
 * The class with utility methods for working with {@link Node}.
 *
 * @author JavaSaBr
 */
public class NodeUtils {

    /**
     * Find the parent of the model.
     */
    @Nullable
    public static <T> T findParent(@NotNull final Spatial spatial, @NotNull final Predicate<Spatial> condition) {
        if (condition.test(spatial)) return unsafeCast(spatial);
        final Node parent = spatial.getParent();
        if (parent == null) return null;
        return findParent(parent, condition);
    }

    /**
     * Find the first geometry in the {@link Spatial}.
     */
    @Nullable
    public static Geometry findGeometry(@NotNull final Spatial spatial) {
        if (!(spatial instanceof Node)) return null;

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            final Geometry geometry = findGeometry(children);
            if (geometry != null) return geometry;
            if (children instanceof Geometry) return (Geometry) children;
        }

        return null;
    }

    /**
     * Collect all geometries from the asset path.
     */
    public static void addGeometryWithMaterial(@NotNull final Spatial spatial, @NotNull final Array<Geometry> container, @NotNull final String assetPath) {
        if (StringUtils.isEmpty(assetPath)) return;

        if (spatial instanceof Geometry) {

            final Geometry geometry = (Geometry) spatial;
            final Material material = geometry.getMaterial();
            final String assetName = material == null ? null : material.getAssetName();

            if (StringUtils.equals(assetName, assetPath)) {
                container.add(geometry);
            }

            return;

        } else if (!(spatial instanceof Node)) {
            return;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addGeometryWithMaterial(children, container, assetPath);
        }
    }

    /**
     * Collect all geometries from the asset path.
     */
    public static void addSpatialWithAssetPath(@NotNull final Spatial spatial, @NotNull final Array<Spatial> container, @NotNull final String assetPath) {
        if (StringUtils.isEmpty(assetPath)) return;

        final AssetKey key = spatial.getKey();

        if (key != null && StringUtils.equals(key.getName(), assetPath)) {
            container.add(spatial);
        }

        if (!(spatial instanceof Node)) {
            return;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addSpatialWithAssetPath(children, container, assetPath);
        }
    }

    /**
     * Collect all geometries.
     */
    public static void addGeometry(@NotNull final Spatial spatial, @NotNull final Array<Geometry> container) {

        if (spatial instanceof Geometry) {
            container.add((Geometry) spatial);
            return;
        } else if (!(spatial instanceof Node)) {
            return;
        }

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addGeometry(children, container);
        }
    }

    /**
     * Collect all lights.
     */
    public static void addLight(@NotNull final Spatial spatial, @NotNull final Array<Light> container) {

        final LightList lightList = spatial.getLocalLightList();
        lightList.forEach(container::add);

        if (!(spatial instanceof Node)) return;

        final Node node = (Node) spatial;

        for (final Spatial children : node.getChildren()) {
            addLight(children, container);
        }
    }
}
