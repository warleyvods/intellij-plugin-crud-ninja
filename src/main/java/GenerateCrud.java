import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.tree.java.PsiNameValuePairImpl;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class GenerateCrud extends AnAction {

    private static final String TITLE_INFORMATION = "Information";
    private static final String TITLE = "Madaoo Demo";
    private Project project;
    private PsiDirectory containerDirectory;
    private PsiUtils psiUtils;
    private Module module;
    private PsiDirectory parentDirectory;
    private PsiPackageStatement packageFile;
    private PsiDirectory containerDirectoryTest;
    private Project testProject;
    private PsiDirectory controllerTestDirectory;


    @Override
    public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
        this.project = anActionEvent.getProject();
        this.psiUtils = PsiUtils.of(this.project);
        PsiFile psiFile = anActionEvent.getData(LangDataKeys.PSI_FILE);
        System.out.println();

        //VERIFICA SE O ARQUIVO É UM JAVA FILE.
        if (!Objects.requireNonNull(psiFile).getFileType().getName().equals("JAVA")) {
            Messages.showMessageDialog(this.project, "You shoud run this generator on Java file", "Information", Messages.getInformationIcon());
            return;
        }

        PsiJavaFile javaFile = (PsiJavaFile)psiFile;
        PsiClass[] classes = javaFile.getClasses();
        if (0 == classes.length) {
            Messages.showMessageDialog(this.project, "No class found", "Information", Messages.getInformationIcon());
            return;
        }

        PsiClass aClass = classes[0];
        if (null == aClass.getAnnotation("javax.persistence.Entity")) {
            Messages.showMessageDialog(this.project, "This class is not an entity", "Information", Messages.getInformationIcon());
            return;
        }

        this.containerDirectory = javaFile.getContainingDirectory();
        this.containerDirectory = javaFile.getContainingDirectory();
        this.module = FileIndexFacade.getInstance(this.project).getModuleForFile(psiFile.getVirtualFile());
        EntityClasses entityClasses = (new EntityClasses()).setServiceDirectory(createServiceDirectory()).setEntityClass(aClass);
        this.parentDirectory = entityClasses.getServiceDirectory().getParent();
        Optional<VirtualFile> optionalVirtualFile = ProjectRootManager.getInstance(this.project).getModuleSourceRoots(JavaModuleSourceRootTypes.TESTS).stream().findFirst();

        if (optionalVirtualFile.isPresent()) {
            VirtualFile virtualFile = optionalVirtualFile.get();
            this.containerDirectoryTest = PsiManager.getInstance(this.project).findDirectory(virtualFile);
            String packageName = javaFile.getPackageName();
            String last = packageName.substring(packageName.lastIndexOf('.') + 1);
            String folderName = packageName.replace(last, "");
            this.controllerTestDirectory = this.psiUtils.getOrCreateSubDirectory(this.containerDirectoryTest, folderName + "controller");
            this.testProject = this.containerDirectoryTest.getProject();
        } else {
            Messages.showMessageDialog(this.project, "Module test not defined", "Information", Messages.getInformationIcon());
        }
        WriteCommandAction.runWriteCommandAction(this.project, () -> createRepository(entityClasses));

    }

    private PsiDirectory createServiceDirectory() {
        return null == this.containerDirectory.getParent() ? this.containerDirectory : this.psiUtils.getOrCreateSubDirectory(this.containerDirectory.getParent(), "service");
    }

    private void createRepository(EntityClasses entityClasses) {
        String entityName = entityClasses.getEntityName();

        assert entityName != null;

        PsiDirectory repositoryDirectory = null == this.containerDirectory.getParent() ? this.containerDirectory : this.psiUtils.getOrCreateSubDirectory(this.containerDirectory.getParent(), "repository");
        String repositoryName = entityName.replace("Entity", "").concat("Repository");
        ClassCreator.of(this.project).init(repositoryName, "@Repository public interface "
                        + repositoryName + " extends JpaRepository<" + entityClasses.getEntityClassName() + ", " + entityClasses.getIdType() + ">{\n}")
                .importClass(entityClasses.getEntityClass())
                .importClass("org.springframework.data.jpa.repository.JpaRepository")
                .importClass("org.springframework.stereotype.Repository")
                .addTo(repositoryDirectory)
                .and((repositoryClass) -> {
                    this.createClasses(entityClasses.setRepositoryClass(repositoryClass));
                    this.createService(entityClasses);
                });
    }

    private void createService(EntityClasses entityClasses) {
        String serviceName = entityClasses.getEntityName().concat("Service");
        PsiDirectory serviceImplDirectory = entityClasses.getServiceDirectory();
        StringBuilder content = (new StringBuilder("@Service public class "))
                .append(serviceName).append("{");
        PsiClass repositoryClass = entityClasses.getRepositoryClass();
        String saveAllMethod = "save";
        if (0 != repositoryClass.findMethodsByName("saveAll", true).length) {
            saveAllMethod = "saveAll";
        }

        content.append(ServiceImplClass.getContent(entityClasses));
        content.append("}");
        ClassCreator.of(this.project).init(serviceName, content.toString())
                .importClass(entityClasses.getEntityClass())
                .importClass("org.springframework.stereotype.Service")
                .importClass("java.util.List").importClass("PageHelper")
                .importClass("AbstractBaseEntityService")
                .importClass("com.github.pagehelper.PageInfo")
                .addTo(serviceImplDirectory).and((implClass) -> {
                    this.psiUtils.importClass(implClass, new PsiClass[]{
                            entityClasses.getRepositoryClass(),
                            entityClasses.getMapperClass(),
                            entityClasses.getDtoClass()});
                    entityClasses.setServiceClass(implClass);
                    this.psiUtils.importClass(implClass, new PsiClass[0]);
//                    this.createController(entityClasses);
                });
    }

    private void createClasses(EntityClasses entityClasses) {
        String className = entityClasses.getEntityClassName();
        assert className != null;

        PsiAnnotation[] annotations = entityClasses.getEntityClass().getAnnotations();
        HashMap<String, List<JvmAnnotationAttribute>> tableAnnotations = getTableAnnotations(annotations);
        List<JvmAnnotationAttribute> annotationAttributes = tableAnnotations.get("org.hibernate.annotations.Table");
        String text = "";
        if (annotationAttributes != null){
            text = ((PsiNameValuePairImpl) annotationAttributes.get(1)).getValue().getText();
        }

        String entityName = className.replace("Entity", "");
        PsiDirectory dtoDirectory = null == this.containerDirectory.getParent() ? this.containerDirectory : this.psiUtils.getOrCreateSubDirectory(this.containerDirectory.getParent(), "dtos");
        String dtoRequest = "\npublic class " + entityName + "RequestDTO" + "{}";
        String dtoResponse = "\npublic class " + entityName + "ResponseDTO" + "{}";

        ClassCreator.of(this.project).init(entityName + "RequestDTO", dtoRequest).copyFields(entityClasses.getEntityClass()).addTo(dtoDirectory);
        ClassCreator.of(this.project).init(entityName + "ResponseDTO", dtoResponse).copyFields(entityClasses.getEntityClass()).addTo(dtoDirectory).and((dtoClass) -> {
//            this.createMapperClass(entityClasses.setDtoClass(dtoClass));
        });

    }

    private HashMap<String, List<JvmAnnotationAttribute>> getTableAnnotations(PsiAnnotation[] annotations) {
        HashMap<String, List<JvmAnnotationAttribute>> annotationMaps = new HashMap<>();
        for (PsiAnnotation annotation : annotations) {
            annotationMaps.put(annotation.getQualifiedName(),annotation.getAttributes());
        }
        return annotationMaps;
    }

}
