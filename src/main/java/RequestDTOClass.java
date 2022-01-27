public class RequestDTOClass {

    public static String getCoreString(String entityName, String content) {
        return "import lombok.Getter;\n" +
                "import lombok.Setter;\n" +
                "public class " + entityName + " {" +
                content + "\n" +
                "}";
    }
}
