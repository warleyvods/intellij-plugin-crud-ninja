//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//


public class ServiceImplClass {
    public ServiceImplClass() {
    }

    public static String getContent(EntityClasses entityClasses) {
        String serviceName = entityClasses.getEntityName().concat("Service");
        String entityName = entityClasses.getEntityName();
        String entityFieldName = MyStringUtils.firstLetterToLower(entityName);
        StringBuilder content = new StringBuilder();
        //CONSTRUTOR
        content.append("private final ")
                .append(entityClasses.getRepositoryClass().getName())
                .append(" repository; ")
                .append("public ")
                .append(serviceName)
                .append("(")
                .append(entityClasses.getRepositoryClass().getName())
                .append(" repository){ this.repository = repository;")
                .append("} ")

                //SAVE - OK
                .append("public " + entityClasses.getEntityClass().getName() + " save(")
                .append(entityClasses.getEntityClass().getName() + " " + entityFieldName + "){\n")
                .append("  return repository.save(" + entityFieldName + ");}")

                //DELETE - OK
                .append("public void deleteById(" + entityClasses.getIdType() + " id) { repository.deleteById(id); }")


                //FINDBYID - OK
                .append("public ").append(entityClasses.getEntityClass().getName())
                .append(" findById(" + entityClasses.getIdType() + " id) { ")
                .append("return " + "repository.findById(id).orElseThrow(" + entityClasses.getEntityClass().getName() + "NotFoundException::new);}")

                //FINDALL - OK
                .append("public List<").append(entityClasses.getEntityClass().getName()).append(">")
                .append(" findAll() { ")
                .append("return " + "repository.findAll();}")

                //UPDATE
                .append("public " + entityClasses.getEntityClass().getName() + " update(" + entityClasses.getEntityClass().getName())
                .append(" " + entityFieldName + " ," + entityClasses.getIdField().getType().getPresentableText()
                        + " id){  " + entityClasses.getEntityClass().getName() + " data = findById(id);")
                .append("BeanUtil.copyProperties(data, " + entityFieldName + ");")
                .append("return save(" + entityFieldName + "); }");

        return content.toString();
    }
}
