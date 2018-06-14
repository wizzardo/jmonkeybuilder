package com.ss.editor.ui.component.creator;

import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.ui.component.creator.impl.EmptyFileCreator;
import com.ss.editor.ui.component.creator.impl.EmptyModelCreator;
import com.ss.editor.ui.component.creator.impl.EmptySceneCreator;
import com.ss.editor.ui.component.creator.impl.FolderCreator;
import com.ss.editor.ui.component.creator.impl.material.MaterialFileCreator;
import com.ss.editor.ui.component.creator.impl.material.definition.MaterialDefinitionFileCreator;
import com.ss.editor.ui.component.creator.impl.texture.SingleColorTextureFileCreator;
import com.ss.rlib.common.logging.Logger;
import com.ss.rlib.common.logging.LoggerManager;
import com.ss.rlib.common.util.array.Array;
import com.ss.rlib.common.util.array.ArrayFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Path;

/**
 * The registry with file creators.
 *
 * @author JavaSaBr
 */
public class FileCreatorRegistry {

    private static final Logger LOGGER = LoggerManager.getLogger(FileCreatorRegistry.class);

    private static final FileCreatorRegistry INSTANCE = new FileCreatorRegistry();

    @FromAnyThread
    public @NotNull static FileCreatorRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * The list of file creator descriptions.
     */
    @NotNull
    private final Array<FileCreatorDescription> descriptions;

    private FileCreatorRegistry() {
        this.descriptions = ArrayFactory.newCopyOnModifyArray(FileCreatorDescription.class);
        register(MaterialFileCreator.DESCRIPTION);
        register(MaterialDefinitionFileCreator.DESCRIPTION);
        register(EmptyFileCreator.DESCRIPTION);
        register(FolderCreator.DESCRIPTION);
        register(EmptyModelCreator.DESCRIPTION);
        register(SingleColorTextureFileCreator.DESCRIPTION);
        register(EmptySceneCreator.DESCRIPTION);
        LOGGER.info("initialized.");
    }

    /**
     * Add a new creator description.
     *
     * @param description the new description.
     */
    @FromAnyThread
    public void register(@NotNull FileCreatorDescription description) {
        descriptions.add(description);
    }

    /**
     * Gets descriptions.
     *
     * @return the list of file creator descriptions.
     */
    @FromAnyThread
    public @NotNull Array<FileCreatorDescription> getDescriptions() {
        return descriptions;
    }

    /**
     * Create a new creator of the description for the file.
     *
     * @param description the file creator description.
     * @param file        the file.
     * @return the file creator.
     */
    @FromAnyThread
    public @Nullable FileCreator newCreator(@NotNull FileCreatorDescription description, @NotNull Path file) {

        var constructor = description.getConstructor();
        try {
            return constructor.call();
        } catch (Exception e) {
            LOGGER.warning(e);
        }

        return null;
    }
}
