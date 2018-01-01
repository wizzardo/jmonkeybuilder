package com.ss.editor.ui.control.model.tree.action.operation;


import com.jme3.light.Light;
import com.ss.editor.model.undo.editor.ModelChangeConsumer;
import com.ss.editor.model.undo.impl.AbstractEditorOperation;
import org.jetbrains.annotations.NotNull;

public class RenameLightOperation extends AbstractEditorOperation<ModelChangeConsumer> {
    /**
     * The constant PROPERTY_NAME.
     */
    public static final String PROPERTY_NAME = "name";

    /**
     * The old name.
     */
    @NotNull
    private final String oldName;

    /**
     * The new name.
     */
    @NotNull
    private final String newName;

    /**
     * The node.
     */
    @NotNull
    private final Light light;

    /**
     * Instantiates a new Rename node operation.
     *
     * @param oldName the old name
     * @param newName the new name
     * @param light the light
     */
    public RenameLightOperation(@NotNull final String oldName, @NotNull final String newName, @NotNull final Light light) {
        this.oldName = oldName;
        this.newName = newName;
        this.light = light;
    }

    @Override
    protected void redoImpl(@NotNull final ModelChangeConsumer editor) {
        EXECUTOR_MANAGER.addJMETask(() -> {
            light.setName(newName);
            EXECUTOR_MANAGER.addFXTask(() -> editor.notifyFXChangeProperty(light, PROPERTY_NAME));
        });
    }

    @Override
    protected void undoImpl(@NotNull final ModelChangeConsumer editor) {
        EXECUTOR_MANAGER.addJMETask(() -> {
            light.setName(oldName);
            EXECUTOR_MANAGER.addFXTask(() -> editor.notifyFXChangeProperty(light, PROPERTY_NAME));
        });
    }
}
