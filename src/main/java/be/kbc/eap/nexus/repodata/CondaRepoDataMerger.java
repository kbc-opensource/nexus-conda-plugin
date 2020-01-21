package be.kbc.eap.nexus.repodata;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.sonatype.goodies.common.Loggers;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;

public class CondaRepoDataMerger {

    protected final Logger log = Loggers.getLogger(this);

    public String mergeRepoDataFiles(List<InputStream> inputStreams) {


        if (inputStreams.size() == 0) return null;


        log.debug("Merge repodata for " +  inputStreams.size() + " input streams");

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();


        try {
            InputStreamReader rdr = new InputStreamReader(inputStreams.get(inputStreams.size()-1));
            JsonObject result = gson.fromJson(rdr, JsonObject.class);
            rdr.close();

            int j = inputStreams.size()-2;
            while (j >= 0) {

                rdr = new InputStreamReader(inputStreams.get(j));
                JsonObject otherResult = gson.fromJson(rdr, JsonObject.class);
                rdr.close();

                JsonObject parentPackages = result.getAsJsonObject("packages");
                JsonObject childPackages = otherResult.getAsJsonObject("packages");

                for (Map.Entry<String, JsonElement> key : childPackages.entrySet()) {
                    parentPackages.add(key.getKey(), key.getValue());
                }

                j--;
            }


            String jsonResult = gson.toJson(result);

            return jsonResult;


        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

}
