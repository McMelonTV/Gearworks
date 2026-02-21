package ing.boykiss.gearworks.launcher.ui;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWVulkan;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkApplicationInfo;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkDeviceCreateInfo;
import org.lwjgl.vulkan.VkDeviceQueueCreateInfo;
import org.lwjgl.vulkan.VkExtent2D;
import org.lwjgl.vulkan.VkInstance;
import org.lwjgl.vulkan.VkInstanceCreateInfo;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceFeatures;
import org.lwjgl.vulkan.VkQueue;
import org.lwjgl.vulkan.VkQueueFamilyProperties;
import org.lwjgl.vulkan.VkSurfaceCapabilitiesKHR;
import org.lwjgl.vulkan.VkSurfaceFormatKHR;
import org.lwjgl.vulkan.VkSwapchainCreateInfoKHR;

import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSurface.VK_COLOR_SPACE_SRGB_NONLINEAR_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_FIFO_KHR;
import static org.lwjgl.vulkan.KHRSurface.VK_PRESENT_MODE_MAILBOX_KHR;
import static org.lwjgl.vulkan.KHRSurface.vkDestroySurfaceKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceCapabilitiesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceFormatsKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfacePresentModesKHR;
import static org.lwjgl.vulkan.KHRSurface.vkGetPhysicalDeviceSurfaceSupportKHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_KHR_SWAPCHAIN_EXTENSION_NAME;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkCreateSwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkDestroySwapchainKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkGetSwapchainImagesKHR;
import static org.lwjgl.vulkan.VK10.VK_API_VERSION_1_0;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_B8G8R8A8_SRGB;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MAKE_VERSION;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_GRAPHICS_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_CONCURRENT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_APPLICATION_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_TRUE;
import static org.lwjgl.vulkan.VK10.vkCreateDevice;
import static org.lwjgl.vulkan.VK10.vkCreateInstance;
import static org.lwjgl.vulkan.VK10.vkDestroyDevice;
import static org.lwjgl.vulkan.VK10.vkDestroyInstance;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEnumeratePhysicalDevices;
import static org.lwjgl.vulkan.VK10.vkGetDeviceQueue;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceQueueFamilyProperties;

/**
 * Vulkan-backed GLFW window for the launcher UI.
 * Creates the Vulkan instance, device, and swapchain, then delegates
 * all per-frame rendering to {@link LauncherRenderer}.
 */
public class LauncherWindow {
    static final int WIDTH = 960;
    static final int HEIGHT = 540;
    private static final String TITLE = "Gearworks Launcher";
    /**
     * Mouse cursor position in window pixels (updated by GLFW callback).
     */
    public volatile double mouseX, mouseY;
    // Package-private so LauncherRenderer can read them directly
    long windowHandle;
    long surface;
    VkInstance instance;
    VkPhysicalDevice physicalDevice;
    VkDevice device;
    long swapchain;
    long[] swapchainImages;
    int swapchainFormat;
    int swapchainWidth, swapchainHeight;
    int graphicsQueueFamily = -1;
    int presentQueueFamily = -1;
    VkQueue graphicsQueue;
    VkQueue presentQueue;
    private volatile boolean pendingClick;

    private LauncherRenderer renderer;

    static void check(int result, String op) {
        if (result != VK_SUCCESS)
            throw new RuntimeException(op + " failed: " + result);
    }

    public void init(LauncherUIState uiState) {
        initGLFW();
        initVulkan();
        renderer = new LauncherRenderer(this, uiState);
        renderer.init();
    }

    /**
     * Consume a pending left-click. Returns true once per physical click.
     */
    public boolean consumeClick() {
        if (pendingClick) {
            pendingClick = false;
            return true;
        }
        return false;
    }

    /**
     * Signal that the window should close at the end of the current frame.
     */
    public void requestClose() {
        GLFW.glfwSetWindowShouldClose(windowHandle, true);
    }

    public void loop(Runnable onFrame) {
        while (!GLFW.glfwWindowShouldClose(windowHandle)) {
            GLFW.glfwPollEvents();
            onFrame.run();
            renderer.drawFrame();
        }
        // Drain the device before tearing down
        vkDeviceWaitIdle(device);
    }

    // -------------------------------------------------------------------------
    // GLFW
    // -------------------------------------------------------------------------

    public void cleanup() {
        renderer.cleanup();
        vkDestroySwapchainKHR(device, swapchain, null);
        vkDestroyDevice(device, null);
        vkDestroySurfaceKHR(instance, surface, null);
        vkDestroyInstance(instance, null);
        GLFW.glfwDestroyWindow(windowHandle);
        GLFW.glfwTerminate();
    }

    // -------------------------------------------------------------------------
    // Vulkan
    // -------------------------------------------------------------------------

    private void initGLFW() {
        if (!GLFW.glfwInit()) throw new RuntimeException("Failed to initialise GLFW");
        GLFW.glfwWindowHint(GLFW.GLFW_CLIENT_API, GLFW.GLFW_NO_API);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        windowHandle = GLFW.glfwCreateWindow(WIDTH, HEIGHT, TITLE, 0, 0);
        if (windowHandle == 0) throw new RuntimeException("Failed to create GLFW window");
        // Allow ESC to close the launcher window
        GLFW.glfwSetKeyCallback(windowHandle, (win, key, scancode, action, mods) -> {
            if (key == GLFW.GLFW_KEY_ESCAPE && action == GLFW.GLFW_PRESS)
                GLFW.glfwSetWindowShouldClose(win, true);
        });
        // Track mouse position
        GLFW.glfwSetCursorPosCallback(windowHandle, (win, x, y) -> {
            mouseX = x;
            mouseY = y;
        });
        // Track left-button clicks
        GLFW.glfwSetMouseButtonCallback(windowHandle, (win, btn, action, mods) -> {
            if (btn == GLFW.GLFW_MOUSE_BUTTON_LEFT && action == GLFW.GLFW_PRESS)
                pendingClick = true;
        });
    }

    private void initVulkan() {
        createInstance();
        createSurface();
        pickPhysicalDevice();
        createLogicalDevice();
        createSwapchain();
    }

    private void createInstance() {
        try (MemoryStack stack = stackPush()) {
            VkApplicationInfo appInfo = VkApplicationInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_APPLICATION_INFO)
                    .pApplicationName(stack.UTF8Safe(TITLE))
                    .applicationVersion(VK_MAKE_VERSION(0, 0, 1))
                    .pEngineName(stack.UTF8Safe("No Engine"))
                    .engineVersion(VK_MAKE_VERSION(1, 0, 0))
                    .apiVersion(VK_API_VERSION_1_0);

            var glfwExt = GLFWVulkan.glfwGetRequiredInstanceExtensions();
            if (glfwExt == null) throw new RuntimeException("No Vulkan surface extensions available");

            VkInstanceCreateInfo ci = VkInstanceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO)
                    .pApplicationInfo(appInfo)
                    .ppEnabledExtensionNames(glfwExt);

            var pInst = stack.mallocPointer(1);
            check(vkCreateInstance(ci, null, pInst), "vkCreateInstance");
            instance = new VkInstance(pInst.get(0), ci);
        }
    }

    private void createSurface() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pSurf = stack.mallocLong(1);
            check(GLFWVulkan.glfwCreateWindowSurface(instance, windowHandle, null, pSurf),
                    "glfwCreateWindowSurface");
            surface = pSurf.get(0);
        }
    }

    private void pickPhysicalDevice() {
        try (MemoryStack stack = stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            vkEnumeratePhysicalDevices(instance, count, null);
            if (count.get(0) == 0) throw new RuntimeException("No Vulkan-capable GPU found");

            var pDevs = stack.mallocPointer(count.get(0));
            vkEnumeratePhysicalDevices(instance, count, pDevs);

            for (int i = 0; i < count.get(0); i++) {
                VkPhysicalDevice candidate = new VkPhysicalDevice(pDevs.get(i), instance);
                if (findQueueFamilies(candidate)) {
                    physicalDevice = candidate;
                    return;
                }
            }
            throw new RuntimeException("No suitable GPU found");
        }
    }

    /**
     * Fills {@link #graphicsQueueFamily} and {@link #presentQueueFamily}. Returns true when both found.
     */
    private boolean findQueueFamilies(VkPhysicalDevice dev) {
        try (MemoryStack stack = stackPush()) {
            IntBuffer count = stack.mallocInt(1);
            vkGetPhysicalDeviceQueueFamilyProperties(dev, count, null);
            var props = VkQueueFamilyProperties.malloc(count.get(0), stack);
            vkGetPhysicalDeviceQueueFamilyProperties(dev, count, props);

            IntBuffer presentSupport = stack.mallocInt(1);
            for (int i = 0; i < count.get(0); i++) {
                if ((props.get(i).queueFlags() & VK_QUEUE_GRAPHICS_BIT) != 0)
                    graphicsQueueFamily = i;
                vkGetPhysicalDeviceSurfaceSupportKHR(dev, i, surface, presentSupport);
                if (presentSupport.get(0) == VK_TRUE)
                    presentQueueFamily = i;
                if (graphicsQueueFamily != -1 && presentQueueFamily != -1) return true;
            }
            return false;
        }
    }

    private void createLogicalDevice() {
        try (MemoryStack stack = stackPush()) {
            int[] families = (graphicsQueueFamily == presentQueueFamily)
                    ? new int[]{graphicsQueueFamily}
                    : new int[]{graphicsQueueFamily, presentQueueFamily};

            var queueInfos = VkDeviceQueueCreateInfo.calloc(families.length, stack);
            for (int i = 0; i < families.length; i++)
                queueInfos.get(i)
                        .sType(VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO)
                        .queueFamilyIndex(families[i])
                        .pQueuePriorities(stack.floats(1.0f));

            VkDeviceCreateInfo ci = VkDeviceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO)
                    .pQueueCreateInfos(queueInfos)
                    .ppEnabledExtensionNames(stack.pointers(stack.UTF8(VK_KHR_SWAPCHAIN_EXTENSION_NAME)))
                    .pEnabledFeatures(VkPhysicalDeviceFeatures.calloc(stack));

            var pDev = stack.mallocPointer(1);
            check(vkCreateDevice(physicalDevice, ci, null, pDev), "vkCreateDevice");
            device = new VkDevice(pDev.get(0), physicalDevice, ci);

            var pQ = stack.mallocPointer(1);
            vkGetDeviceQueue(device, graphicsQueueFamily, 0, pQ);
            graphicsQueue = new VkQueue(pQ.get(0), device);
            vkGetDeviceQueue(device, presentQueueFamily, 0, pQ);
            presentQueue = new VkQueue(pQ.get(0), device);
        }
    }

    // -------------------------------------------------------------------------
    // Utility
    // -------------------------------------------------------------------------

    private void createSwapchain() {
        try (MemoryStack stack = stackPush()) {
            // Surface capabilities
            VkSurfaceCapabilitiesKHR caps = VkSurfaceCapabilitiesKHR.malloc(stack);
            vkGetPhysicalDeviceSurfaceCapabilitiesKHR(physicalDevice, surface, caps);

            // Choose surface format — prefer B8G8R8A8_SRGB + SRGB_NONLINEAR
            IntBuffer fmtCount = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, fmtCount, null);
            var formats = VkSurfaceFormatKHR.malloc(fmtCount.get(0), stack);
            vkGetPhysicalDeviceSurfaceFormatsKHR(physicalDevice, surface, fmtCount, formats);

            VkSurfaceFormatKHR chosenFormat = formats.get(0);
            for (int i = 0; i < fmtCount.get(0); i++) {
                VkSurfaceFormatKHR f = formats.get(i);
                if (f.format() == VK_FORMAT_B8G8R8A8_SRGB
                        && f.colorSpace() == VK_COLOR_SPACE_SRGB_NONLINEAR_KHR) {
                    chosenFormat = f;
                    break;
                }
            }
            swapchainFormat = chosenFormat.format();

            // Choose present mode — prefer MAILBOX for low-latency, fall back to FIFO
            IntBuffer pmCount = stack.mallocInt(1);
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pmCount, null);
            IntBuffer presentModes = stack.mallocInt(pmCount.get(0));
            vkGetPhysicalDeviceSurfacePresentModesKHR(physicalDevice, surface, pmCount, presentModes);

            int presentMode = VK_PRESENT_MODE_FIFO_KHR;
            for (int i = 0; i < pmCount.get(0); i++)
                if (presentModes.get(i) == VK_PRESENT_MODE_MAILBOX_KHR) {
                    presentMode = VK_PRESENT_MODE_MAILBOX_KHR;
                    break;
                }

            // Choose swap extent
            VkExtent2D extent;
            if (caps.currentExtent().width() != 0xFFFFFFFF) {
                extent = caps.currentExtent();
            } else {
                extent = VkExtent2D.malloc(stack).set(WIDTH, HEIGHT);
            }
            swapchainWidth = extent.width();
            swapchainHeight = extent.height();

            // Image count: at least minImageCount + 1, capped at maxImageCount
            int imageCount = caps.minImageCount() + 1;
            if (caps.maxImageCount() > 0)
                imageCount = Math.min(imageCount, caps.maxImageCount());

            VkSwapchainCreateInfoKHR ci = VkSwapchainCreateInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR)
                    .surface(surface)
                    .minImageCount(imageCount)
                    .imageFormat(chosenFormat.format())
                    .imageColorSpace(chosenFormat.colorSpace())
                    .imageExtent(extent)
                    .imageArrayLayers(1)
                    .imageUsage(VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT);

            if (graphicsQueueFamily != presentQueueFamily) {
                ci.imageSharingMode(VK_SHARING_MODE_CONCURRENT)
                        .pQueueFamilyIndices(stack.ints(graphicsQueueFamily, presentQueueFamily));
            } else {
                ci.imageSharingMode(VK_SHARING_MODE_EXCLUSIVE);
            }

            ci.preTransform(caps.currentTransform())
                    .compositeAlpha(VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR)
                    .presentMode(presentMode)
                    .clipped(true)
                    .oldSwapchain(VK_NULL_HANDLE);

            LongBuffer pSwap = stack.mallocLong(1);
            check(vkCreateSwapchainKHR(device, ci, null, pSwap), "vkCreateSwapchainKHR");
            swapchain = pSwap.get(0);

            // Retrieve swapchain images
            IntBuffer imgCount = stack.mallocInt(1);
            vkGetSwapchainImagesKHR(device, swapchain, imgCount, null);
            LongBuffer imgs = stack.mallocLong(imgCount.get(0));
            vkGetSwapchainImagesKHR(device, swapchain, imgCount, imgs);
            swapchainImages = new long[imgCount.get(0)];
            for (int i = 0; i < imgCount.get(0); i++) swapchainImages[i] = imgs.get(i);
        }
    }
}
