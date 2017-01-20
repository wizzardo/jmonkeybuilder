package com.ss.editor.state.editor.impl.model;

import com.jme3.app.Application;
import com.jme3.app.state.AppStateManager;
import com.jme3.asset.AssetManager;
import com.jme3.environment.generation.JobProgressAdapter;
import com.jme3.light.DirectionalLight;
import com.jme3.light.Light;
import com.jme3.light.LightProbe;
import com.jme3.material.Material;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.ss.editor.control.light.EditorLightControl;
import com.ss.editor.state.editor.impl.scene.AbstractSceneEditorAppState;
import com.ss.editor.ui.component.editor.impl.model.ModelFileEditor;
import com.ss.editor.util.NodeUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;
import rlib.util.dictionary.DictionaryFactory;
import rlib.util.dictionary.ObjectDictionary;
import tonegod.emitter.filter.TonegodTranslucentBucketFilter;

/**
 * The implementation of the {@link AbstractSceneEditorAppState} for the {@link ModelFileEditor}.
 *
 * @author JavaSaBr
 */
public class ModelEditorAppState extends AbstractSceneEditorAppState<ModelFileEditor, Spatial> {

    public static final String USER_DATA_IS_LIGHT = ModelEditorAppState.class.getName() + ".isLight";

    private final JobProgressAdapter<LightProbe> probeHandler = new JobProgressAdapter<LightProbe>() {

        @Override
        public void done(final LightProbe result) {
            if (!isInitialized()) return;
            notifyProbeComplete();
        }
    };

    /**
     * The table with models for presentation of the lights.
     */
    protected static final ObjectDictionary<Light.Type, Spatial> LIGHT_MODEL_TABLE;

    static {

        final AssetManager assetManager = EDITOR.getAssetManager();

        LIGHT_MODEL_TABLE = DictionaryFactory.newObjectDictionary();
        LIGHT_MODEL_TABLE.put(Light.Type.Point, assetManager.loadModel("graphics/models/light/point.j3o"));
        LIGHT_MODEL_TABLE.put(Light.Type.Directional, assetManager.loadModel("graphics/models/light/sun.j3o"));
        LIGHT_MODEL_TABLE.put(Light.Type.Spot, assetManager.loadModel("graphics/models/light/spot_lamp.j3o"));
        LIGHT_MODEL_TABLE.put(Light.Type.Probe, assetManager.loadModel("graphics/models/light/point.j3o"));
    }

    /**
     * The array controls for lights.
     */
    @NotNull
    protected final Array<EditorLightControl> editorLightControls;

    /**
     * The array of custom skies.
     */
    @NotNull
    protected final Array<Spatial> customSky;

    /**
     * The node for the placement of lights.
     */
    @NotNull
    private final Node lightNode;

    /**
     * The node for the placement of custom sky.
     */
    @NotNull
    private final Node customSkyNode;

    /**
     * The current fast sky.
     */
    private Spatial currentFastSky;

    /**
     * The flag of activity light of the camera.
     */
    private boolean lightEnabled;

    /**
     * The frame rate.
     */
    private int frame;

    public ModelEditorAppState(final ModelFileEditor fileEditor) {
        super(fileEditor);
        this.customSkyNode = new Node("Custom Sky");
        this.lightNode = new Node("Lights");
        this.customSky = ArrayFactory.newArray(Spatial.class);
        this.editorLightControls = ArrayFactory.newArray(EditorLightControl.class);

        final Node stateNode = getStateNode();
        stateNode.attachChild(getCustomSkyNode());

        setLightEnabled(true);
    }

    /**
     * @return the node for the placement of lights.
     */
    @NotNull
    protected Node getLightNode() {
        return lightNode;
    }

    /**
     * @return the array controls for lights.
     */
    @NotNull
    protected Array<EditorLightControl> getEditorLightControls() {
        return editorLightControls;
    }

    /**
     * @return the node for the placement of custom sky.
     */
    @NotNull
    private Node getCustomSkyNode() {
        return customSkyNode;
    }

    /**
     * @return the array of custom skies.
     */
    @NotNull
    private Array<Spatial> getCustomSky() {
        return customSky;
    }

    @Override
    public void notifyTransformed(@NotNull final Spatial spatial) {
        final ModelFileEditor fileEditor = getFileEditor();
        fileEditor.notifyTransformed(spatial);
    }

    /**
     * Activate the node with models.
     */
    private void notifyProbeComplete() {

        final Node stateNode = getStateNode();
        stateNode.attachChild(getModelNode());
        stateNode.attachChild(getToolNode());
        stateNode.attachChild(getLightNode());

        final Node customSkyNode = getCustomSkyNode();
        customSkyNode.detachAllChildren();

        final TonegodTranslucentBucketFilter translucentBucketFilter = EDITOR.getTranslucentBucketFilter();
        translucentBucketFilter.refresh();
    }

    /**
     * @param currentFastSky the current fast sky.
     */
    private void setCurrentFastSky(@Nullable final Spatial currentFastSky) {
        this.currentFastSky = currentFastSky;
    }

    /**
     * @return the current fast sky.
     */
    @Nullable
    private Spatial getCurrentFastSky() {
        return currentFastSky;
    }

    /**
     * @return true if the light of the camera is enabled.
     */
    private boolean isLightEnabled() {
        return lightEnabled;
    }

    /**
     * @param lightEnabled the flag of activity light of the camera.
     */
    private void setLightEnabled(final boolean lightEnabled) {
        this.lightEnabled = lightEnabled;
    }

    @Override
    public void initialize(@NotNull final AppStateManager stateManager, @NotNull final Application application) {
        super.initialize(stateManager, application);
        frame = 0;
    }

    @Override
    public void cleanup() {
        super.cleanup();

        final Node stateNode = getStateNode();
        stateNode.detachChild(getModelNode());
        stateNode.detachChild(getToolNode());
        stateNode.detachChild(getLightNode());
    }

    @Override
    public void update(final float tpf) {
        super.update(tpf);

        if (frame == 2) {

            final Node customSkyNode = getCustomSkyNode();

            final Array<Spatial> customSky = getCustomSky();
            customSky.forEach(spatial -> customSkyNode.attachChild(spatial.clone(false)));

            EDITOR.updateProbe(probeHandler);
        }

        frame++;
    }

    @Override
    protected boolean needUpdateCameraLight() {
        return true;
    }

    @Override
    protected boolean needLightForCamera() {
        return true;
    }

    /**
     * Update light.
     */
    public void updateLightEnabled(final boolean enabled) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> updateLightEnabledImpl(enabled));
    }

    /**
     * The process of updating the light.
     */
    private void updateLightEnabledImpl(boolean enabled) {
        if (enabled == isLightEnabled()) return;

        final DirectionalLight light = getLightForCamera();
        final Node stateNode = getStateNode();

        if (enabled) {
            stateNode.addLight(light);
        } else {
            stateNode.removeLight(light);
        }

        setLightEnabled(enabled);
    }

    /**
     * Change the fast sky.
     */
    public void changeFastSky(final Spatial fastSky) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> changeFastSkyImpl(fastSky));
    }

    /**
     * The process of changing the fast sky.
     */
    private void changeFastSkyImpl(final Spatial fastSky) {

        final Node stateNode = getStateNode();
        final Spatial currentFastSky = getCurrentFastSky();

        if (currentFastSky != null) {
            stateNode.detachChild(currentFastSky);
        }

        if (fastSky != null) {
            stateNode.attachChild(fastSky);
        }

        stateNode.detachChild(getModelNode());
        stateNode.detachChild(getToolNode());
        stateNode.detachChild(getLightNode());

        setCurrentFastSky(fastSky);

        frame = 0;
    }

    /**
     * Add the custom sky.
     */
    public void addCustomSky(@NotNull final Spatial sky) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> addCustomSkyImpl(sky));
    }

    /**
     * The process of adding the custom sky.
     */
    private void addCustomSkyImpl(@NotNull final Spatial sky) {
        final Array<Spatial> customSky = getCustomSky();
        customSky.add(sky);
    }

    /**
     * Add the light.
     */
    public void addLight(@NotNull final Light light) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> addLightImpl(light));
    }

    /**
     * The process of adding the light.
     */
    private void addLightImpl(final Light light) {

        final Spatial original = LIGHT_MODEL_TABLE.get(light.getType());
        if (original == null) return;

        final Spatial newModel = original.clone();
        newModel.setUserData(USER_DATA_IS_LIGHT, Boolean.TRUE);
        newModel.setLocalScale(0.02F);

        final Geometry geometry = NodeUtils.findGeometry(newModel);

        if (geometry == null) {
            LOGGER.warning(this, "not found geometry for the node " + newModel);
            return;
        }

        final Material material = geometry.getMaterial();
        material.setColor("Color", light.getColor());

        final EditorLightControl editorLightControl = new EditorLightControl(light);
        newModel.addControl(editorLightControl);

        final Array<EditorLightControl> lights = getEditorLightControls();
        lights.add(editorLightControl);

        final Node lightNode = getLightNode();
        lightNode.attachChild(newModel);
    }

    /**
     * Remove the custom sky.
     */
    public void removeCustomSky(@NotNull final Spatial sky) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> removeCustomSkyImpl(sky));
    }

    /**
     * The process of removing the custom sky.
     */
    private void removeCustomSkyImpl(@NotNull final Spatial sky) {
        final Array<Spatial> customSky = getCustomSky();
        customSky.slowRemove(sky);
    }

    /**
     * Remove the light.
     */
    public void removeLight(@NotNull final Light light) {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> removeLightImpl(light));
    }

    /**
     * The process of removing the light.
     */
    private void removeLightImpl(@NotNull final Light light) {

        EditorLightControl control = null;

        final Array<EditorLightControl> lights = getEditorLightControls();

        for (final EditorLightControl lightControl : lights) {
            if (lightControl.getLight() == light) {
                control = lightControl;
                break;
            }
        }

        if (control == null) return;

        final Spatial spatial = control.getSpatial();
        spatial.removeFromParent();

        lights.fastRemove(control);
    }

    /**
     * Update the light probe.
     */
    public void updateLightProbe() {
        EXECUTOR_MANAGER.addEditorThreadTask(() -> {

            final Node stateNode = getStateNode();
            stateNode.detachChild(getModelNode());
            stateNode.detachChild(getToolNode());
            stateNode.detachChild(getLightNode());

            frame = 0;
        });
    }

    @Override
    protected void notifyChangedCamera(@NotNull final Vector3f cameraLocation, final float hRotation,
                                       final float vRotation, final float targetDistance) {
        EXECUTOR_MANAGER.addFXTask(() -> getFileEditor().notifyChangedCamera(cameraLocation, hRotation, vRotation, targetDistance));
    }

    @Override
    protected void notifySelected(@Nullable final Object object) {
        getFileEditor().notifySelected(object);
    }

    @Override
    public String toString() {
        return "ModelEditorAppState{" +
                ", lightEnabled=" + lightEnabled +
                "} " + super.toString();
    }
}
