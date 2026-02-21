package ing.boykiss.gearworks.launcher.ui;

import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTPackContext;
import org.lwjgl.stb.STBTTPackedchar;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.vulkan.VkBufferImageCopy;
import org.lwjgl.vulkan.VkCommandBuffer;
import org.lwjgl.vulkan.VkDevice;
import org.lwjgl.vulkan.VkImageCreateInfo;
import org.lwjgl.vulkan.VkImageViewCreateInfo;
import org.lwjgl.vulkan.VkMemoryAllocateInfo;
import org.lwjgl.vulkan.VkMemoryRequirements;
import org.lwjgl.vulkan.VkSamplerCreateInfo;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import static ing.boykiss.gearworks.launcher.ui.LauncherWindow.check;
import static ing.boykiss.gearworks.launcher.ui.VkUtils.beginOneTimeCmd;
import static ing.boykiss.gearworks.launcher.ui.VkUtils.createBuffer;
import static ing.boykiss.gearworks.launcher.ui.VkUtils.endOneTimeCmd;
import static ing.boykiss.gearworks.launcher.ui.VkUtils.findMemoryType;
import static ing.boykiss.gearworks.launcher.ui.VkUtils.transitionImageLayout;
import static org.lwjgl.stb.STBTruetype.stbtt_GetPackedQuad;
import static org.lwjgl.stb.STBTruetype.stbtt_PackBegin;
import static org.lwjgl.stb.STBTruetype.stbtt_PackEnd;
import static org.lwjgl.stb.STBTruetype.stbtt_PackFontRange;
import static org.lwjgl.stb.STBTruetype.stbtt_PackSetOversampling;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_SHADER_READ_BIT;
import static org.lwjgl.vulkan.VK10.VK_ACCESS_TRANSFER_WRITE_BIT;
import static org.lwjgl.vulkan.VK10.VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK;
import static org.lwjgl.vulkan.VK10.VK_BUFFER_USAGE_TRANSFER_SRC_BIT;
import static org.lwjgl.vulkan.VK10.VK_COMPONENT_SWIZZLE_R;
import static org.lwjgl.vulkan.VK10.VK_FILTER_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_FORMAT_R8_UNORM;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_ASPECT_COLOR_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_LAYOUT_UNDEFINED;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TILING_OPTIMAL;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_SAMPLED_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_USAGE_TRANSFER_DST_BIT;
import static org.lwjgl.vulkan.VK10.VK_IMAGE_VIEW_TYPE_2D;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;
import static org.lwjgl.vulkan.VK10.VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT;
import static org.lwjgl.vulkan.VK10.VK_PIPELINE_STAGE_TRANSFER_BIT;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER;
import static org.lwjgl.vulkan.VK10.VK_SAMPLER_MIPMAP_MODE_LINEAR;
import static org.lwjgl.vulkan.VK10.VK_SAMPLE_COUNT_1_BIT;
import static org.lwjgl.vulkan.VK10.VK_SHARING_MODE_EXCLUSIVE;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO;
import static org.lwjgl.vulkan.VK10.VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO;
import static org.lwjgl.vulkan.VK10.vkAllocateMemory;
import static org.lwjgl.vulkan.VK10.vkBindImageMemory;
import static org.lwjgl.vulkan.VK10.vkCmdCopyBufferToImage;
import static org.lwjgl.vulkan.VK10.vkCreateImage;
import static org.lwjgl.vulkan.VK10.vkCreateImageView;
import static org.lwjgl.vulkan.VK10.vkCreateSampler;
import static org.lwjgl.vulkan.VK10.vkDestroyBuffer;
import static org.lwjgl.vulkan.VK10.vkDestroyImage;
import static org.lwjgl.vulkan.VK10.vkDestroyImageView;
import static org.lwjgl.vulkan.VK10.vkDestroySampler;
import static org.lwjgl.vulkan.VK10.vkFreeMemory;
import static org.lwjgl.vulkan.VK10.vkGetImageMemoryRequirements;
import static org.lwjgl.vulkan.VK10.vkMapMemory;
import static org.lwjgl.vulkan.VK10.vkUnmapMemory;

/**
 * STB TrueType font atlas uploaded as a Vulkan R8_UNORM texture.
 * <p>
 * Covers ASCII printable characters (32–127). Glyphs are baked with 2x
 * oversampling for sharp rendering at small sizes. The atlas bitmap is
 * handed to Vulkan via a staging buffer and lives in device-local memory.
 */
class FontAtlas {
    static final int FIRST_CHAR = 32;
    static final int NUM_CHARS = 96; // ASCII 32–127

    private static final int ATLAS_W = 512;
    private static final int ATLAS_H = 512;

    // Larger atlas for high-px fonts that need more packing space
    private static final int ATLAS_W_LARGE = 1024;
    private static final int ATLAS_H_LARGE = 1024;
    /**
     * Font size used during baking (pixels). Exposed so the renderer can compute baseline offsets.
     */
    final float sizePx;
    // Vulkan resources
    long image;
    long imageMemory;
    long imageView;
    long sampler;
    // STB packed-char data: positions + UVs for every glyph.
    // Must remain alive as long as getPackedQuad() is called.
    private STBTTPackedchar.Buffer chardata;
    /**
     * Actual atlas dimensions used when baking this atlas.
     */
    private int atlasW;
    private int atlasH;

    private FontAtlas(float sizePx) {
        this.sizePx = sizePx;
        this.atlasW = ATLAS_W;
        this.atlasH = ATLAS_H;
    }

    // -------------------------------------------------------------------------
    // Factory
    // -------------------------------------------------------------------------

    /**
     * Loads a TTF file, bakes glyphs into an alpha bitmap, and
     * uploads the result to a device-local Vulkan texture.
     * Atlas size is chosen automatically: fonts >= 40 px use a 1024×1024 atlas
     * so that 60 px glyphs at 2× oversampling (~120 px effective) all fit.
     *
     * @param commandPool A command pool from which to allocate the one-time upload command buffer.
     */
    static FontAtlas create(LauncherWindow w, long commandPool, String fontPath, float sizePx)
            throws IOException {
        byte[] bytes = Files.readAllBytes(Path.of(fontPath));
        ByteBuffer fontBuf = MemoryUtil.memAlloc(bytes.length);
        try {
            fontBuf.put(bytes).flip();
            return bakeAndUpload(w, commandPool, fontBuf, sizePx);
        } finally {
            MemoryUtil.memFree(fontBuf);
        }
    }

    private static FontAtlas bakeAndUpload(LauncherWindow w, long commandPool,
                                           ByteBuffer fontBuf, float sizePx) {
        FontAtlas atlas = new FontAtlas(sizePx);

        // For large font sizes (>= 40 px) the 2× oversampled glyphs are too big
        // to all fit in 512×512.  Use a 1024×1024 atlas instead.
        int aw = sizePx >= 40f ? ATLAS_W_LARGE : ATLAS_W;
        int ah = sizePx >= 40f ? ATLAS_H_LARGE : ATLAS_H;
        atlas.atlasW = aw;
        atlas.atlasH = ah;

        // 1. Bake glyphs to a CPU-side grayscale bitmap
        ByteBuffer bitmap = MemoryUtil.memAlloc(aw * ah);
        STBTTPackedchar.Buffer chardata = STBTTPackedchar.malloc(NUM_CHARS);

        try (STBTTPackContext pc = STBTTPackContext.malloc()) {
            stbtt_PackBegin(pc, bitmap, aw, ah, 0, 1, MemoryUtil.NULL);
            stbtt_PackSetOversampling(pc, 2, 2); // 2× oversampling for sub-pixel sharpness
            stbtt_PackFontRange(pc, fontBuf, 0, sizePx, FIRST_CHAR, chardata);
            stbtt_PackEnd(pc);
        }

        atlas.chardata = chardata;

        // 2. Upload bitmap → Vulkan texture
        atlas.uploadTexture(w, commandPool, bitmap);
        MemoryUtil.memFree(bitmap);

        return atlas;
    }

    // -------------------------------------------------------------------------
    // Glyph queries
    // -------------------------------------------------------------------------

    /**
     * Returns the path to a usable system TTF or throws with a helpful message.
     */
    static String findSystemFont() {
        String[] candidates = {
                // Arch / Manjaro
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/ttf-dejavu/DejaVuSans.ttf",
                // Ubuntu / Debian
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                // Fedora / RHEL
                "/usr/share/fonts/dejavu-sans-fonts/DejaVuSans.ttf",
                // Liberation (common fallback)
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/liberation-sans/LiberationSans-Regular.ttf",
                "/usr/share/fonts/liberation/LiberationSans-Regular.ttf",
                // Noto
                "/usr/share/fonts/noto/NotoSans-Regular.ttf",
                "/usr/share/fonts/truetype/noto/NotoSans-Regular.ttf",
                // macOS
                "/System/Library/Fonts/Helvetica.ttc",
                "/System/Library/Fonts/SFNSDisplay.otf",
                // Windows
                "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/arial.ttf",
        };
        for (String path : candidates) {
            if (Path.of(path).toFile().exists()) return path;
        }
        throw new RuntimeException(
                "No system TTF font found. Install dejavu-fonts or liberation-fonts, "
                        + "or add a font path to FontAtlas.findSystemFont().");
    }

    /**
     * Fills {@code q} with the screen-space quad for a single glyph.
     * {@code xBuf[0]} and {@code yBuf[0]} are both IN/OUT: pass the current
     * pen position; they will be advanced by the glyph's advance width.
     *
     * @param charIndex glyph index = (char - FIRST_CHAR)
     */
    void getPackedQuad(int charIndex, FloatBuffer xBuf, FloatBuffer yBuf, STBTTAlignedQuad q) {
        stbtt_GetPackedQuad(chardata, atlasW, atlasH, charIndex, xBuf, yBuf, q, false);
    }

    /**
     * Returns the advance width of the given string in pixels. Used for horizontal centering.
     */
    float measureText(String text) {
        try (MemoryStack stack = stackPush()) {
            FloatBuffer x = stack.floats(0.0f);
            FloatBuffer y = stack.floats(0.0f);
            STBTTAlignedQuad q = STBTTAlignedQuad.malloc(stack);
            for (int i = 0; i < text.length(); i++) {
                int idx = text.charAt(i) - FIRST_CHAR;
                if (idx < 0 || idx >= NUM_CHARS) continue;
                stbtt_GetPackedQuad(chardata, atlasW, atlasH, idx, x, y, q, false);
            }
            return x.get(0);
        }
    }

    // -------------------------------------------------------------------------
    // Vulkan upload
    // -------------------------------------------------------------------------

    void cleanup(VkDevice device) {
        vkDestroySampler(device, sampler, null);
        vkDestroyImageView(device, imageView, null);
        vkDestroyImage(device, image, null);
        vkFreeMemory(device, imageMemory, null);
        if (chardata != null) chardata.free();
    }

    // -------------------------------------------------------------------------
    // System font discovery
    // -------------------------------------------------------------------------

    private void uploadTexture(LauncherWindow w, long commandPool, ByteBuffer pixels) {
        int pixelCount = atlasW * atlasH;

        // --- Staging buffer -------------------------------------------------
        long[] stagingMem = new long[1];
        long stagingBuf = createBuffer(w, pixelCount,
                VK_BUFFER_USAGE_TRANSFER_SRC_BIT,
                VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT,
                stagingMem);

        try (MemoryStack stack = stackPush()) {
            var ppData = stack.mallocPointer(1);
            vkMapMemory(w.device, stagingMem[0], 0, pixelCount, 0, ppData);
            MemoryUtil.memCopy(MemoryUtil.memAddress(pixels), ppData.get(0), pixelCount);
            vkUnmapMemory(w.device, stagingMem[0]);

            // --- Device-local R8 image ------------------------------------
            LongBuffer pHandle = stack.mallocLong(1);

            VkImageCreateInfo ici = VkImageCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO)
                    .imageType(VK_IMAGE_TYPE_2D)
                    .format(VK_FORMAT_R8_UNORM)
                    .mipLevels(1).arrayLayers(1)
                    .samples(VK_SAMPLE_COUNT_1_BIT)
                    .tiling(VK_IMAGE_TILING_OPTIMAL)
                    .usage(VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT)
                    .sharingMode(VK_SHARING_MODE_EXCLUSIVE)
                    .initialLayout(VK_IMAGE_LAYOUT_UNDEFINED);
            ici.extent().set(atlasW, atlasH, 1);
            check(vkCreateImage(w.device, ici, null, pHandle), "vkCreateImage (font)");
            image = pHandle.get(0);

            VkMemoryRequirements imgMR = VkMemoryRequirements.malloc(stack);
            vkGetImageMemoryRequirements(w.device, image, imgMR);

            VkMemoryAllocateInfo iai = VkMemoryAllocateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO)
                    .allocationSize(imgMR.size())
                    .memoryTypeIndex(findMemoryType(w.physicalDevice,
                            imgMR.memoryTypeBits(), VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT));
            check(vkAllocateMemory(w.device, iai, null, pHandle), "vkAllocateMemory (font)");
            imageMemory = pHandle.get(0);
            vkBindImageMemory(w.device, image, imageMemory, 0);

            // --- Upload via one-time command buffer -----------------------
            VkCommandBuffer cmd = beginOneTimeCmd(w, commandPool);

            transitionImageLayout(cmd, image,
                    VK_IMAGE_LAYOUT_UNDEFINED, VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
                    0, VK_ACCESS_TRANSFER_WRITE_BIT,
                    VK_PIPELINE_STAGE_TOP_OF_PIPE_BIT, VK_PIPELINE_STAGE_TRANSFER_BIT);

            VkBufferImageCopy.Buffer region = VkBufferImageCopy.calloc(1, stack)
                    .bufferOffset(0).bufferRowLength(0).bufferImageHeight(0);
            region.imageSubresource()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .mipLevel(0).baseArrayLayer(0).layerCount(1);
            region.imageOffset().set(0, 0, 0);
            region.imageExtent().set(atlasW, atlasH, 1);
            vkCmdCopyBufferToImage(cmd, stagingBuf, image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, region);

            transitionImageLayout(cmd, image,
                    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
                    VK_ACCESS_TRANSFER_WRITE_BIT, VK_ACCESS_SHADER_READ_BIT,
                    VK_PIPELINE_STAGE_TRANSFER_BIT, VK_PIPELINE_STAGE_FRAGMENT_SHADER_BIT);

            endOneTimeCmd(w, cmd, commandPool);

            // --- Image view (R channel → sample as red) ------------------
            VkImageViewCreateInfo vci = VkImageViewCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO)
                    .image(image)
                    .viewType(VK_IMAGE_VIEW_TYPE_2D)
                    .format(VK_FORMAT_R8_UNORM);
            // Swizzle: sample as (r, r, r, r) so text has colour from our vertex colour
            vci.components().r(VK_COMPONENT_SWIZZLE_R).g(VK_COMPONENT_SWIZZLE_R)
                    .b(VK_COMPONENT_SWIZZLE_R).a(VK_COMPONENT_SWIZZLE_R);
            vci.subresourceRange()
                    .aspectMask(VK_IMAGE_ASPECT_COLOR_BIT)
                    .baseMipLevel(0).levelCount(1).baseArrayLayer(0).layerCount(1);
            check(vkCreateImageView(w.device, vci, null, pHandle), "vkCreateImageView (font)");
            imageView = pHandle.get(0);

            // --- Bilinear sampler ----------------------------------------
            VkSamplerCreateInfo sci = VkSamplerCreateInfo.calloc(stack)
                    .sType(VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO)
                    .magFilter(VK_FILTER_LINEAR).minFilter(VK_FILTER_LINEAR)
                    .addressModeU(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER)
                    .addressModeV(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER)
                    .addressModeW(VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_BORDER)
                    .mipmapMode(VK_SAMPLER_MIPMAP_MODE_LINEAR)
                    .borderColor(VK_BORDER_COLOR_FLOAT_TRANSPARENT_BLACK)
                    .unnormalizedCoordinates(false)
                    .compareEnable(false)
                    .anisotropyEnable(false);
            check(vkCreateSampler(w.device, sci, null, pHandle), "vkCreateSampler (font)");
            sampler = pHandle.get(0);
        }

        // Clean up staging
        vkDestroyBuffer(w.device, stagingBuf, null);
        vkFreeMemory(w.device, stagingMem[0], null);
    }
}
