

public class EntityMapperClass {
    public EntityMapperClass() {
    }

    public static String getCoreString() {
        return "public interface EntityMapper<E, T, D> {\n" +
                "\n" +
                "    E toDomain(T requestDTO);\n" +
                "\n" +
                "    D fromDomain(E entity);\n" +
                "\n" +
                "    List<D> fromDomainList(List<E> entityList);\n" +
                "}\n";
    }
}
