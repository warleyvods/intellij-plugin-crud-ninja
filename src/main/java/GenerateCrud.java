import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import org.jetbrains.annotations.NotNull;
import utils.PsiUtils;

public class GenerateCrud extends AnAction {

    private Project project;
    private PsiDirectory psiDirectory;
    private PsiUtils psiUtils;

    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        this.project = anActionEvent.getProject();
        this.psiUtils = PsiUtils.of(this.project);
    }
}
