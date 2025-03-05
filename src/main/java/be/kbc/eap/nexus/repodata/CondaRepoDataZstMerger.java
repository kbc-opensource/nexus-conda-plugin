package be.kbc.eap.nexus.repodata;

import be.kbc.eap.nexus.CondaPath;
import be.kbc.eap.nexus.util.ZstdUtils;
import org.sonatype.goodies.common.ComponentSupport;
import org.sonatype.nexus.repository.Repository;
import org.sonatype.nexus.repository.view.Content;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CondaRepoDataZstMerger extends ComponentSupport {

    public void merge(final OutputStream outputStream,
                      final CondaPath condaPath,
                      final Map<Repository, Content> contents)

    {

        log.debug("Merging content for " + condaPath.getPath());

        CondaRepoDataMerger merger = new CondaRepoDataMerger();

        if(contents.values().size() == 1) {
            // for one stream we don't need to decompress and compress
            log.debug("Only one stream so returning the file directly without decompressing");

            try (InputStream is = contents.values().stream().findFirst().get().openInputStream();
                    BufferedOutputStream bw = new BufferedOutputStream(outputStream);
                 BufferedInputStream isr = new BufferedInputStream(is)) {
                byte[] buf = new byte[8192];
                int length;
                while ((length = isr.read(buf)) > 0) {
                    bw.write(buf, 0, length);
                }
                bw.flush();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }
        else {

            log.debug("Multiple streams so decompressing, merging, and compressing the files");

            List<byte[]> decompressedData = contents.values().stream()
                    .map(contentPayload -> {
                        try (InputStream inputStream = contentPayload.openInputStream()) {
                            return ZstdUtils.decompressZstdStream(inputStream);
                        } catch (IOException e) {
                            log.error("Failed to decompress .zst content", e);
                            return null;
                        }
                    })
                    .collect(Collectors.toList());

            // convert to List<InputStream> for merging
            List<InputStream> inputStreams = decompressedData.stream()
                    .map(ByteArrayInputStream::new)
                    .collect(Collectors.toList());

            // Merge contents
            String mergedJson = merger.mergeRepoDataFiles(inputStreams);


            try (BufferedOutputStream bw = new BufferedOutputStream(outputStream)) {
                log.debug("Write " + mergedJson.length() + " chars to outputstream");
                // recompress JSON to .zst file
                byte[] recompressedData = ZstdUtils.compressZstdData(mergedJson.getBytes(StandardCharsets.UTF_8));
                bw.write(recompressedData);
                bw.flush();
            } catch (Exception e) {
                log.error(e.getMessage());
            }
        }


//        log.debug("Closing open streams");
//        streams.stream().map(s -> {
//            try {
//                s.close();
//            } catch (IOException e) {
//                //e.printStackTrace();
//            }
//            return s;
//        });
//
    }

}
