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
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jps.model.java.JavaModuleSourceRootTypes;

import java.util.*;
import java.util.function.Consumer;

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

        PsiJavaFile javaFile = (PsiJavaFile) psiFile;
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
                            entityClasses.getRepositoryClass()
                    });
                    entityClasses.setServiceClass(implClass);
                    this.psiUtils.importClass(implClass, new PsiClass[0]);
                    this.createMapperClass(entityClasses);
                });
    }

    private void createController(EntityClasses entityClasses) {
        PsiDirectory controllerDirectory = null == this.containerDirectory.getParent() ? this.containerDirectory :
                this.psiUtils.getOrCreateSubDirectory(this.containerDirectory.getParent(), "controller");
        entityClasses.setControllerDirectory(controllerDirectory);
//        this.createControllerInterface(entityClasses);
        PsiFile[] files = controllerDirectory.getFiles();
        String prefix = "/api/";
        if (0 != files.length) {
            PsiFile[] var6 = files;
            int var7 = files.length;

            for (int var8 = 0; var8 < var7; ++var8) {
                PsiFile file = var6[var8];
                if (file instanceof PsiJavaFile) {
                    Optional<String> value = this.psiUtils.getAnnotationValue(file, "org.springframework.web.bind.annotation.RequestMapping", "value");
                    if (value.isPresent() && !((String) value.get()).startsWith("/api")) {
                        prefix = "/";
                    }
                }
            }
        }

        Optional<PsiClass> apiClass = this.psiUtils.findClass("io.swagger.annotations.Api");
        boolean useAPI = apiClass.isPresent();
        Optional<PsiClass> baseClassOptional = this.psiUtils.findClass("BaseController");
        String suffix = "Controller";
        if (!baseClassOptional.isPresent()) {
            baseClassOptional = this.psiUtils.findClass("BaseResource");
            suffix = "Resource";
            if (!baseClassOptional.isPresent()) {
                suffix = "Controller";
            }
        }

        String entityName = entityClasses.getEntityName();
        String controllerPath = (String) Arrays.stream(StringUtils.splitByCharacterTypeCamelCase(entityName)).reduce((s1, s2) -> {
            return s1.toLowerCase().concat("-").concat(s2.toLowerCase());
        }).orElse("");
        String var10000 = controllerPath.substring(0, 1).toLowerCase();
        controllerPath = var10000 + controllerPath.substring(1);
        entityClasses.setControllerPath(controllerPath);
        StringBuilder content = new StringBuilder();
        content.append("@RequestMapping(\"").append(prefix).append(controllerPath).append("/"+"\")")
                .append("@RestController");

        content.append(" public class ").append(entityName).append(suffix);
        String entityFieldName = MyStringUtils.firstLetterToLower(entityName);
        entityClasses.setEntityFieldName(entityFieldName);
        StringBuilder var19 = content.append("{");

        //CONSTRUCTOR
        String var10002 = entityClasses.getMapperClass().getName();
        String var10001 = entityClasses.getServiceClass().getName();
        var19.append("private final " + var10001 + " " + entityFieldName + "Service; ");
        var19.append("private final " + var10002 + " " + entityFieldName + "Mapper; ");
        var19.append("public " + entityName + suffix + "(" + entityClasses.getServiceClass().getName() + " " + entityFieldName + "Service ," + entityClasses.getMapperClass().getName() + " " + entityFieldName + "Mapper) {");
        var19.append("this." + entityFieldName + "Service = " + entityFieldName + "Service;");
        var19.append("this." + entityFieldName + "Mapper = " + entityFieldName + "Mapper;");
        var19.append("}");


        //SAVE
        var19.append("@ResponseStatus(HttpStatus.CREATED)\n@PostMapping ");
        var19.append("public "+ entityClasses.getDtoResponseClass().getName() +" save(@RequestBody @Valid ");
        var19.append(entityClasses.getDtoRequestClass().getName()).append(" ").append(entityFieldName + "RequestDTO").append(") { ");
        var19.append(entityClasses.getEntityClass().getName() + " " + entityFieldName +" = " + entityFieldName + "Service.save(" + entityFieldName + "Mapper.toDomain(" + entityFieldName + "RequestDTO));");
        var19.append("return "+ entityFieldName + "Mapper.fromDomain(" + entityFieldName +  "); }");


        //FINDBYID
        var19.append("@ResponseStatus(HttpStatus.OK)\n@GetMapping(\"{id}\") public "+ entityClasses.getDtoResponseClass().getName() +" findById(@PathVariable ");
        var19.append(entityClasses.getIdField().getType().getPresentableText() + " id) {");
        var19.append("return "+ entityFieldName + "Mapper.fromDomain(" + entityFieldName + "Service.findById(id)"+ "); }");

        //DELETE
        var19.append("@ResponseStatus(HttpStatus.NO_CONTENT)\n@DeleteMapping(\"{id}\") public void delete(@PathVariable ");
        var19.append(entityClasses.getIdField().getType().getPresentableText() + " id) {").append(entityFieldName);
        var19.append("Service.deleteById(id);\n}");

        //UPDATE
        var19.append("@ResponseStatus(HttpStatus.OK)\n@PutMapping(\"{id}\") public " + entityClasses.getDtoResponseClass().getName());
        var19.append(" update(@RequestBody @Valid " + entityClasses.getDtoRequestClass().getName() + " " + entityFieldName + "RequestDTO, @PathVariable " + entityClasses.getIdField().getType().getPresentableText() + " id) { ");
        var19.append(entityName + " update = " + entityFieldName+"Service.update(" + entityFieldName + "Mapper.toDomain(" + entityFieldName+"RequestDTO), id);");
        var19.append("return " + entityFieldName + "Mapper.fromDomain(update);\n}");

        //LIST ALL
        var19.append("@ResponseStatus(HttpStatus.OK)\n@GetMapping public " + "List<" + entityClasses.getDtoResponseClass().getName() + "> listAll() {");
        var19.append("return "+ entityFieldName + "Mapper.fromDomainList(" + entityFieldName + "Service.findAll()"+ "); }");

        var19.append("\n}");

        ClassCreator.of(this.project).init(entityClasses.getEntityName() + suffix, content.toString())
                .importClass("org.springframework.web.bind.annotation.RequestMapping")
                .importClass("org.springframework.web.bind.annotation.PostMapping")
                .importClass("org.springframework.web.bind.annotation.ResponseStatus")
                .importClass("org.springframework.http.HttpStatus")
                .importClass("GetMapping").importClass("DeleteMapping")
                .importClass("org.springframework.web.bind.annotation.RequestBody")
                .importClass("PathVariable")
                .importClass("RequestParam")
                .importClass("java.util.List")
                .importClass("javax.validation.Valid")
                .importClass(entityClasses.getControllerClass())
                .importClass(entityClasses.getEntityClass())
                .importClass(entityClasses.getDtoResponseClass())
                .importClass(entityClasses.getDtoRequestClass())
                .importClass(entityClasses.getMapperClass())
                .importClass(entityClasses.getServiceClass())
                .addTo(entityClasses.getControllerDirectory())
                .and(entityClasses::setControllerClass);
        WriteCommandAction.runWriteCommandAction(this.testProject, () -> {
//            this.createUtilsClass(entityClasses);
//            this.createAnnotation();
//            this.createBuilderClass(entityClasses);
        });
    }

    private void createMapperClass(EntityClasses entityClasses) {
        String entityName = entityClasses.getEntityName();
        PsiDirectory mapperDirectory = null == this.containerDirectory.getParent() ? this.containerDirectory : this.psiUtils.getOrCreateSubDirectory(this.containerDirectory.getParent(), "mapper");
        entityClasses.setMapperDirectory(mapperDirectory);
        Optional<PsiClass> entityMapperClassOptional = this.psiUtils.findClass("EntityMapper");
        Consumer<PsiClass> createMapperFunction = (entityMapperClass) -> {
            String mapperName = entityName + "Mapper";
            ClassCreator.of(this.project).init(mapperName,
                            "@Mapper(componentModel = \"spring\")public interface " + mapperName + " extends EntityMapper<" +
                                    entityClasses.getEntityClass().getName() + ", " +
                                    entityClasses.getDtoRequestClass().getName() + ", " +
                                    entityClasses.getDtoResponseClass().getName() +
                                    "> { \n}").importClass("org.mapstruct.Mapper")
                    .importClass(entityClasses.getEntityClass()).importClass(entityClasses.getDtoRequestClass())
                    .importClass(entityClasses.getEntityClass()).importClass(entityClasses.getDtoResponseClass())
                    .importClass("org.mapstruct.Mapping").addTo(mapperDirectory).and((mapperClass) -> {
                        this.psiUtils.importClass(mapperClass, entityClasses.getDtoRequestClass(), entityMapperClass);
                        this.psiUtils.importClass(mapperClass, entityClasses.getDtoResponseClass(), entityMapperClass);

                        entityClasses.setMapperClass(mapperClass);
                        this.createController(entityClasses);
                    });
        };

        if (entityMapperClassOptional.isPresent()) {
            createMapperFunction.accept(entityMapperClassOptional.get());
        } else {
            ClassCreator.of(this.project).init("EntityMapper", EntityMapperClass.getCoreString())
                    .importClass("java.util.List")
                    .addTo(mapperDirectory).and(createMapperFunction);
        }

    }

    private void createClasses(EntityClasses entityClasses) {
        String className = entityClasses.getEntityClassName();
        assert className != null;

        PsiAnnotation[] annotations = entityClasses.getEntityClass().getAnnotations();
        HashMap<String, List<JvmAnnotationAttribute>> tableAnnotations = getTableAnnotations(annotations);
        List<JvmAnnotationAttribute> annotationAttributes = tableAnnotations.get("org.hibernate.annotations.Table");
        String text = "";
        if (annotationAttributes != null) {
            text = ((PsiNameValuePairImpl) annotationAttributes.get(1)).getValue().getText();
        }

        String entityName = className.replace("Entity", "");
        PsiDirectory dtoDirectory = null == this.containerDirectory.getParent() ? this.containerDirectory : this.psiUtils.getOrCreateSubDirectory(this.containerDirectory.getParent(), "dtos");
        String dtoRequest = "\npublic class " + entityName + "RequestDTO" + "{}";
        String dtoResponse = "\npublic class " + entityName + "ResponseDTO" + "{}";

        ClassCreator.of(this.project).init(entityName + "RequestDTO", dtoRequest).copyFields(entityClasses.getEntityClass()).addTo(dtoDirectory).and(entityClasses::setDtoRequestClass);
        ClassCreator.of(this.project).init(entityName + "ResponseDTO", dtoResponse).copyFields(entityClasses.getEntityClass()).addTo(dtoDirectory).and(entityClasses::setDtoResponseClass);
    }

    private HashMap<String, List<JvmAnnotationAttribute>> getTableAnnotations(PsiAnnotation[] annotations) {
        HashMap<String, List<JvmAnnotationAttribute>> annotationMaps = new HashMap<>();
        for (PsiAnnotation annotation : annotations) {
            annotationMaps.put(annotation.getQualifiedName(), annotation.getAttributes());
        }
        return annotationMaps;
    }

}
