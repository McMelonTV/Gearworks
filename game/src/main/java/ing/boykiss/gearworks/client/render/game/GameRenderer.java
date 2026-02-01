package ing.boykiss.gearworks.client.render.game;

import ing.boykiss.gearworks.client.render.Window;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

public class GameRenderer {
    private final Thread renderThread;
    @Getter
    private Window window;

    private boolean isStarted = false;

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

        GL.createCapabilities();

        GL46.glEnable(GL46.GL_DEBUG_OUTPUT);
        GL46.glEnable(GL46.GL_DEBUG_OUTPUT_SYNCHRONOUS);

        GL46.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        while (!window.shouldClose()) {
            window.pollEvents();

            renderFrame();

            window.update();
        }

        window.cleanup();

        System.exit(0); // TODO: unsure how to properly handle this, for now this should be fine
    }

    private void processKeys(long window, int key, int scancode, int action, int mods) {
        if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
            GLFW.glfwSetWindowShouldClose(window, true);
    }

    private void processResize(long window, int width, int height) {

    }

    private void renderFrame() {
        GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT);
    }
}
