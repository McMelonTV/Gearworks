package ing.boykiss.gearworks.launcher.ui;

import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.Shaderc;
import org.lwjgl.vulkan.VkAttachmentDescription;
import org.lwjgl.vulkan.VkAttachmentReference;
import org.lwjgl.vulkan.VkClearValue;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkCommandBufferAllocateInfo;
import org.lwjgl.vulkan.VkCommandBufferBeginInfo;
import org.lwjgl.vulkan.VkCommandPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorImageInfo;
import org.lwjgl.vulkan.VkDescriptorPoolCreateInfo;
import org.lwjgl.vulkan.VkDescriptorPoolSize;
import org.lwjgl.vulkan.VkDescriptorSetAllocateInfo;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.lwjgl.vulkan.VkDescriptorSetLayoutCreateInfo;
import org.lwjgl.vulkan.VkFenceCreateInfo;
import org.lwjgl.vulkan.VkFramebufferCreateInfo;
import org.lwjgl.vulkan.VkGraphicsPipelineCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkPipelineColorBlendAttachmentState;
import org.lwjgl.vulkan.VkPipelineColorBlendStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineInputAssemblyStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineLayoutCreateInfo;
import org.lwjgl.vulkan.VkPipelineMultisampleStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineRasterizationStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineShaderStageCreateInfo;
import org.lwjgl.vulkan.VkPipelineVertexInputStateCreateInfo;
import org.lwjgl.vulkan.VkPipelineViewportStateCreateInfo;
import org.lwjgl.vulkan.VkPresentInfoKHR;
import org.lwjgl.vulkan.VkRect2D;
import org.lwjgl.vulkan.VkRenderPassBeginInfo;
import org.lwjgl.vulkan.VkRenderPassCreateInfo;
import org.lwjgl.vulkan.VkSemaphoreCreateInfo;
import org.lwjgl.vulkan.VkShaderModuleCreateInfo;
import org.lwjgl.vulkan.VkSubmitInfo;
import org.lwjgl.vulkan.VkSubpassDependency;
import org.lwjgl.vulkan.VkSubpassDescription;
import org.lwjgl.vulkan.VkVertexInputAttributeDescription;
import org.lwjgl.vulkan.VkVertexInputBindingDescription;
import org.lwjgl.vulkan.VkViewport;
import org.lwjgl.vulkan.VkWriteDescriptorSet;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;

import static ing.boykiss.gearworks.launcher.ui.LauncherWindow.check;
import static ing.boykiss.gearworks.launcher.ui.VkUtils.createBuffer;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.KHRSwapchain.VK_ERROR_OUT_OF_DATE_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_IMAGE_LAYOUT_PRESENT_SRC_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_STRUCTURE_TYPE_PRESENT_INFO_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.VK_SUBOPTIMAL_KHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkAcquireNextImageKHR;
import static org.lwjgl.vulkan.KHRSwapchain.vkQueuePresentKHR;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_CLEAR;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_LOAD_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_DONT_CARE;
import static org.lwjgl.vulkan.VK10.VK_ATTACHMENT_STORE_OP_STORE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_SRC_ALPHA;
import static org.lwjgl.vulkan.VK10.VK_BLEND_FACTOR_ZERO;
import static org.lwjgl.vulkan.VK10.VK_BLEND_OP_ADD;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_VERTEX_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_A_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_B_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_G_BIT;
import static org.lwjgl.vulkan.VK10.VK_COLOR_COMPONENT_R_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_BUFFER_LEVEL_PRIMARY;
import static org.lwjgl.vulkan.VK10.VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_IDENTITY;
import static org.lwjgl.vulkan.VK10.VK_CULL_MODE_NONE;
import static org.lwjgl.vulkan.VK10.VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
import static org.lwjgl.vulkan.VK10.VK_FENCE_CREATE_SIGNALED_BIT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32B32A32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32G32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R32_SFLOAT;
import static org.lwjgl.vulkan.VK10.VK_FRONT_FACE_COUNTER_CLOCKWISE;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_NULL_HANDLE;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_BIND_POINT_GRAPHICS;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT;
import static org.lwjgl.vulkan.VK10.VK_POLYGON_MODE_FILL;
import static org.lwjgl.vulkan.VK10.VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_FRAGMENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHADER_STAGE_VERTEX_BIT;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FENCE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SUBMIT_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_CONTENTS_INLINE;
import static org.lwjgl.vulkan.VK10.VK_SUBPASS_EXTERNAL;
import static org.lwjgl.vulkan.VK10.VK_SUCCESS;
import static org.lwjgl.vulkan.VK10.VK_VERTEX_INPUT_RATE_VERTEX;
import static org.lwjgl.vulkan.VK10.vkAllocateCommandBuffers;
import static org.lwjgl.vulkan.VK10.vkAllocateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkBeginCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkCmdBeginRenderPass;
import static org.lwjgl.vulkan.VK10.vkCmdBindDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkCmdBindPipeline;
import static org.lwjgl.vulkan.VK10.vkCmdBindVertexBuffers;
import static org.lwjgl.vulkan.VK10.vkCmdDraw;
import static org.lwjgl.vulkan.VK10.vkCmdEndRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateCommandPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkCreateDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkCreateFence;
import static org.lwjgl.vulkan.VK10.vkCreateFramebuffer;
import static org.lwjgl.vulkan.VK10.vkCreateGraphicsPipelines;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkCreatePipelineLayout;
import static org.lwjgl.vulkan.VK10.vkCreateRenderPass;
import static org.lwjgl.vulkan.VK10.vkCreateSemaphore;
import static org.lwjgl.vulkan.VK10.vkCreateShaderModule;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyCommandPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorPool;
import static org.lwjgl.vulkan.VK10.vkDestroyDescriptorSetLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyFence;
import static org.lwjgl.vulkan.VK10.vkDestroyFramebuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroyPipeline;
import static org.lwjgl.vulkan.VK10.vkDestroyPipelineLayout;
import static org.lwjgl.vulkan.VK10.vkDestroyRenderPass;
import static org.lwjgl.vulkan.VK10.vkDestroySemaphore;
import static org.lwjgl.vulkan.VK10.vkDestroyShaderModule;
import static org.lwjgl.vulkan.VK10.vkDeviceWaitIdle;
import static org.lwjgl.vulkan.VK10.vkEndCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkQueueSubmit;
import static org.lwjgl.vulkan.VK10.vkResetCommandBuffer;
import static org.lwjgl.vulkan.VK10.vkResetFences;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;
import static org.lwjgl.vulkan.VK10.vkUpdateDescriptorSets;
import static org.lwjgl.vulkan.VK10.vkWaitForFences;

/**
 * Vulkan 2D UI renderer for the launcher window.
 * <p>
 * Renders two kinds of primitives:
 * - Solid coloured quads  (buttons, separators) — mode = 0.0
 * - Font glyphs           (text via STB TrueType atlas) — mode = 1.0
 * <p>
 * Vertex format (9 floats, stride = 36 bytes):
 * vec2 pos  | vec2 uv  | vec4 color  | float mode
 * <p>
 * Two font atlases (title 60 px, body 26 px) are baked at startup.
 * The pipeline expects a single COMBINED_IMAGE_SAMPLER descriptor at binding 0;
 * descriptor sets are swapped between the two atlases mid-frame.
 * <p>
 * Geometry is built CPU-side each frame into {@code verts[]} then
 * memcpy'd into a persistently-mapped, per-in-flight-frame vertex buffer.
 * Command buffers are re-recorded every frame so draw counts can change.
 */
class LauncherRenderer {

    // -------------------------------------------------------------------------
    // UI layout constants  (pixel space, origin = top-left)
    // -------------------------------------------------------------------------
    private static final float W = LauncherWindow.WIDTH;
    private static final float H = LauncherWindow.HEIGHT;

    private static final float BTN_X0 = (W - 300) / 2f;   // 330
    private static final float BTN_X1 = BTN_X0 + 300;     // 630
    private static final float BTN_Y0 = 370;
    private static final float BTN_Y1 = 430;

    // Appearance colours
    private static final float[] CLR_TITLE = {1.00f, 1.00f, 1.00f, 1.00f};
    private static final float[] CLR_SUBTITLE = {0.60f, 0.70f, 0.85f, 1.00f};
    private static final float[] CLR_BTN_NORM = {0.18f, 0.50f, 0.88f, 1.00f};
    private static final float[] CLR_BTN_HOV = {0.28f, 0.63f, 1.00f, 1.00f};
    private static final float[] CLR_BTN_TXT = {1.00f, 1.00f, 1.00f, 1.00f};
    private static final float[] CLR_HINT = {0.37f, 0.41f, 0.52f, 1.00f};

    // -------------------------------------------------------------------------
    // Vertex buffer geometry
    // -------------------------------------------------------------------------
    private static final int FLOATS_PER_VERT = 9;  // pos(2)+uv(2)+color(4)+mode(1)
    private static final int MAX_QUADS = 2048;
    private static final int MAX_VERTS = MAX_QUADS * 6;
    private static final long VB_SIZE = (long) MAX_VERTS * FLOATS_PER_VERT * Float.BYTES;
    // -------------------------------------------------------------------------
    // Shaders (GLSL source compiled to SPIR-V via shaderc at startup)
    // -------------------------------------------------------------------------
    private static final String VERT_SRC = """
            #version 450
            layout(location = 0) in vec2  inPos;
            layout(location = 1) in vec2  inUV;
            layout(location = 2) in vec4  inColor;
            layout(location = 3) in float inMode;
            layout(location = 0) out vec2  fragUV;
            layout(location = 1) out vec4  fragColor;
            layout(location = 2) out float fragMode;
            void main() {
                gl_Position = vec4(inPos, 0.0, 1.0);
                fragUV      = inUV;
                fragColor   = inColor;
                fragMode    = inMode;
            }
            """;
    private static final String FRAG_SRC = """
            #version 450
            layout(location = 0) in vec2  fragUV;
            layout(location = 1) in vec4  fragColor;
            layout(location = 2) in float fragMode;
            layout(binding = 0) uniform sampler2D fontAtlas;
            layout(location = 0) out vec4 outColor;
            void main() {
                if (fragMode > 0.5) {
                    // Font glyph — atlas encodes coverage in all channels (R=G=B=A after swizzle)
                    float a = texture(fontAtlas, fragUV).r;
                    outColor = vec4(fragColor.rgb, fragColor.a * a);
                } else {
                    outColor = fragColor;
                }
            }
            """;
    // -------------------------------------------------------------------------
    // Vulkan handles
    // -------------------------------------------------------------------------
    private static final int MAX_FRAMES = 2;
    private final float[] verts = new float[MAX_VERTS * FLOATS_PER_VERT];
    private final LauncherWindow w;
    private final LauncherUIState state;
    private int vertCount;
    /**
     * Vertex count for (solid rects + title text) drawn with the title-font descriptor set.
     */
    private int titleSectionEnd;
    // Swapchain image resources
    private long[] imageViews;
    private long[] framebuffers;

    // Per-frame-in-flight
    private VkCommandBuffer[] cmdBufs;     // re-recorded each frame
    private long[] vertexBuffers;
    private long[] vertexMemories;
    private long[] mappedPtrs;             // native pointers to persistently-mapped VBs
    private long[] imageAvailSems, renderDoneSems, inFlightFences;

    // Shared
    private long renderPass;
    private long cmdPool;
    private long descSetLayout;
    private long descPool;
    private long[] descSets;             // [0] = titleFont DS, [1] = bodyFont DS
    private long pipelineLayout;
    private long pipeline;

    // Fonts
    private FontAtlas titleFont; // 60 px — "Gearworks" heading
    private FontAtlas bodyFont;  // 26 px — subtitles, button text, hints

    private int currentFrame = 0;

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    LauncherRenderer(LauncherWindow window, LauncherUIState uiState) {
        this.w = window;
        this.state = uiState;
    }

    /**
     * Pixel → Vulkan NDC (y points down to match Vulkan clip space).
     */
    private static float px(float x) {
        return (x / W) * 2f - 1f;
    }

    private static float py(float y) {
        return (y / H) * 2f - 1f;
    }

    void init() {
        createImageViews();
        createRenderPass();
        createCommandPool();
        loadFonts();
        createDescriptorSetLayout();
        createDescriptorPool();
        allocateAndWriteDescriptorSets();
        createPipeline();
        createFramebuffers();
        createVertexBuffers();
        createCommandBuffers();
        createSyncObjects();
    }

    // -------------------------------------------------------------------------
    // Geometry building (CPU-side)
    // -------------------------------------------------------------------------

    void drawFrame() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer fenceHandle = stack.longs(inFlightFences[currentFrame]);
            vkWaitForFences(w.device, fenceHandle, true, Long.MAX_VALUE);
            vkResetFences(w.device, fenceHandle);

            // Acquire next swapchain image
            IntBuffer pImg = stack.mallocInt(1);
            int r = vkAcquireNextImageKHR(w.device, w.swapchain, Long.MAX_VALUE,
                    imageAvailSems[currentFrame], VK_NULL_HANDLE, pImg);
            if (r == VK_ERROR_OUT_OF_DATE_KHR) return;
            if (r != VK_SUCCESS && r != VK_SUBOPTIMAL_KHR)
                throw new RuntimeException("vkAcquireNextImageKHR: " + r);
            int imageIndex = pImg.get(0);

            // Build geometry and upload to this frame's vertex buffer
            buildGeometry();
            uploadVertices(currentFrame);

            // Re-record command buffer with correct framebuffer + draw counts
            recordCommandBuffer(cmdBufs[currentFrame], framebuffers[imageIndex]);

            // Submit
            VkSubmitInfo si = VkSubmitInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SUBMIT_INFO)
                    .waitSemaphoreCount(1)
                    .pWaitSemaphores(stack.longs(imageAvailSems[currentFrame]))
                    .pWaitDstStageMask(stack.ints(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT))
                    .pCommandBuffers(stack.pointers(cmdBufs[currentFrame]))
                    .pSignalSemaphores(stack.longs(renderDoneSems[currentFrame]));
            check(vkQueueSubmit(w.graphicsQueue, si, inFlightFences[currentFrame]), "vkQueueSubmit");

            // Present
            VkPresentInfoKHR pi = VkPresentInfoKHR.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PRESENT_INFO_KHR)
                    .pWaitSemaphores(stack.longs(renderDoneSems[currentFrame]))
                    .swapchainCount(1)
                    .pSwapchains(stack.longs(w.swapchain))
                    .pImageIndices(pImg);
            vkQueuePresentKHR(w.presentQueue, pi);

            currentFrame = (currentFrame + 1) % MAX_FRAMES;
        }
    }

    // ----------- Vertex helpers -------------------------------------------

    void cleanup() {
        vkDeviceWaitIdle(w.device);

        for (int i = 0; i < MAX_FRAMES; i++) {
            vkUnmapMemory(w.device, vertexMemories[i]);
            vkDestroyBuffer(w.device, vertexBuffers[i], null);
            vkFreeMemory(w.device, vertexMemories[i], null);
            vkDestroySemaphore(w.device, renderDoneSems[i], null);
            vkDestroySemaphore(w.device, imageAvailSems[i], null);
            vkDestroyFence(w.device, inFlightFences[i], null);
        }
        vkDestroyPipeline(w.device, pipeline, null);
        vkDestroyPipelineLayout(w.device, pipelineLayout, null);
        vkDestroyDescriptorPool(w.device, descPool, null);
        vkDestroyDescriptorSetLayout(w.device, descSetLayout, null);
        titleFont.cleanup(w.device);
        bodyFont.cleanup(w.device);
        vkDestroyCommandPool(w.device, cmdPool, null);
        for (long fb : framebuffers) vkDestroyFramebuffer(w.device, fb, null);
        vkDestroyRenderPass(w.device, renderPass, null);
        for (long iv : imageViews) vkDestroyImageView(w.device, iv, null);
    }

    private void buildGeometry() {
        vertCount = 0;

        // ---- Solid section (button background) + title text ----
        float[] btnColor = state.isPlayButtonHovered() ? CLR_BTN_HOV : CLR_BTN_NORM;
        addRect(BTN_X0, BTN_Y0, BTN_X1, BTN_Y1, btnColor);

        // Title text ("Gearworks") — drawn with the title-font descriptor set
        String title = "Gearworks";
        float titleX = (W - titleFont.measureText(title)) / 2f;
        addText(titleFont, title, titleX, 200f, CLR_TITLE);

        titleSectionEnd = vertCount; // end of (rect + title-font text) section

        // ---- Body section (drawn with body-font descriptor set) ----
        String subtitle = "LAUNCHER";
        float subX = (W - bodyFont.measureText(subtitle)) / 2f;
        addText(bodyFont, subtitle, subX, 256f, CLR_SUBTITLE);

        // "PLAY" centered in the button (baseline ≈ 60% down button height)
        String btnText = "PLAY";
        float btnTW = bodyFont.measureText(btnText);
        float btnTX = (BTN_X0 + BTN_X1 - btnTW) / 2f;
        float btnTY = BTN_Y0 + (BTN_Y1 - BTN_Y0) * 0.65f;
        addText(bodyFont, btnText, btnTX, btnTY, CLR_BTN_TXT);

        // Hint text below button
        String hint = "Press ESC to close";
        float hintX = (W - bodyFont.measureText(hint)) / 2f;
        addText(bodyFont, hint, hintX, 500f, CLR_HINT);
    }

    private void addVert(float nx, float ny, float u, float v,
                         float r, float g, float b, float a, float mode) {
        int base = vertCount * FLOATS_PER_VERT;
        verts[base] = nx;
        verts[base + 1] = ny;
        verts[base + 2] = u;
        verts[base + 3] = v;
        verts[base + 4] = r;
        verts[base + 5] = g;
        verts[base + 6] = b;
        verts[base + 7] = a;
        verts[base + 8] = mode;
        vertCount++;
    }

    private void addRect(float x0, float y0, float x1, float y1, float[] c) {
        float nx0 = px(x0), nx1 = px(x1), ny0 = py(y0), ny1 = py(y1);
        // Two triangles (CCW, culling disabled so winding doesn't matter)
        addVert(nx0, ny0, 0, 0, c[0], c[1], c[2], c[3], 0); // TL
        addVert(nx1, ny0, 0, 0, c[0], c[1], c[2], c[3], 0); // TR
        addVert(nx0, ny1, 0, 0, c[0], c[1], c[2], c[3], 0); // BL
        addVert(nx1, ny0, 0, 0, c[0], c[1], c[2], c[3], 0); // TR
        addVert(nx1, ny1, 0, 0, c[0], c[1], c[2], c[3], 0); // BR
        addVert(nx0, ny1, 0, 0, c[0], c[1], c[2], c[3], 0); // BL
    }

    private void addText(FontAtlas font, String text, float pixelX, float pixelY, float[] c) {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer xB = stack.floats(pixelX);
            FloatBuffer yB = stack.floats(pixelY);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
            for (int i = 0; i < text.length(); i++) {
                int idx = text.charAt(i) - FontAtlas.FIRST_CHAR;
                if (idx < 0 || idx >= FontAtlas.NUM_CHARS) continue;
                font.getPackedQuad(idx, xB, yB, q);
                float nx0 = px(q.x0()), nx1 = px(q.x1());
                float ny0 = py(q.y0()), ny1 = py(q.y1());
                addVert(nx0, ny0, q.s0(), q.t0(), c[0], c[1], c[2], c[3], 1); // TL
                addVert(nx1, ny0, q.s1(), q.t0(), c[0], c[1], c[2], c[3], 1); // TR
                addVert(nx0, ny1, q.s0(), q.t1(), c[0], c[1], c[2], c[3], 1); // BL
                addVert(nx1, ny0, q.s1(), q.t0(), c[0], c[1], c[2], c[3], 1); // TR
                addVert(nx1, ny1, q.s1(), q.t1(), c[0], c[1], c[2], c[3], 1); // BR
                addVert(nx0, ny1, q.s0(), q.t1(), c[0], c[1], c[2], c[3], 1); // BL
            }
        }
    }

    private void uploadVertices(int frameIndex) {
        int bytes = vertCount * FLOATS_PER_VERT * Float.BYTES;
        if (bytes == 0) return;
        ByteBuffer dst = MemoryUtil.memByteBuffer(mappedPtrs[frameIndex], bytes);
        FloatBuffer fDst = dst.asFloatBuffer();
        fDst.put(verts, 0, vertCount * FLOATS_PER_VERT);
    }

    // -------------------------------------------------------------------------
    // Command buffer recording (called every frame)
    // -------------------------------------------------------------------------

    private void recordCommandBuffer(VkCommandBuffer cmd, long framebuffer) {
        try (MemoryStack stack = stackPush()) {
            vkResetCommandBuffer(cmd, 0);

            VkCommandBufferBeginInfo bi = VkCommandBufferBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO);
            check(vkBeginCommandBuffer(cmd, bi), "vkBeginCommandBuffer");

            // Clear colour = dark slate background
            VkClearValue.Buffer cvs = VkClearValue.calloc(1, stack);
            cvs.get(0).color().float32(0, 0.08f).float32(1, 0.10f).float32(2, 0.16f).float32(3, 1f);

            VkRenderPassBeginInfo rpi = VkRenderPassBeginInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_BEGIN_INFO)
                    .renderPass(renderPass)
                    .framebuffer(framebuffer)
                    .pClearValues(cvs);
            rpi.renderArea().offset().set(0, 0);
            rpi.renderArea().extent().set(w.swapchainWidth, w.swapchainHeight);

            vkCmdBeginRenderPass(cmd, rpi, VK_SUBPASS_CONTENTS_INLINE);

            if (vertCount > 0) {
                vkCmdBindPipeline(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline);
                LongBuffer vbHandle = stack.longs(vertexBuffers[currentFrame]);
                LongBuffer offsets = stack.longs(0L);
                vkCmdBindVertexBuffers(cmd, 0, vbHandle, offsets);

                // Draw 1: solid rects + title text (bind title-font descriptor set)
                if (titleSectionEnd > 0) {
                    vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 0, stack.longs(descSets[0]), null);
                    vkCmdDraw(cmd, titleSectionEnd, 1, 0, 0);
                }

                // Draw 2: body text (labels, hints — bind body-font descriptor set)
                int bodyCount = vertCount - titleSectionEnd;
                if (bodyCount > 0) {
                    vkCmdBindDescriptorSets(cmd, VK_PIPELINE_BIND_POINT_GRAPHICS,
                            pipelineLayout, 0, stack.longs(descSets[1]), null);
                    vkCmdDraw(cmd, bodyCount, 1, titleSectionEnd, 0);
                }
            }

            vkCmdEndRenderPass(cmd);
            check(vkEndCommandBuffer(cmd), "vkEndCommandBuffer");
        }
    }

    // -------------------------------------------------------------------------
    // One-time initialisation helpers
    // -------------------------------------------------------------------------

    private void createImageViews() {
        imageViews = new long[w.swapchainImages.length];
        try (MemoryStack stack = stackPush()) {
            LongBuffer pView = stack.mallocLong(1);
            for (int i = 0; i < w.swapchainImages.length; i++) {
                VkImageViewCreateInfo ci = VkImageViewCreateInfo.calloc(stack)
                        .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                        .image(w.swapchainImages[i])
                        .viewType(VK_IMAGE_VIEW_TYPE_2D)
                        .format(w.swapchainFormat);
                ci.components().r(VK_COMPONENT_SWIZZLE_IDENTITY).g(VK_COMPONENT_SWIZZLE_IDENTITY)
                        .b(VK_COMPONENT_SWIZZLE_IDENTITY).a(VK_COMPONENT_SWIZZLE_IDENTITY);
                ci.subresourceRange().aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                        .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
                check(vkCreateImageView(w.device, ci, null, pView), "vkCreateImageView");
                imageViews[i] = pView.get(0);
            }
        }
    }

    private void createRenderPass() {
        try (MemoryStack stack = stackPush()) {
            VkAttachmentDescription.Buffer att = VkAttachmentDescription.calloc(1, stack)
                    .format(w.swapchainFormat).samples(VK_SAMPLE_COUNT_1_BIT)
                    .loadOp(VK_ATTACHMENT_LOAD_OP_CLEAR).storeOp(VK_ATTACHMENT_STORE_OP_STORE)
                    .stencilLoadOp(VK_ATTACHMENT_LOAD_OP_DONT_CARE)
                    .stencilStoreOp(VK_ATTACHMENT_STORE_OP_DONT_CARE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED)
                    .finalLayout(VK_IMAGE_LAYOUT_PRESENT_SRC_KHR);

            VkAttachmentReference.Buffer ref = VkAttachmentReference.calloc(1, stack)
                    .attachment(0).layout(VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);

            VkSubpassDescription.Buffer sub = VkSubpassDescription.calloc(1, stack)
                    .pipelineBindPoint(VK_PIPELINE_BIND_POINT_GRAPHICS)
                    .colorAttachmentCount(1).pColorAttachments(ref);

            VkSubpassDependency.Buffer dep = VkSubpassDependency.calloc(1, stack)
                    .srcSubpass(VK_SUBPASS_EXTERNAL).dstSubpass(0)
                    .srcStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT).srcAccessMask(0)
                    .dstStageMask(VK_PIPELINE_STAGE_COLOR_ATTACHMENT_OUTPUT_BIT)
                    .dstAccessMask(VK_ACCESS_COLOR_ATTACHMENT_WRITE_BIT);

            LongBuffer pPass = stack.mallocLong(1);
            check(vkCreateRenderPass(w.device, VkRenderPassCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_RENDER_PASS_CREATE_INFO)
                    .pAttachments(att).pSubpasses(sub).pDependencies(dep), null, pPass), "vkCreateRenderPass");
            renderPass = pPass.get(0);
        }
    }

    private void createCommandPool() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer pPool = stack.mallocLong(1);
            check(vkCreateCommandPool(w.device, VkCommandPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO)
                    .queueFamilyIndex(w.graphicsQueueFamily)
                    .flags(VK_COMMAND_POOL_CREATE_RESET_COMMAND_BUFFER_BIT), null, pPool), "vkCreateCommandPool");
            cmdPool = pPool.get(0);
        }
    }

    private void loadFonts() {
        try {
            String fontPath = FontAtlas.findSystemFont();
            titleFont = FontAtlas.create(w, cmdPool, fontPath, 60f);
            bodyFont = FontAtlas.create(w, cmdPool, fontPath, 26f);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load font atlas", e);
        }
    }

    private void createDescriptorSetLayout() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorSetLayoutBinding.Buffer binding = VkDescriptorSetLayoutBinding.calloc(1, stack)
                    .binding(0)
                    .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(1)
                    .stageFlags(VK_SHADER_STAGE_FRAGMENT_BIT);

            LongBuffer pLayout = stack.mallocLong(1);
            check(vkCreateDescriptorSetLayout(w.device, VkDescriptorSetLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO)
                    .pBindings(binding), null, pLayout), "vkCreateDescriptorSetLayout");
            descSetLayout = pLayout.get(0);
        }
    }

    private void createDescriptorPool() {
        try (MemoryStack stack = stackPush()) {
            VkDescriptorPoolSize.Buffer size = VkDescriptorPoolSize.calloc(1, stack)
                    .type(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                    .descriptorCount(2); // one per font atlas

            LongBuffer pPool = stack.mallocLong(1);
            check(vkCreateDescriptorPool(w.device, VkDescriptorPoolCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO)
                    .maxSets(2).pPoolSizes(size), null, pPool), "vkCreateDescriptorPool");
            descPool = pPool.get(0);
        }
    }

    private void allocateAndWriteDescriptorSets() {
        try (MemoryStack stack = stackPush()) {
            LongBuffer layouts = stack.longs(descSetLayout, descSetLayout);
            LongBuffer pSets = stack.mallocLong(2);
            check(vkAllocateDescriptorSets(w.device, VkDescriptorSetAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO)
                    .descriptorPool(descPool)
                    .pSetLayouts(layouts), pSets), "vkAllocateDescriptorSets");
            descSets = new long[]{pSets.get(0), pSets.get(1)};

            // Write both atlases
            writeDescriptorSet(stack, descSets[0], titleFont);
            writeDescriptorSet(stack, descSets[1], bodyFont);
        }
    }

    private void writeDescriptorSet(MemoryStack stack, long ds, FontAtlas font) {
        VkDescriptorImageInfo.Buffer ii = VkDescriptorImageInfo.calloc(1, stack)
                .sampler(font.sampler)
                .imageView(font.imageView)
                .imageLayout(VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL);

        VkWriteDescriptorSet.Buffer write = VkWriteDescriptorSet.calloc(1, stack)
                .sType(VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET)
                .dstSet(ds).dstBinding(0).dstArrayElement(0)
                .descriptorType(VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER)
                .descriptorCount(1).pImageInfo(ii);

        vkUpdateDescriptorSets(w.device, write, null);
    }

    private void createPipeline() {
        long vertModule = compileSPIRV(VERT_SRC, Shaderc.shaderc_glsl_vertex_shader);
        long fragModule = compileSPIRV(FRAG_SRC, Shaderc.shaderc_glsl_fragment_shader);

        try (MemoryStack stack = stackPush()) {
            // Shader stages
            ByteBuffer entryPoint = stack.UTF8("main");
            VkPipelineShaderStageCreateInfo.Buffer stages = VkPipelineShaderStageCreateInfo.calloc(2, stack);
            stages.get(0).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_VERTEX_BIT).module(vertModule).pName(entryPoint);
            stages.get(1).sType(VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO)
                    .stage(VK_SHADER_STAGE_FRAGMENT_BIT).module(fragModule).pName(entryPoint);

            // Vertex input — stride = 9 floats = 36 bytes
            VkVertexInputBindingDescription.Buffer binding = VkVertexInputBindingDescription.calloc(1, stack)
                    .binding(0).stride(FLOATS_PER_VERT * Float.BYTES)
                    .inputRate(VK_VERTEX_INPUT_RATE_VERTEX);

            VkVertexInputAttributeDescription.Buffer attrs = VkVertexInputAttributeDescription.calloc(4, stack);
            attrs.get(0).location(0).binding(0).format(VK_FORMAT_R32G32_SFLOAT).offset(0);          // pos
            attrs.get(1).location(1).binding(0).format(VK_FORMAT_R32G32_SFLOAT).offset(8);          // uv
            attrs.get(2).location(2).binding(0).format(VK_FORMAT_R32G32B32A32_SFLOAT).offset(16);   // color
            attrs.get(3).location(3).binding(0).format(VK_FORMAT_R32_SFLOAT).offset(32);            // mode

            VkPipelineVertexInputStateCreateInfo vis = VkPipelineVertexInputStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO)
                    .pVertexBindingDescriptions(binding)
                    .pVertexAttributeDescriptions(attrs);

            VkPipelineInputAssemblyStateCreateInfo ias = VkPipelineInputAssemblyStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO)
                    .topology(VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST)
                    .primitiveRestartEnable(false);

            // Viewport + scissor (static)
            VkViewport.Buffer viewport = VkViewport.calloc(1, stack)
                    .x(0).y(0).width(w.swapchainWidth).height(w.swapchainHeight)
                    .minDepth(0f).maxDepth(1f);
            VkRect2D.Buffer scissor = VkRect2D.calloc(1, stack);
            scissor.offset().set(0, 0);
            scissor.extent().set(w.swapchainWidth, w.swapchainHeight);

            VkPipelineViewportStateCreateInfo vps = VkPipelineViewportStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO)
                    .pViewports(viewport).pScissors(scissor);

            VkPipelineRasterizationStateCreateInfo rast = VkPipelineRasterizationStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO)
                    .polygonMode(VK_POLYGON_MODE_FILL)
                    .cullMode(VK_CULL_MODE_NONE)    // 2D UI — no face culling
                    .frontFace(VK_FRONT_FACE_COUNTER_CLOCKWISE)
                    .lineWidth(1f);

            VkPipelineMultisampleStateCreateInfo ms = VkPipelineMultisampleStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO)
                    .rasterizationSamples(VK_SAMPLE_COUNT_1_BIT)
                    .sampleShadingEnable(false);

            // Alpha blending — required for font antialiasing
            VkPipelineColorBlendAttachmentState.Buffer blendAtt =
                    VkPipelineColorBlendAttachmentState.calloc(1, stack)
                            .blendEnable(true)
                            .srcColorBlendFactor(VK_BLEND_FACTOR_SRC_ALPHA)
                            .dstColorBlendFactor(VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA)
                            .colorBlendOp(VK_BLEND_OP_ADD)
                            .srcAlphaBlendFactor(VK_BLEND_FACTOR_ONE)
                            .dstAlphaBlendFactor(VK_BLEND_FACTOR_ZERO)
                            .alphaBlendOp(VK_BLEND_OP_ADD)
                            .colorWriteMask(VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT
                                    | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT);

            VkPipelineColorBlendStateCreateInfo blend = VkPipelineColorBlendStateCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO)
                    .logicOpEnable(false)
                    .pAttachments(blendAtt);

            // Pipeline layout (one descriptor set layout for the font sampler)
            LongBuffer pLayout = stack.mallocLong(1);
            check(vkCreatePipelineLayout(w.device, VkPipelineLayoutCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO)
                    .pSetLayouts(stack.longs(descSetLayout)), null, pLayout), "vkCreatePipelineLayout");
            pipelineLayout = pLayout.get(0);

            // Create pipeline
            VkGraphicsPipelineCreateInfo.Buffer gpci = VkGraphicsPipelineCreateInfo.calloc(1, stack)
                    .sType(VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO)
                    .pStages(stages)
                    .pVertexInputState(vis)
                    .pInputAssemblyState(ias)
                    .pViewportState(vps)
                    .pRasterizationState(rast)
                    .pMultisampleState(ms)
                    .pColorBlendState(blend)
                    .layout(pipelineLayout)
                    .renderPass(renderPass)
                    .subpass(0)
                    .basePipelineHandle(VK_NULL_HANDLE);

            LongBuffer pPipeline = stack.mallocLong(1);
            check(vkCreateGraphicsPipelines(w.device, VK_NULL_HANDLE, gpci, null, pPipeline),
                    "vkCreateGraphicsPipelines");
            pipeline = pPipeline.get(0);
        }

        vkDestroyShaderModule(w.device, vertModule, null);
        vkDestroyShaderModule(w.device, fragModule, null);
    }

    /**
     * Compile a GLSL source string to SPIR-V and create the VkShaderModule.
     */
    private long compileSPIRV(String source, int kind) {
        long compiler = Shaderc.shaderc_compiler_initialize();
        long options = Shaderc.shaderc_compile_options_initialize();
        try {
            Shaderc.shaderc_compile_options_set_optimization_level(
                    options, Shaderc.shaderc_optimization_level_performance);
            long result = Shaderc.shaderc_compile_into_spv(
                    compiler, source, kind, "launcher_shader.glsl", "main", options);
            try {
                if (Shaderc.shaderc_result_get_compilation_status(result)
                        != Shaderc.shaderc_compilation_status_success) {
                    throw new RuntimeException("Shader compilation failed:\n"
                            + Shaderc.shaderc_result_get_error_message(result));
                }
                ByteBuffer spirv = Shaderc.shaderc_result_get_bytes(result);
                try (MemoryStack stack = stackPush()) {
                    LongBuffer pMod = stack.mallocLong(1);
                    check(vkCreateShaderModule(w.device, VkShaderModuleCreateInfo.calloc(stack)
                            .sType(VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO)
                            .pCode(spirv), null, pMod), "vkCreateShaderModule");
                    return pMod.get(0);
                }
            } finally {
                Shaderc.shaderc_result_release(result);
            }
        } finally {
            Shaderc.shaderc_compile_options_release(options);
            Shaderc.shaderc_compiler_release(compiler);
        }
    }

    private void createFramebuffers() {
        framebuffers = new long[imageViews.length];
        try (MemoryStack stack = stackPush()) {
            LongBuffer pFb = stack.mallocLong(1);
            for (int i = 0; i < imageViews.length; i++) {
                check(vkCreateFramebuffer(w.device, VkFramebufferCreateInfo.calloc(stack)
                                .sType(VK_STRUCTURE_TYPE_FRAMEBUFFER_CREATE_INFO)
                                .renderPass(renderPass)
                                .pAttachments(stack.longs(imageViews[i]))
                                .width(w.swapchainWidth).height(w.swapchainHeight).layers(1),
                        null, pFb), "vkCreateFramebuffer");
                framebuffers[i] = pFb.get(0);
            }
        }
    }

    private void createVertexBuffers() {
        vertexBuffers = new long[MAX_FRAMES];
        vertexMemories = new long[MAX_FRAMES];
        mappedPtrs = new long[MAX_FRAMES];
        try (MemoryStack stack = stackPush()) {
            var ppData = stack.mallocPointer(1);
            for (int i = 0; i < MAX_FRAMES; i++) {
                long[] mem = new long[1];
                vertexBuffers[i] = createBuffer(w, VB_SIZE,
                        VK_BUFFER_USAGE_VERTEX_BUFFER_BIT,
                        VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                        mem);
                vertexMemories[i] = mem[0];
                // Persistently map — we write new data every frame
                vkMapMemory(w.device, vertexMemories[i], 0, VB_SIZE, 0, ppData);
                mappedPtrs[i] = ppData.get(0);
            }
        }
    }

    private void createCommandBuffers() {
        cmdBufs = new VkCommandBuffer[MAX_FRAMES];
        try (MemoryStack stack = stackPush()) {
            var pBufs = stack.mallocPointer(MAX_FRAMES);
            check(vkAllocateCommandBuffers(w.device, VkCommandBufferAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO)
                    .commandPool(cmdPool)
                    .level(VK_COMMAND_BUFFER_LEVEL_PRIMARY)
                    .commandBufferCount(MAX_FRAMES), pBufs), "vkAllocateCommandBuffers");
            for (int i = 0; i < MAX_FRAMES; i++)
                cmdBufs[i] = new VkCommandBuffer(pBufs.get(i), w.device);
        }
    }

    private void createSyncObjects() {
        imageAvailSems = new long[MAX_FRAMES];
        renderDoneSems = new long[MAX_FRAMES];
        inFlightFences = new long[MAX_FRAMES];
        try (MemoryStack stack = stackPush()) {
            VkSemaphoreCreateInfo si = VkSemaphoreCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO);
            VkFenceCreateInfo fi = VkFenceCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_FENCE_CREATE_INFO)
                    .flags(VK_FENCE_CREATE_SIGNALED_BIT); // pre-signaled for first frame
            LongBuffer p = stack.mallocLong(1);
            for (int i = 0; i < MAX_FRAMES; i++) {
                check(vkCreateSemaphore(w.device, si, null, p), "imageAvailSem");
                imageAvailSems[i] = p.get(0);
                check(vkCreateSemaphore(w.device, si, null, p), "renderDoneSem");
                renderDoneSems[i] = p.get(0);
                check(vkCreateFence(w.device, fi, null, p), "inFlightFence");
                inFlightFences[i] = p.get(0);
            }
        }
    }
}
