import org.apache.commons.lang3.StringUtils;

public class MyStringUtils {
    public static String firstLetterToLower(String str) {
        return StringUtils.isBlank(str) ? "" : str.replaceFirst(str.substring(0, 1), str.substring(0, 1).toLowerCase());
    }
}
