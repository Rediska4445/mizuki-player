package rf.ebanina.File;

@FunctionalInterface
public interface ReferenceFactory<R extends Referencable>
{
    R fromString(String line);
}