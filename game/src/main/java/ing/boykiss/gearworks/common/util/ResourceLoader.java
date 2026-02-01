package ing.boykiss.gearworks.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceLoader {
    public static ByteBuffer loadFileAsByteBuffer(String file) {
        InputStream stream = ClassLoader.getSystemResourceAsStream(file);

        if (stream == null) {
            System.err.println("File not found in resources: " + file);
            return null;
        }

        try {
            byte[] data = stream.readAllBytes();

            ByteBuffer buffer = ByteBuffer.allocateDirect(data.length);
            buffer.put(data);
            buffer.flip();

            return buffer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
