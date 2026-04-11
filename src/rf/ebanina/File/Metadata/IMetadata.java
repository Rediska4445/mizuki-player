package rf.ebanina.File.Metadata;

import java.util.List;
import java.util.Map;

public interface IMetadata
{
    String getMetadataValue(String path, String key);
    void setMetadataValue(String path, String key, String value);
    List<Map.Entry<String, String>> getAllMetadata(String path);
}
