package rf.ebanina.File;

public interface IFileProcessor {

    <T> void save(String path, String track, String type, T value);

    <T> T read(String path, String track, String type, T ifNull);

    String name(String name);
}