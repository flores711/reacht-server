package flores.caro.model.entities;

import java.util.Map;

public class DataPackage {
    private String action;
    private Map<String, Object> data;

    public DataPackage(String action, Map<String, Object> data) {
        this.action = action;
        this.data = data;
    }

    public String getAction() {
        return action;
    }

    public Map<String, Object> getData() {
        return data;
    }
}

