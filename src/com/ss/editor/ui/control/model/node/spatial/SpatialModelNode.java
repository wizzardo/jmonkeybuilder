package com.ss.editor.ui.control.model.node.spatial;

import static com.ss.editor.ui.control.tree.node.ModelNodeFactory.createFor;
import static java.util.Objects.requireNonNull;

import com.jme3.light.Light;
import com.jme3.light.LightList;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.control.Control;
import com.ss.editor.Messages;
import com.ss.editor.control.transform.SceneEditorControl;
import com.ss.editor.model.undo.editor.ChangeConsumer;
import com.ss.editor.ui.Icons;
import com.ss.editor.ui.control.model.node.control.ControlModelNode;
import com.ss.editor.ui.control.model.node.light.LightModelNode;
import com.ss.editor.ui.control.model.tree.action.AddUserDataAction;
import com.ss.editor.ui.control.model.tree.action.RemoveNodeAction;
import com.ss.editor.ui.control.model.tree.action.RenameNodeAction;
import com.ss.editor.ui.control.model.tree.action.control.CreateCharacterAction;
import com.ss.editor.ui.control.model.tree.action.control.CreateCustomControlAction;
import com.ss.editor.ui.control.model.tree.action.control.CreateMotionControlAction;
import com.ss.editor.ui.control.model.tree.action.control.CreateRigidBodyControlAction;
import com.ss.editor.ui.control.model.tree.action.operation.RenameNodeOperation;
import com.ss.editor.ui.control.tree.AbstractNodeTree;
import com.ss.editor.ui.control.tree.node.ModelNode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javafx.collections.ObservableList;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.ImageView;
import rlib.util.StringUtils;
import rlib.util.array.Array;
import rlib.util.array.ArrayFactory;

/**
 * The implementation of the {@link ModelNode} for representing the {@link Spatial} in the editor.
 *
 * @author JavaSaBr
 */
public class SpatialModelNode<T extends Spatial> extends ModelNode<T> {

    public SpatialModelNode(@NotNull final T element, final long objectId) {
        super(element, objectId);
    }

    @Override
    public void fillContextMenu(@NotNull final AbstractNodeTree<?> nodeTree, @NotNull final ObservableList<MenuItem> items) {
        if (canEditName()) items.add(new RenameNodeAction(nodeTree, this));
        if (canRemove()) items.add(new RemoveNodeAction(nodeTree, this));
        items.add(new AddUserDataAction(nodeTree, this));
        super.fillContextMenu(nodeTree, items);
    }

    @Override
    public boolean canRemove() {
        final Node parent = getElement().getParent();
        return parent != null && parent.getUserData(SceneEditorControl.class.getName()) != Boolean.TRUE;
    }

    @Nullable
    protected Menu createCreationMenu(@NotNull final AbstractNodeTree<?> nodeTree) {

        final Menu menu = new Menu(Messages.MODEL_NODE_TREE_ACTION_CREATE, new ImageView(Icons.ADD_18));

        final Menu createControlsMenu = new Menu(Messages.MODEL_NODE_TREE_ACTION_ADD_CONTROL, new ImageView(Icons.ADD_18));
        createControlsMenu.getItems().addAll(new CreateCustomControlAction(nodeTree, this),
                new CreateRigidBodyControlAction(nodeTree, this),
                new CreateMotionControlAction(nodeTree, this),
                new CreateCharacterAction(nodeTree, this));

        menu.getItems().add(createControlsMenu);

        return menu;
    }

    @NotNull
    @Override
    public String getName() {
        final String name = getElement().getName();
        return name == null ? "name is null" : name;
    }

    @Override
    public boolean canEditName() {
        return true;
    }

    @Override
    public boolean hasChildren() {
        return true;
    }

    @Override
    public void changeName(@NotNull final AbstractNodeTree<?> nodeTree, @NotNull final String newName) {
        if (StringUtils.equals(getName(), newName)) return;

        super.changeName(nodeTree, newName);

        final Spatial spatial = getElement();
        final ChangeConsumer consumer = requireNonNull(nodeTree.getChangeConsumer());
        consumer.execute(new RenameNodeOperation(spatial.getName(), newName, spatial));
    }

    @NotNull
    @Override
    public Array<ModelNode<?>> getChildren() {

        final Array<ModelNode<?>> result = ArrayFactory.newArray(ModelNode.class);
        final Spatial element = getElement();

        final LightList lightList = element.getLocalLightList();
        lightList.forEach(light -> result.add(createFor(light)));

        final int numControls = element.getNumControls();

        for (int i = 0; i < numControls; i++) {
            final Control control = element.getControl(i);
            result.add(createFor(control));
        }

        return result;
    }

    @Override
    public void add(@NotNull final ModelNode<?> child) {
        super.add(child);

        final T element = getElement();

        if (child instanceof LightModelNode) {
            final Light light = (Light) child.getElement();
            element.addLight(light);
        } else if (child instanceof ControlModelNode) {
            final Control control = (Control) child.getElement();
            element.addControl(control);
        }
    }

    @Override
    public void remove(@NotNull final ModelNode<?> child) {
        super.remove(child);

        final T element = getElement();

        if (child instanceof LightModelNode) {
            final Light light = (Light) child.getElement();
            element.removeLight(light);
        } else if (child instanceof ControlModelNode) {
            final Control control = (Control) child.getElement();
            element.removeControl(control);
        }
    }
}
