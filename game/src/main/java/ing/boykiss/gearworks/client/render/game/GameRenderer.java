package ing.boykiss.gearworks.client.render.game;

import ing.boykiss.gearworks.client.render.Window;
import ing.boykiss.gearworks.client.render.font.Font;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryUtil;

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

        GL46.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

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
    }
}
