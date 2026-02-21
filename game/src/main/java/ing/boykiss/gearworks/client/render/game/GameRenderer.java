package ing.boykiss.gearworks.client.render.game;

import ing.boykiss.gearworks.client.render.Window;
import ing.boykiss.gearworks.client.render.font.Font;
import ing.boykiss.gearworks.common.util.ModList;
import lombok.Getter;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.ModMetadata;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

public class GameRenderer {
    private final Thread renderThread;

    @Getter
    private Window window;

    private boolean isStarted = false;
    private int targetFPS = 0;

    private Font font;

    public GameRenderer() {
        // OpenGL requires a platform thread
        renderThread = Thread.ofPlatform().unstarted(this::initThread);
    }

    public void start() {
        if (!isStarted) isStarted = true;
        renderThread.start();
    }

    private void initThread() {
        window = new Window(
                new Window.Properties(
                        1024,
                        576,
                        "Gearworks",
                        MemoryUtil.NULL,
                        MemoryUtil.NULL,
                        4,
                        6
                ),
                this::processResize,
                this::processKeys
        );

        this.font = new Font("font.ttf");

        GL46.glMatrixMode(GL46.GL_PROJECTION);
        GL46.glLoadIdentity();
        GL46.glOrtho(0, window.getWidth(), window.getHeight(), 0, -1, 1);  // top left origin
        GL46.glMatrixMode(GL46.GL_MODELVIEW);
        GL46.glLoadIdentity();

        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);

        GL46.glEnable(GL46.GL_TEXTURE_2D);

        GL46.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        long lastFrameNanos = System.nanoTime();
        long frameTargetNanos = 1_000_000_000 / (targetFPS > 0 ? targetFPS : Integer.MAX_VALUE);

        while (!window.shouldClose()) {
            window.pollEvents();

            long currentNanos = System.nanoTime();
            long deltaNanos = currentNanos - lastFrameNanos;

            if (deltaNanos >= frameTargetNanos) {
                renderFrame(deltaNanos);
                window.update();

                lastFrameNanos = currentNanos;
            } else {
                Thread.yield(); // TODO: should this be used?
            }
        }

        window.cleanup();

        System.exit(0); // TODO: unsure how to properly handle this, for now this should be fine
    }

    private void processKeys(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
            GLFW.glfwSetWindowShouldClose(window, true);
    }

    private void processResize(long window, int width, int height) {
        GL46.glViewport(0, 0, width, height);

        GL46.glMatrixMode(GL46.GL_PROJECTION);
        GL46.glLoadIdentity();
        GL46.glOrtho(0, width, height, 0, -1, 1);
        GL46.glMatrixMode(GL46.GL_MODELVIEW);
    }

    private void renderFrame(long deltaNanos) {
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT);

        int fps = (int) (1_000_000_000 / deltaNanos);

        font.drawText("FPS: " + fps, 50, 50);

        FabricLoader loader = FabricLoader.getInstance();
        font.drawText("Running " + loader.getEnvironmentType().name() + " v" + loader.getRawGameVersion() + " " + (loader.isDevelopmentEnvironment() ? "(Dev)" : "(Release)"), 50, 100);

        ModList modList = new ModList();

        font.drawText(modList.getAllMods().size() + " loaded mods.", 50, 130);

        record Category(String label, List<ModContainer> list) {
        }
        List<Category> categories = List.of(
                new Category("Mods", modList.getMods()),
                new Category("Libraries", modList.getLibraries()),
                new Category("Core", modList.getBuiltins())
        );

        int y = 160;
        for (Category category : categories) {
            if (category.list().isEmpty()) continue;
            font.drawText(category.label() + " (" + category.list().size() + ")" + ":", 50, y);
            for (ModContainer mod : category.list()) {
                ModMetadata meta = mod.getMetadata();
                y += 20;
                font.drawText(meta.getName() + " v" + meta.getVersion() + " (" + meta.getId() + ")", 60, y);
            }
            y += 30;
        }
    }
}
