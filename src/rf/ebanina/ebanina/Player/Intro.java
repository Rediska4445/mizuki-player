package rf.ebanina.ebanina.Player;

import java.io.File;
import java.nio.file.Path;

public class Intro
        implements MediaReference
{
    protected String source;

    public Intro() {}

    public Intro(Path path) {
        this.source = path.toFile().getAbsolutePath();
    }

    public Intro(File file) {
        this.source = file.getAbsolutePath();
    }

    public Intro(String source) {
        this.source = source;
    }

    public File getFileSource() {
        return new File(source);
    }

    public Intro setFileSource(File source) {
        this.source = source.getAbsolutePath();
        return this;
    }

    public String getSource() {
        return source;
    }

    public Intro setSource(String source) {
        this.source = source;
        return this;
    }

    @Override
    public String getPath() {
        return source;
    }

    @Override
    public boolean isNetty() {
        return false;
    }
}
