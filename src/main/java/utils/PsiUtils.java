package utils;

import com.intellij.openapi.project.Project;

public class PsiUtils {
    private Project project;

    private PsiUtils(Project project) {
        this.project = project;
    }

    public static PsiUtils of(Project project) {
        return new PsiUtils(project);
    }

}
