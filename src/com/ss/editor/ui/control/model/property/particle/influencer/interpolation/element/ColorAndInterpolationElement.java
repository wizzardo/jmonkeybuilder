package com.ss.editor.ui.control.model.property.particle.influencer.interpolation.element;

import static java.lang.Math.min;

import com.jme3.math.ColorRGBA;
import com.ss.editor.ui.control.model.property.particle.influencer.interpolation.control.ColorInfluencerControl;
import com.ss.editor.ui.css.CSSIds;
import com.ss.editor.ui.util.UIUtils;

import org.jetbrains.annotations.NotNull;

import javafx.scene.control.ColorPicker;
import javafx.scene.control.ComboBox;
import javafx.scene.paint.Color;
import tonegod.emitter.influencers.ColorInfluencer;
import tonegod.emitter.interpolation.Interpolation;

/**
 * The implementation of the element for {@link ColorInfluencerControl} for editing color and
 * interpolation.
 *
 * @author JavaSaBr
 */
public class ColorAndInterpolationElement extends InterpolationElement<ColorInfluencer, ColorPicker, ColorInfluencerControl> {

    public ColorAndInterpolationElement(@NotNull final ColorInfluencerControl control, final int index) {
        super(control, index);
    }

    @NotNull
    @Override
    protected String getEditableTitle() {
        return "Color:";
    }

    @Override
    protected ColorPicker createEditableControl() {

        final ColorPicker colorPicker = new ColorPicker();
        colorPicker.setId(CSSIds.MODEL_PARAM_CONTROL_COLOR_PICKER);
        colorPicker.prefWidthProperty().bind(widthProperty().multiply(0.3));
        colorPicker.valueProperty().addListener((observable, oldValue, newValue) -> processChange(newValue));

        return colorPicker;
    }

    private void processChange(@NotNull final Color newValue) {
        if (isIgnoreListeners()) return;
        final ColorRGBA newColor = UIUtils.convertColor(newValue);
        final ColorInfluencerControl control = getControl();
        control.requestToChange(newColor, getIndex());
    }

    /**
     * Reload this element.
     */
    public void reload() {

        final ColorInfluencerControl control = getControl();
        final ColorInfluencer influencer = control.getInfluencer();

        final ColorRGBA newColor = influencer.getColor(getIndex());
        final Interpolation newInterpolation = influencer.getInterpolation(getIndex());

        final float red = min(newColor.getRed(), 1F);
        final float green = min(newColor.getGreen(), 1F);
        final float blue = min(newColor.getBlue(), 1F);
        final float alpha = min(newColor.getAlpha(), 1F);

        final ColorPicker colorPicker = getEditableControl();
        colorPicker.setValue(new Color(red, green, blue, alpha));

        final ComboBox<Interpolation> interpolationComboBox = getInterpolationComboBox();
        interpolationComboBox.getSelectionModel().select(newInterpolation);
    }
}
