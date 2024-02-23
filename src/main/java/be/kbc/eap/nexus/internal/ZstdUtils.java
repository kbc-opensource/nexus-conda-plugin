package be.kbc.eap.nexus.internal;

import com.github.luben.zstd.Zstd;
import com.github.luben.zstd.ZstdInputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ZstdUtils {


    /**
     *
     * @param data the data to compress to zst as byte array
     * @return the compressed data as byte array
     * @throws IOException if an IO error occurs during compression
     */
    public static byte[] compressZstdData(byte[] data) throws IOException {
        byte[] compressedData = Zstd.compress(data);

        if(Zstd.isError(compressedData.length)){
            throw new IOException("Zstd compression error: " + Zstd.getErrorName(compressedData.length));
        }
        return compressedData;
    }

    /**
     *
     * @param compressedInputStream The InputStream of the compressed data
     * @return a byte array containing decompressed data
     * @throws IOException if an IOException is encountered while decompressing
     */
    public static byte[] decompressZstdStream(InputStream compressedInputStream) throws IOException{
        try(ZstdInputStream zstdInputStream = new ZstdInputStream(compressedInputStream)){
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            int nRead;
            int totalRead = 0;
            while((nRead = zstdInputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data,0,nRead);
                totalRead += nRead;
            }

            //log.debug("Decompressed zstd stream. Number of bytes written: " + totalRead);

            return buffer.toByteArray();
        }
    }

}
