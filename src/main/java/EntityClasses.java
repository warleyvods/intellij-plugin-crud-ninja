import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiField;

import java.util.Objects;
import java.util.stream.Stream;


public class EntityClasses {

    private PsiClass entityClass;
    private PsiClass repositoryClass;
    private PsiClass mapperClass;
    private PsiClass dtoClass;
    private PsiClass dtoRequestClass;
    private PsiClass dtoResponseClass;
    private PsiClass serviceClass;
    private PsiClass controllerClass;
    private PsiDirectory serviceDirectory;
    private PsiDirectory mapperDirectory;
    private PsiDirectory controllerDirectory;

    public EntityClasses() {
    }

    public PsiClass getEntityClass() {
        return this.entityClass;
    }

    public EntityClasses setEntityClass(PsiClass entityClass) {
        this.entityClass = entityClass;
        return this;
    }

    public PsiClass getRepositoryClass() {
        return this.repositoryClass;
    }

    EntityClasses setRepositoryClass(PsiClass repositoryClass) {
        this.repositoryClass = repositoryClass;
        return this;
    }

    public PsiClass getMapperClass() {
        return this.mapperClass;
    }

    public void setMapperClass(PsiClass mapperClass) {
        this.mapperClass = mapperClass;
    }

    public PsiClass getDtoClass() {
        return this.dtoClass;
    }

    public PsiClass getDtoRequestClass() {
        return this.dtoRequestClass;
    }

    public PsiClass getDtoResponseClass() {
        return this.dtoResponseClass;
    }

    public EntityClasses setDtoClass(PsiClass dtoClass) {
        this.dtoClass = dtoClass;
        return this;
    }

    public void setDtoRequestClass(PsiClass dtoRequestClass) {
        this.dtoRequestClass = dtoRequestClass;
    }

    public void setDtoResponseClass(PsiClass dtoResponseClass) {
        this.dtoResponseClass = dtoResponseClass;
    }

    public PsiClass getServiceClass() {
        return this.serviceClass;
    }

    public void setServiceClass(PsiClass serviceClass) {
        this.serviceClass = serviceClass;
    }

    public PsiClass getControllerClass() {
        return this.controllerClass;
    }

    public void setControllerClass(PsiClass controllerClass) {
        this.controllerClass = controllerClass;
    }

    public PsiDirectory getServiceDirectory() {
        return this.serviceDirectory;
    }

    public EntityClasses setServiceDirectory(PsiDirectory serviceDirectory) {
        this.serviceDirectory = serviceDirectory;
        return this;
    }

    public String getEntityName() {
        return Objects.requireNonNull(this.getEntityClass().getName()).replace("Entity", "");
    }

    public String getEntityClassName() {
        return this.entityClass.getName();
    }


    public String getIdType() {
        return PsiUtils.getIdType(this.entityClass);
    }

    public PsiDirectory getMapperDirectory() {
        return this.mapperDirectory;
    }

    public void setMapperDirectory(PsiDirectory mapperDirectory) {
        this.mapperDirectory = mapperDirectory;
    }

    public void setControllerDirectory(PsiDirectory controllerDirectory) {
        this.controllerDirectory = controllerDirectory;
    }

    public PsiDirectory getControllerDirectory() {
        return this.controllerDirectory;
    }

    public PsiField getIdField() {
        return Stream.of(this.entityClass.getFields()).filter((psiField) ->
                Stream.of(Objects.requireNonNull(psiField.getModifierList()).getAnnotations()).anyMatch((psiAnnotation) ->
                        psiAnnotation.getText().equals("@Id"))).findFirst().get();
    }

}
