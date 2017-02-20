package com.ss.editor.ui.component.editing.terrain.paint;

import static com.ss.editor.ui.component.editing.terrain.TerrainEditingComponent.FIELD_PERCENT;
import static com.ss.editor.ui.component.editing.terrain.TerrainEditingComponent.LABEL_PERCENT;
import com.ss.editor.ui.control.choose.NamedChooseTextureControl;
import com.ss.editor.ui.css.CSSClasses;
import com.ss.editor.ui.css.CSSIds;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import org.jetbrains.annotations.NotNull;
import rlib.ui.control.input.FloatTextField;
import rlib.ui.util.FXUtils;

import java.nio.file.Path;

/**
 * The implementation of a list cell to edit/show a texture layer.
 *
 * @author JavaSaBr
 */
public class TextureLayerCell extends ListCell<TextureLayer> {

    /**
     * The diffuse texture chooser.
     */
    @NotNull
    private final NamedChooseTextureControl diffuseTextureControl;

    /**
     * The normal texture chooser.
     */
    @NotNull
    private final NamedChooseTextureControl normalTextureControl;

    /**
     * The texture scale field.
     */
    @NotNull
    private final FloatTextField scaleField;

    /**
     * The layer field.
     */
    @NotNull
    private final TextField layerField;

    /**
     * The settings container.
     */
    @NotNull
    private final GridPane settingContainer;

    /**
     * The flag of ignoring listeners.
     */
    private boolean ignoreListeners;

    public TextureLayerCell(@NotNull final ReadOnlyDoubleProperty prefWidth,
                            @NotNull final ReadOnlyDoubleProperty maxWidth) {
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);

        settingContainer = new GridPane();
        settingContainer.setId(CSSIds.TERRAIN_EDITING_TEXTURE_LAYER_SETTINGS);
        settingContainer.prefWidthProperty().bind(prefWidth);
        settingContainer.maxWidthProperty().bind(maxWidth);

        final Label layerLabel = new Label("Layer:");
        layerLabel.setId(CSSIds.ABSTRACT_PARAM_CONTROL_PARAM_NAME_SINGLE_ROW);
        layerLabel.prefWidthProperty().bind(settingContainer.widthProperty().multiply(LABEL_PERCENT));

        layerField = new TextField();
        layerField.setId(CSSIds.ABSTRACT_PARAM_CONTROL_COMBO_BOX);
        layerField.setEditable(false);
        layerField.prefWidthProperty().bind(settingContainer.widthProperty().multiply(FIELD_PERCENT));

        diffuseTextureControl = new NamedChooseTextureControl("Diffuse");
        diffuseTextureControl.setChangeHandler(this::updateDiffuse);

        normalTextureControl = new NamedChooseTextureControl("Normal");
        normalTextureControl.setChangeHandler(this::updateNormal);

        final Label scaleLabel = new Label("Scale:");
        scaleLabel.setId(CSSIds.ABSTRACT_PARAM_CONTROL_PARAM_NAME_SINGLE_ROW);
        scaleLabel.prefWidthProperty().bind(settingContainer.widthProperty().multiply(LABEL_PERCENT));

        scaleField = new FloatTextField();
        scaleField.setId(CSSIds.ABSTRACT_PARAM_CONTROL_COMBO_BOX);
        scaleField.setMinMax(0.0001F, Integer.MAX_VALUE);
        scaleField.addChangeListener((observable, oldValue, newValue) -> updateScale(newValue));
        scaleField.prefWidthProperty().bind(settingContainer.widthProperty().multiply(FIELD_PERCENT));

        settingContainer.add(layerLabel, 0, 0);
        settingContainer.add(layerField, 1, 0);
        settingContainer.add(diffuseTextureControl, 0, 1, 2, 1);
        settingContainer.add(normalTextureControl, 0, 2, 2, 1);
        settingContainer.add(scaleLabel, 0, 3);
        settingContainer.add(scaleField, 1, 3);

        FXUtils.addClassTo(this, CSSClasses.TRANSPARENT_LIST_CELL);
        FXUtils.addClassTo(this, CSSClasses.LIST_CELL_WITHOUT_PADDING);
        FXUtils.addClassTo(layerLabel, CSSClasses.SPECIAL_FONT_13);
        FXUtils.addClassTo(layerField, CSSClasses.SPECIAL_FONT_13);
        FXUtils.addClassTo(scaleLabel, CSSClasses.SPECIAL_FONT_13);
        FXUtils.addClassTo(scaleField, CSSClasses.SPECIAL_FONT_13);
    }

    /**
     * @return the texture scale field.
     */
    @NotNull
    private FloatTextField getScaleField() {
        return scaleField;
    }

    /**
     * @return the diffuse texture chooser.
     */
    @NotNull
    private NamedChooseTextureControl getDiffuseTextureControl() {
        return diffuseTextureControl;
    }

    /**
     * @return the normal texture chooser.
     */
    @NotNull
    private NamedChooseTextureControl getNormalTextureControl() {
        return normalTextureControl;
    }

    /**
     * @return the layer field.
     */
    @NotNull
    private TextField getLayerField() {
        return layerField;
    }

    /**
     * @return the settings container.
     */
    @NotNull
    private GridPane getSettingContainer() {
        return settingContainer;
    }

    /**
     * @param ignoreListeners true if need to ignore listeners.
     */
    private void setIgnoreListeners(final boolean ignoreListeners) {
        this.ignoreListeners = ignoreListeners;
    }

    /**
     * @return true if need to ignore listeners.
     */
    private boolean isIgnoreListeners() {
        return ignoreListeners;
    }

    /**
     * Update texture scale.
     */
    private void updateScale(@NotNull final Float newValue) {
        if (isIgnoreListeners()) return;
        getItem().setScale(newValue);

    }

    private void updateNormal() {
        if (isIgnoreListeners()) return;

    }

    /**
     * Update a diffuse texture.
     */
    private void updateDiffuse() {
        if (isIgnoreListeners()) return;
        final NamedChooseTextureControl diffuseTextureControl = getDiffuseTextureControl();
        final Path textureFile = diffuseTextureControl.getTextureFile();
        getItem().setDiffuseFile(textureFile);
    }

    @Override
    protected void updateItem(final TextureLayer item, final boolean empty) {
        super.updateItem(item, empty);

        setText("");

        if (item == null) {
            setGraphic(null);
            return;
        }

        setIgnoreListeners(true);
        try {

            final FloatTextField scaleField = getScaleField();
            scaleField.setValue(item.getScale());

            final NamedChooseTextureControl normalTextureControl = getNormalTextureControl();
            normalTextureControl.setTextureFile(item.getNormalFile());

            final NamedChooseTextureControl diffuseTextureControl = getDiffuseTextureControl();
            diffuseTextureControl.setTextureFile(item.getDiffuseFile());

            final TextField layerField = getLayerField();
            layerField.setText(String.valueOf(item.getLayer()));

        } finally {
            setIgnoreListeners(false);
        }

        setGraphic(getSettingContainer());
    }
}
