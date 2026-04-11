package rf.ebanina.File.Resources;

public interface IResource
{
    <R> R loadResource(Class<R> resourceClazz, String resourceType, String resourceId, String[] extensions);
    <R> R loadResource(String resourceId);
}
