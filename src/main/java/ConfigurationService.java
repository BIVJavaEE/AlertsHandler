import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;

public class ConfigurationService {

    private String _path;

    public ConfigurationService(String path) {
        _path = path;
    }

    public ObjectNode read() throws IOException {
        File file = new File(_path);
        return (ObjectNode) new ObjectMapper().readTree(file);
    }
}
