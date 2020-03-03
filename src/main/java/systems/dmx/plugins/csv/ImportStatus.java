package systems.dmx.plugins.csv;

import java.util.List;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;
import systems.dmx.core.JSONEnabled;

public class ImportStatus implements JSONEnabled {

    private String message;

    private boolean success;

    private List<String> infos;

    public ImportStatus(boolean success, String message, List<String> infos) {
        this.message = message;
        this.success = success;
        this.infos = infos;
    }

    public JSONObject toJSON() {
        try {
            JSONObject json = new JSONObject()//
                    .put("success", success)//
                    .put("message", message)//
                    .put("infos", new JSONArray(infos));
            return json;
        } catch (Exception e) {
            throw new RuntimeException("Serialization failed (" + this + ")", e);
        }
    }
}
