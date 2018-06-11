package com.ss.editor.manager;

import static com.ss.rlib.common.plugin.impl.PluginSystemFactory.newBasePluginSystem;
import com.ss.editor.JmeApplication;
import com.ss.editor.annotation.BackgroundThread;
import com.ss.editor.annotation.FromAnyThread;
import com.ss.editor.annotation.FxThread;
import com.ss.editor.annotation.JmeThread;
import com.ss.editor.config.Config;
import com.ss.editor.file.converter.FileConverterRegistry;
import com.ss.editor.manager.AsyncEventManager.SingleAsyncEventHandlerBuilder;
import com.ss.editor.plugin.EditorPlugin;
import com.ss.editor.plugin.api.settings.SettingsProviderRegistry;
import com.ss.editor.ui.component.asset.tree.AssetTreeContextMenuFillerRegistry;
import com.ss.editor.ui.component.creator.FileCreatorRegistry;
import com.ss.editor.ui.component.editor.EditorRegistry;
import com.ss.editor.ui.component.painting.PaintingComponentRegistry;
import com.ss.editor.ui.control.property.builder.PropertyBuilderRegistry;
import com.ss.editor.ui.control.tree.node.factory.TreeNodeFactoryRegistry;
import com.ss.editor.ui.event.impl.*;
import com.ss.editor.ui.preview.FilePreviewFactoryRegistry;
import com.ss.editor.util.EditorUtil;
import com.ss.rlib.common.logging.Logger;
import com.ss.rlib.common.logging.LoggerManager;
import com.ss.rlib.common.manager.InitializeManager;
import com.ss.rlib.common.plugin.ConfigurablePluginSystem;
import com.ss.rlib.common.plugin.exception.PreloadPluginException;
import com.ss.rlib.common.util.FileUtils;
import com.ss.rlib.common.util.ObjectUtils;
import com.ss.rlib.common.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * The manager to work with plugins.
 *
 * @author JavaSaBr
 */
public class PluginManager {

    private static final Logger LOGGER = LoggerManager.getLogger(PluginManager.class);
    private static final ExecutorManager EXECUTOR_MANAGER = ExecutorManager.getInstance();

    @Nullable
    private static PluginManager instance;

    public static @NotNull PluginManager getInstance() {

        if (instance == null) {
            instance = new PluginManager();
        }

        return instance;
    }

    @NotNull
    private final CountDownLatch waiter;

    /**
     * The plugin system.
     */
    @Nullable
    private volatile ConfigurablePluginSystem pluginSystem;

    private PluginManager() {
        InitializeManager.valid(getClass());

        this.waiter = new CountDownLatch(1);

        ExecutorManager.getInstance()
                .addBackgroundTask(this::loadPluginsInBackground);

        SingleAsyncEventHandlerBuilder.of(EditorFinishedLoadingEvent.EVENT_TYPE)
                .add(this::onFinishLoading);
        SingleAsyncEventHandlerBuilder.of(JmeContextCreatedEvent.EVENT_TYPE)
                .add(this::onAfterCreateJmeContext);
        SingleAsyncEventHandlerBuilder.of(FxContextCreatedEvent.EVENT_TYPE)
                .add(this::onAfterCreateJavaFxContext);

        AsyncEventManager.CombinedAsyncEventHandlerBuilder.of(this::registeredExtensions)
                .add(FxSceneCreatedEvent.EVENT_TYPE)
                .add(PluginsRegisteredResourcesEvent.EVENT_TYPE)
                .buildAndRegister();

    }

    /**
     * Get the plugin system.
     *
     * @return the plugin system.
     */
    @FromAnyThread
    private @NotNull ConfigurablePluginSystem getPluginSystem() {
        return ObjectUtils.notNull(pluginSystem);
    }

    @BackgroundThread
    private void loadPluginsInBackground() {

        var pluginSystem = newBasePluginSystem(getClass().getClassLoader());
        pluginSystem.setAppVersion(Config.APP_VERSION);

        var folderInUserHome = Config.getAppFolderInUserHome();
        var embeddedPath = System.getProperty("editor.embedded.plugins.path");

        if (embeddedPath != null && Files.exists(Paths.get(embeddedPath))) {
            var embeddedPluginPath = Paths.get(embeddedPath);
            LOGGER.debug(this, "embedded plugin path: " + embeddedPluginPath);
            pluginSystem.configureEmbeddedPluginPath(embeddedPluginPath);
        } else {
            var rootFolder = Utils.getRootFolderFromClass(JmeApplication.class);
            var embeddedPluginPath = rootFolder.resolve("embedded-plugins");
            LOGGER.debug(this, "embedded plugin path: " + embeddedPluginPath);
            if (Files.exists(embeddedPluginPath)) {
                pluginSystem.configureEmbeddedPluginPath(embeddedPluginPath);
            } else {
                LOGGER.warning(this, "The embedded plugin folder doesn't exists.");
            }
        }

        var userPluginsFolder = folderInUserHome.resolve("plugins");

        LOGGER.debug(this, "installation plugin path: " + userPluginsFolder);

        if (!Files.exists(userPluginsFolder)) {
            FileUtils.createDirectories(userPluginsFolder);
        }

        pluginSystem.configureInstallationPluginsPath(userPluginsFolder);
        try {
            pluginSystem.preLoad();
        } catch (PreloadPluginException e) {
            FileUtils.delete(e.getPath());
            throw e;
        }

        pluginSystem.initialize();

        this.pluginSystem = pluginSystem;

        waiter.countDown();

        var eventManager = AsyncEventManager.getInstance();
        eventManager.notify(new PluginsLoadedEvent());

        handlePlugins(editorPlugin ->
                editorPlugin.register(ResourceManager.getInstance()));

        eventManager.notify(new PluginsRegisteredResourcesEvent());

    }

    /**
     * Register all extension of all plugins.
     */
    @BackgroundThread
    private void registeredExtensions() {

        var executorManager = ExecutorManager.getInstance();
        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(FileCreatorRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsFileCreatorsRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(EditorRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsEditorsRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(FileIconManager.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsFileIconFindersRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(TreeNodeFactoryRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsTreeNodeFactoriesRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(PropertyBuilderRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsPropertyBuildersRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(FileConverterRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsFileConvertersRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(FilePreviewFactoryRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsFilePreviewFactoriesRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(AssetTreeContextMenuFillerRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsAssetTreeContextMenuFillersRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(AssetTreeContextMenuFillerRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsAssetTreeContextMenuFillersRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                editorPlugin.register(SettingsProviderRegistry.getInstance()));

            AsyncEventManager.getInstance()
                .notify(new PluginsSettingsProvidersRegisteredEvent());
        });

        executorManager.addBackgroundTask(() -> {

            handlePlugins(editorPlugin ->
                    editorPlugin.register(PaintingComponentRegistry.getInstance()));

            AsyncEventManager.getInstance()
                    .notify(new PluginsPaintingComponentsRegisteredEvent());
        });
    }

    /**
     * Install a new plugin to the system.
     *
     * @param path the path to the plugin.
     */
    public void installPlugin(@NotNull Path path) {
        getPluginSystem().installPlugin(path, false);
    }

    /**
     * Remove a plugin from this editor.
     *
     * @param plugin the plugin.
     */
    public void removePlugin(@NotNull EditorPlugin plugin) {
        pluginSystem.removePlugin(plugin);
    }

    /**
     * Do some things after when JME context was created.
     */
    @BackgroundThread
    private void onAfterCreateJmeContext() {
        handlePlugins(editorPlugin -> {

            editorPlugin.onAfterCreateJmeContext(getPluginSystem());

            var classLoader = editorPlugin.getContainer().
                    getClassLoader();

            EXECUTOR_MANAGER.addFxTask(() -> {
                var assetManager = EditorUtil.getAssetManager();
                assetManager.addClassLoader(classLoader);
            });
        });
    }

    /**
     * Do some things after when JavaFX context was created.
     */
    @FxThread
    private void onAfterCreateJavaFxContext() {
        pluginSystem.getPlugins().stream()
                .filter(EditorPlugin.class::isInstance)
                .map(EditorPlugin.class::cast)
                .forEach(editorPlugin -> editorPlugin.onAfterCreateJavaFxContext(pluginSystem));
    }

    /**
     * Do some things before when the editor is ready to work.
     */
    @BackgroundThread
    private void onFinishLoading() {

        Utils.run(waiter, CountDownLatch::await);

        var pluginSystem = getPluginSystem();
        pluginSystem.getPlugins().stream()
                .filter(EditorPlugin.class::isInstance)
                .map(EditorPlugin.class::cast)
                .forEach(editorPlugin -> editorPlugin.onFinishLoading(pluginSystem));
    }

    /**
     * Handle each loaded plugin now.
     *
     * @param consumer the consumer.
     */
    @FromAnyThread
    public void handlePluginsNow(@NotNull Consumer<EditorPlugin> consumer) {
        handlePlugins(consumer);
    }

    /**
     * Handle each loaded plugin in background.
     *
     * @param consumer the consumer.
     */
    @FromAnyThread
    public void handlePluginsInBackground(@NotNull Consumer<EditorPlugin> consumer) {
        EXECUTOR_MANAGER.addBackgroundTask(() -> handlePlugins(consumer));
    }

    /**
     * Handle each loaded plugin in background and execute the result task in result.
     *
     * @param consumer the consumer.
     * @param result   the result task.
     */
    @FromAnyThread
    public void handlePluginsInBackground(@NotNull Consumer<EditorPlugin> consumer, @NotNull Runnable result) {
        EXECUTOR_MANAGER.addBackgroundTask(() -> {
            handlePlugins(consumer);
            result.run();
        });
    }

    @FromAnyThread
    private void handlePlugins(@NotNull Consumer<EditorPlugin> consumer) {
        getPluginSystem().getPlugins().stream()
                .filter(EditorPlugin.class::isInstance)
                .map(EditorPlugin.class::cast)
                .forEach(editorPlugin -> safeApply(consumer, editorPlugin));
    }

    @FromAnyThread
    private void safeApply(@NotNull Consumer<EditorPlugin> consumer, @NotNull EditorPlugin editorPlugin) {
        try {
            consumer.accept(editorPlugin);
        } catch (Throwable e) {
            LOGGER.warning(e);
        }
    }
}
