package ing.boykiss.gearworks.launcher.ui;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.vulkan.VkBufferCreateInfo;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkImageMemoryBarrier;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkPhysicalDevice;
import org.lwjgl.vulkan.VkPhysicalDeviceMemoryProperties;
import org.lwjgl.vulkan.VkSubmitInfo;

import java.nio.LongBuffer;

import static ing.boykiss.gearworks.launcher.ui.LauncherWindow.check;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_QUEUE_FAMILY_IGNORED;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkBindBufferMemory;
import static org.lwjgl.vulkan.VK10.vkCmdPipelineBarrier;
import static org.lwjgl.vulkan.VK10.vkCreateBuffer;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkGetBufferMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkGetPhysicalDeviceMemoryProperties;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkQueueWaitIdle;

/**
 * Shared Vulkan utilities used across the launcher UI package.
 */
class VkUtils {

    private VkUtils() {
    }

    // -------------------------------------------------------------------------
    // Memory
    // -------------------------------------------------------------------------

    static int findMemoryType(VkPhysicalDevice physDev, int typeFilter, int properties) {
        try (MemoryStack stack = stackPush()) {
            VkPhysicalDeviceMemoryProperties mp = VkPhysicalDeviceMemoryProperties.malloc(stack);
            vkGetPhysicalDeviceMemoryProperties(physDev, mp);
            for (int i = 0; i < mp.memoryTypeCount(); i++) {
                if ((typeFilter & (1 << i)) != 0
                        && (mp.memoryTypes(i).propertyFlags() & properties) == properties)
                    return i;
            }
            throw new RuntimeException("No suitable memory type (filter=" + typeFilter
                    + ", props=" + properties + ")");
        }
    }

    /**
     * Allocates a VkBuffer with backing memory and binds them together.
     *
     * @param outMemory receives the allocated VkDeviceMemory handle
     * @return the created VkBuffer handle
     */
    static long createBuffer(LauncherWindow w, long size, int usage, int memProps, long[] outMemory) {
        try (MemoryStack stack = stackPush()) {
            VkBufferCreateInfo bci = VkBufferCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO)
                    .size(size)
                    .usage(usage)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE);

            LongBuffer pBuf = stack.mallocLong(1);
            check(vkCreateBuffer(w.device, bci, null, pBuf), "vkCreateBuffer");
            long buf = pBuf.get(0);

            VkMemoryRequirements mr = VkMemoryRequirements.malloc(stack);
            vkGetBufferMemoryRequirements(w.device, buf, mr);

            VkMemoryAllocateInfo ai = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(mr.size())
                    .memoryTypeIndex(findMemoryType(w.physicalDevice, mr.memoryTypeBits(), memProps));

            LongBuffer pMem = stack.mallocLong(1);
            check(vkAllocateMemory(w.device, ai, null, pMem), "vkAllocateMemory (buffer)");
            outMemory[0] = pMem.get(0);
            vkBindBufferMemory(w.device, buf, outMemory[0], 0);
            return buf;
        }
    }

    // -------------------------------------------------------------------------
    // One-time command buffers
    // -------------------------------------------------------------------------

    static VkCommandBuffer beginOneTimeCmd(LauncherWindow w, long commandPool) {
        try (MemoryStack stack = stackPush()) {
            VkCommandBufferAllocateInfo ai = VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(commandPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(1);

            var pBuf = stack.mallocPointer(1);
            check(vkAllocateCommandBuffers(w.device, ai, pBuf), "vkAllocateCommandBuffers (one-time)");
            VkCommandBuffer cmd = new VkCommandBuffer(pBuf.get(0), w.device);

            VkCommandBufferBeginInfo bi = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO)
                    .flags(VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT);
            check(vkBeginCommandBuffer(cmd, bi), "vkBeginCommandBuffer (one-time)");
            return cmd;
        }
    }

    static void endOneTimeCmd(LauncherWindow w, VkCommandBuffer cmd, long commandPool) {
        try (MemoryStack stack = stackPush()) {
            check(vkEndCommandBuffer(cmd), "vkEndCommandBuffer (one-time)");

            VkSubmitInfo si = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .pCommandBuffers(stack.pointers(cmd));
            check(vkQueueSubmit(w.graphicsQueue, si, VK_NULL_HANDLE), "vkQueueSubmit (one-time)");
            vkQueueWaitIdle(w.graphicsQueue);
            vkFreeCommandBuffers(w.device, commandPool, cmd);
        }
    }

    // -------------------------------------------------------------------------
    // Image layout transition
    // -------------------------------------------------------------------------

    static void transitionImageLayout(VkCommandBuffer cmd, long image,
                                      int oldLayout, int newLayout,
                                      int srcAccess, int dstAccess,
                                      int srcStage, int dstStage) {
        try (MemoryStack stack = stackPush()) {
            VkImageMemoryBarrier.Buffer barrier = VkImageMemoryBarrier.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER)
                    .oldLayout(oldLayout).newLayout(newLayout)
                    .srcQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .dstQueueFamilyIndex(VK_QUEUE_FAMILY_IGNORED)
                    .image(image)
                    .srcAccessMask(srcAccess)
                    .dstAccessMask(dstAccess);
            barrier.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1)
                    .baseArrayLayer(0).layerCount(1);
            vkCmdPipelineBarrier(cmd, srcStage, dstStage, 0, null, null, barrier);
        }
    }
}
