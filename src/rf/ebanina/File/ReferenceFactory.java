package rf.ebanina.File;

@FunctionalInterface
public interface ReferenceFactory<R extends Reference>
{
    R fromString(String line);
}