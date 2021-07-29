package com.ss.editor.ui.control.property.impl.particle;

import com.jme3.effect.ParticleEmitter;
import com.jme3.math.Vector2f;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.ui.control.property.impl.Vector2fPropertyControl;
import org.jetbrains.annotations.NotNull;

/**
 * The implementation of the {@link Vector2fPropertyControl} to edit images count of the {@link
 * ParticleEmitter}*.
 *
 * @author JavaSaBr.
 */
public class ParticleEmitterImagesModelPropertyControl extends Vector2fPropertyControl<ModelChangeConsumer, ParticleEmitter> {

    public ParticleEmitterImagesModelPropertyControl(
            @NotNull Vector2f element,
            @NotNull String paramName,
            @NotNull ModelChangeConsumer changeConsumer
    ) {
        super(element, paramName, changeConsumer);
        getXField().setMinMax(1f, (float) Integer.MAX_VALUE);
        getYField().setMinMax(1f, (float) Integer.MAX_VALUE);
    }
}
