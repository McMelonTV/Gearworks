package ing.boykiss.gearworks.client;

import ing.boykiss.gearworks.common.Greeting;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL46;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class ClientMain {
    private static long window;

    private static boolean vsyncEnabled = false;

    private static boolean fpsLimiterEnabled = false;
    private static int fpsLimit = 500;
    private static long variableYieldTime = 0;
    private static long lastTime = 0;

    private static double lastFpsTime = 0.0;
    private static int frames = 0;
    private static int currentFps = 0;

    private static int textTextureId = 0;
    private static int textWidth = 0;
    private static int textHeight = 0;

    static void main(String[] args) {
        System.out.println(Greeting.getGreeting());

        init();
        loop();

        Callbacks.glfwFreeCallbacks(window);
        GLFW.glfwDestroyWindow(window);

        GLFW.glfwTerminate();
        GLFW.glfwSetErrorCallback(null).free();
    }

    private static void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        if (!GLFW.glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        window = GLFW.glfwCreateWindow(1024, 576, "Gearworks", MemoryUtil.NULL, MemoryUtil.NULL);
        if (window == MemoryUtil.NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        GLFW.glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_RELEASE)
                GLFW.glfwSetWindowShouldClose(window, true);
        });

        // Get the thread stack and push a new frame
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1); // int*
            IntBuffer pHeight = stack.mallocInt(1); // int*

            // Get the window size passed to glfwCreateWindow
            GLFW.glfwGetWindowSize(window, pWidth, pHeight);

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND) {
                GLFW.glfwSetWindowPos(
                        window,
                        (vidmode.width() - pWidth.get(0)) / 2,
                        (vidmode.height() - pHeight.get(0)) / 2
                );
            }
        } // the stack frame is popped automatically

        GLFW.glfwMakeContextCurrent(window);

        GL.createCapabilities();

        GL46.glEnable(GL46.GL_DEBUG_OUTPUT);
        GL46.glEnable(GL46.GL_DEBUG_OUTPUT_SYNCHRONOUS);

        if (vsyncEnabled) {
            GLFW.glfwSwapInterval(1);
        } else {
            GLFW.glfwSwapInterval(0);
        }

        GLFW.glfwShowWindow(window);
    }

    private static void loop() {
        GL.createCapabilities();

        GL46.glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

        lastFpsTime = GLFW.glfwGetTime();

        while (!GLFW.glfwWindowShouldClose(window)) {
            double currentTime = GLFW.glfwGetTime();

            GL46.glClear(GL46.GL_COLOR_BUFFER_BIT | GL46.GL_DEPTH_BUFFER_BIT);

            frames++;
            if (currentTime - lastFpsTime >= 1.0) {
                currentFps = frames;
                frames = 0;
                lastFpsTime = currentTime;

                updateFpsTexture("FPS: " + currentFps);
            }

            renderFpsText();

            GLFW.glfwSwapBuffers(window);

            if (fpsLimiterEnabled) {
                sync(fpsLimit);
            }

            GLFW.glfwPollEvents();
        }

        if (textTextureId != 0) {
            GL46.glDeleteTextures(textTextureId);
        }
    }

    /**
     * An accurate sync method that adapts automatically to the system
     * it runs on to provide reliable results.
     *
     * @param fps The desired frame rate, in frames per second
     * @author kappa (LWJGL Forums)
     */
    private static void sync(int fps) {
        if (fps <= 0) return;

        long sleepTime = 1000000000 / fps; // nanoseconds to sleep this frame
        long yieldTime = Math.min(sleepTime, variableYieldTime + sleepTime % (1000 * 1000));
        long overSleep = 0;

        try {
            while (true) {
                long t = System.nanoTime() - lastTime;

                if (t < sleepTime - yieldTime) {
                    Thread.sleep(1);
                } else if (t < sleepTime) {
                    // burn the last few nanoseconds with yield to avoid oversleeping
                    Thread.yield();
                } else {
                    overSleep = t - sleepTime;
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lastTime = System.nanoTime() - Math.min(overSleep, sleepTime);

            // Adaptive algorithm to improve over time
            if (overSleep > variableYieldTime) {
                // increase by 200 microseconds (1/5 a ms)
                variableYieldTime = Math.min(variableYieldTime + 200 * 1000, sleepTime);
            } else if (overSleep < variableYieldTime - 200 * 1000) {
                // decrease by 2 microseconds
                variableYieldTime = Math.max(variableYieldTime - 2 * 1000, 0);
            }
        }
    }

    private static void updateFpsTexture(String text) {
        // Delete old texture if it exists
        if (textTextureId != 0) {
            GL46.glDeleteTextures(textTextureId);
        }

        // Create text image using Java AWT
        Font font = new Font("Arial", Font.BOLD, 24);
        BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setFont(font);
        FontMetrics fm = g2d.getFontMetrics();
        textWidth = fm.stringWidth(text);
        textHeight = fm.getHeight();
        g2d.dispose();

        // Create actual image with proper size
        img = new BufferedImage(textWidth, textHeight, BufferedImage.TYPE_INT_ARGB);
        g2d = img.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(font);
        g2d.setColor(Color.WHITE);
        g2d.drawString(text, 0, fm.getAscent());
        g2d.dispose();

        // Convert to ByteBuffer
        int[] pixels = new int[img.getWidth() * img.getHeight()];
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), pixels, 0, img.getWidth());

        ByteBuffer buffer = BufferUtils.createByteBuffer(img.getWidth() * img.getHeight() * 4);
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int pixel = pixels[y * img.getWidth() + x];
                buffer.put((byte) ((pixel >> 16) & 0xFF)); // Red
                buffer.put((byte) ((pixel >> 8) & 0xFF));  // Green
                buffer.put((byte) (pixel & 0xFF));         // Blue
                buffer.put((byte) ((pixel >> 24) & 0xFF)); // Alpha
            }
        }
        buffer.flip();

        // Create OpenGL texture
        textTextureId = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, textTextureId);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MAG_FILTER, GL46.GL_LINEAR);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_RGBA, img.getWidth(), img.getHeight(),
                0, GL46.GL_RGBA, GL46.GL_UNSIGNED_BYTE, buffer);
    }

    private static void renderFpsText() {
        if (textTextureId == 0) return;

        // Get window size for proper positioning
        int[] width = new int[1];
        int[] height = new int[1];
        GLFW.glfwGetWindowSize(window, width, height);

        // Setup orthographic projection
        GL46.glMatrixMode(GL46.GL_PROJECTION);
        GL46.glPushMatrix();
        GL46.glLoadIdentity();
        GL46.glOrtho(0, width[0], height[0], 0, -1, 1);

        GL46.glMatrixMode(GL46.GL_MODELVIEW);
        GL46.glPushMatrix();
        GL46.glLoadIdentity();

        // Enable blending for transparency
        GL46.glEnable(GL46.GL_BLEND);
        GL46.glBlendFunc(GL46.GL_SRC_ALPHA, GL46.GL_ONE_MINUS_SRC_ALPHA);
        GL46.glEnable(GL46.GL_TEXTURE_2D);
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, textTextureId);

        // Draw text quad at top-left corner
        int x = 10;
        int y = 10;
        GL46.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GL46.glBegin(GL46.GL_QUADS);
        GL46.glTexCoord2f(0, 0);
        GL46.glVertex2f(x, y);
        GL46.glTexCoord2f(1, 0);
        GL46.glVertex2f(x + textWidth, y);
        GL46.glTexCoord2f(1, 1);
        GL46.glVertex2f(x + textWidth, y + textHeight);
        GL46.glTexCoord2f(0, 1);
        GL46.glVertex2f(x, y + textHeight);
        GL46.glEnd();

        // Restore state
        GL46.glDisable(GL46.GL_BLEND);
        GL46.glDisable(GL46.GL_TEXTURE_2D);

        GL46.glPopMatrix();
        GL46.glMatrixMode(GL46.GL_PROJECTION);
        GL46.glPopMatrix();
        GL46.glMatrixMode(GL46.GL_MODELVIEW);
    }
}