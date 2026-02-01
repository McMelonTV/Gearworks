package ing.boykiss.gearworks.client.render;

import lombok.Getter;
import org.lwjgl.glfw.Callbacks;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWFramebufferSizeCallbackI;
import org.lwjgl.glfw.GLFWKeyCallbackI;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.IntBuffer;

public class Window {
    protected static final Logger LOGGER = LoggerFactory.getLogger(Window.class);

    @Getter
    private final long windowHandle;

    private final GLFWFramebufferSizeCallbackI framebufferSizeCallback;

    @Getter
    private int width;
    @Getter
    private int height;

    private boolean vsync = true;
    private int targetFPS = 120; // TODO: implement

    public Window(Properties props, GLFWFramebufferSizeCallbackI framebufferSizeCallback, GLFWKeyCallbackI keyCallback) {
        if (!GLFW.glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, props.contextVersionMajor);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, props.contextVersionMinor);

        windowHandle = GLFW.glfwCreateWindow(props.width, props.height, props.title, props.monitor, props.share);
        if (windowHandle == MemoryUtil.NULL) {
            throw new RuntimeException("Failed to create GLFW window");
        }

        GLFW.glfwSetErrorCallback((int errorCode, long msgPtr) ->
                LOGGER.error("[GLFW ERROR] error code: [{}], message: [{}]", errorCode, MemoryUtil.memUTF8(msgPtr))
        );

        this.framebufferSizeCallback = framebufferSizeCallback;
        GLFW.glfwSetFramebufferSizeCallback(windowHandle, this::resize);

        GLFW.glfwSetKeyCallback(windowHandle, keyCallback);

        GLFW.glfwMakeContextCurrent(windowHandle);

        if (vsync) {
            GLFW.glfwSwapInterval(1);
        } else {
            GLFW.glfwSwapInterval(0);
        }

        GLFW.glfwShowWindow(windowHandle);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            GLFW.glfwGetWindowSize(windowHandle, pWidth, pHeight);

            GLFWVidMode vidMode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());

            if (vidMode == null) {
                throw new RuntimeException("Failed to get primary monitor VidMode");
            }

            // Wayland protocol does not support window positioning
            if (GLFW.glfwGetPlatform() != GLFW.GLFW_PLATFORM_WAYLAND) {
                GLFW.glfwSetWindowPos(
                        windowHandle,
                        (vidMode.width() - pWidth.get(0)) / 2,
                        (vidMode.height() - pHeight.get(0)) / 2
                );
            }

            GLFW.glfwGetFramebufferSize(windowHandle, pWidth, pHeight);

            width = pWidth.get(0);
            height = pHeight.get(0);
        }
    }

    private void resize(long window, int width, int height) {
        this.width = width;
        this.height = height;
        framebufferSizeCallback.invoke(window, width, height);
    }

    public void update() {
        GLFW.glfwSwapBuffers(windowHandle);
    }

    public void pollEvents() {
        GLFW.glfwPollEvents();
    }

    public boolean shouldClose() {
        return GLFW.glfwWindowShouldClose(windowHandle);
    }

    public void cleanup() {
        Callbacks.glfwFreeCallbacks(windowHandle);
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
        GLFWErrorCallback callback = GLFW.glfwSetErrorCallback(null);
        if (callback != null) {
            callback.free();
        }
    }

    public record Properties(
            int width,
            int height,
            CharSequence title,
            long monitor,
            long share,
            int contextVersionMajor,
            int contextVersionMinor
    ) {
    }
}
