package ing.boykiss.gearworks.client.render.font;

import ing.boykiss.gearworks.common.util.ResourceLoader;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL46;
import org.lwjgl.stb.STBTTAlignedQuad;
import org.lwjgl.stb.STBTTBakedChar;
import org.lwjgl.stb.STBTruetype;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

// TODO: refactor this
public class Font {
    private final int fontTexId;
    private final STBTTBakedChar.Buffer fontCharData;

    public Font(String fontFile) {
        ByteBuffer bitmap = BufferUtils.createByteBuffer(512 * 512);
        fontCharData = STBTTBakedChar.malloc(96);

        ByteBuffer ttf = ResourceLoader.readFileBytes(fontFile);
        if (ttf == null) throw new RuntimeException("Failed to load font file");
        STBTruetype.stbtt_BakeFontBitmap(ttf, 24, bitmap, 512, 512, 32, fontCharData);

        fontTexId = GL46.glGenTextures();
        GL46.glBindTexture(GL46.GL_TEXTURE_2D, fontTexId);
        GL46.glTexImage2D(GL46.GL_TEXTURE_2D, 0, GL46.GL_ALPHA, 512, 512, 0, GL46.GL_ALPHA, GL46.GL_UNSIGNED_BYTE, bitmap);
        GL46.glTexParameteri(GL46.GL_TEXTURE_2D, GL46.GL_TEXTURE_MIN_FILTER, GL46.GL_LINEAR);
    }

    public void drawText(String text, float x, float y) {
        FloatBuffer xb = BufferUtils.createFloatBuffer(1).put(0, x);
        FloatBuffer yb = BufferUtils.createFloatBuffer(1).put(0, y);
        STBTTAlignedQuad q = STBTTAlignedQuad.malloc();

        GL46.glBindTexture(GL46.GL_TEXTURE_2D, fontTexId);
        GL46.glBegin(GL46.GL_QUADS);

        if (fontCharData == null) throw new RuntimeException("Failed to load font char data");
        for (int i = 0; i < text.length(); i++) {
            STBTruetype.stbtt_GetBakedQuad(fontCharData, 512, 512, text.charAt(i) - 32, xb, yb, q, true);

            GL46.glTexCoord2f(q.s0(), q.t0());
            GL46.glVertex2f(q.x0(), q.y0());
            GL46.glTexCoord2f(q.s1(), q.t0());
            GL46.glVertex2f(q.x1(), q.y0());
            GL46.glTexCoord2f(q.s1(), q.t1());
            GL46.glVertex2f(q.x1(), q.y1());
            GL46.glTexCoord2f(q.s0(), q.t1());
            GL46.glVertex2f(q.x0(), q.y1());
        }

        GL46.glEnd();
        q.free();
    }
}
