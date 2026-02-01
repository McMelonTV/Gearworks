package ing.boykiss.gearworks.common.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public class ResourceLoader {
    public static ByteBuffer readFileBytes(String filePath) {
        InputStream stream = ClassLoader.getSystemResourceAsStream(filePath);

        if (stream == null) {
            System.err.println("File not found in resources: " + filePath);
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
