package be.kbc.eap.nexus.repodata;

import be.kbc.eap.nexus.CondaPath;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CondaRepoDataJsonMerger
    extends ComponentSupport  {


    public void merge(final OutputStream outputStream,
                      final CondaPath condaPath,
                      final Map<Repository, Content> contents)

    {

        CondaRepoDataMerger merger = new CondaRepoDataMerger();

        if (contents.values().size() == 1) {
            try (InputStream is = contents.values().stream().findFirst().get().openInputStream();
                    OutputStreamWriter bw = new OutputStreamWriter(outputStream);
                 InputStreamReader isr = new InputStreamReader(is)) {
                char[] buf = new char[8192];
                int length;
                while ((length = isr.read(buf)) > 0) {
                    bw.write(buf, 0, length);
                }
                bw.flush();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        } else {

            List<byte[]> inputData = contents.values().stream()
                    .map(contentPayload -> {
                        try (InputStream inputStream = contentPayload.openInputStream()) {
                            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                            byte[] data = new byte[16384];
                            int nRead;
                            int totalRead = 0;
                            while((nRead = inputStream.read(data, 0, data.length)) != -1) {
                                buffer.write(data,0,nRead);
                                totalRead += nRead;
                            }

                            //log.debug("Decompressed zstd stream. Number of bytes written: " + totalRead);

                            return buffer.toByteArray();
                        } catch (IOException e) {
                            log.error("Failed to decompress .zst content", e);
                            return null;
                        }
                    })
                    .collect(Collectors.toList());

            // convert to List<InputStream> for merging
            List<InputStream> inputStreams = inputData.stream()
                    .map(ByteArrayInputStream::new)
                    .collect(Collectors.toList());

            String result = merger.mergeRepoDataFiles(inputStreams);
            try (OutputStreamWriter bw = new OutputStreamWriter(outputStream)) {
                log.debug("Write " + result.length() + " chars to outputstream");
                bw.write(result);
                bw.flush();
            } catch (Exception e) {
                log.error(e.getMessage());
            }

        }

    }

}
