//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;


public class ClassCreator {
    private PsiJavaFile javaFile;
    private Project project;
    private PsiUtils psiUtils;

    private ClassCreator(Project project) {
        this.project = project;
        this.psiUtils = PsiUtils.of(project);
    }

    public static ClassCreator of(Project project) {
        return new ClassCreator(project);
    }

    public ClassCreator init(String name, String content) {
        this.javaFile = (PsiJavaFile)PsiFileFactory.getInstance(this.project).createFileFromText(name + ".java", JavaFileType.INSTANCE, content);
        return this;
    }

    ClassCreator importClass(String className) {
        if (StringUtils.isBlank(className))
            return this;
        Objects.requireNonNull(this.javaFile);
        this.psiUtils.findClassGlobal(className, psiClass -> true).ifPresent(this.javaFile::importClass);
        return this;
    }

    ClassCreator importClassIf(String className, Supplier<Boolean> supplier) {
        if (supplier.get()) {
            this.importClass(className);
        }

        return this;
    }

    ClassCreator importClassIf(Supplier<String> nameSupplier, Supplier<Boolean> supplier) {
        if ((Boolean)supplier.get()) {
            this.importClass((String)nameSupplier.get());
        }

        return this;
    }

    ClassCreator importClass(PsiClass psiClass) {
        if (null != psiClass) {
            this.javaFile.importClass(psiClass);
        }
        return this;
    }

    And addTo(PsiDirectory psiDirectory) {
        return new And(((PsiJavaFile)Optional.ofNullable(psiDirectory.findFile(this.javaFile.getName())).orElseGet(() -> {
            this.psiUtils.format(this.javaFile);
            return (PsiJavaFile)psiDirectory.add(this.javaFile);
        })).getClasses()[0]);
    }

    ClassCreator copyFields(PsiClass srcClass) {
        PsiClass aClass = this.javaFile.getClasses()[0];
        PsiElementFactory elementFactory = PsiElementFactory.getInstance(this.project);
        PsiField[] fields = srcClass.getFields();
        PsiMethod constructeur = elementFactory.createConstructor((String)Objects.requireNonNull(aClass.getQualifiedName()));
        aClass.add(constructeur);
        PsiField[] var6 = fields;
        int var7 = fields.length;

        for(int var8 = 0; var8 < var7; ++var8) {
            PsiField field = var6[var8];
            String name = field.getName();

            assert name != null;

            PsiType type = field.getType();
            this.psiUtils.findClass(type.getCanonicalText()).ifPresent((typeClass) -> {
                this.psiUtils.importClass(aClass, new PsiClass[]{typeClass});
            });
            String typeName = type.getPresentableText();
            if (typeName.contains(".")) {
                typeName = typeName.substring(typeName.lastIndexOf(".") + 1);
            }

            PsiAnnotation psiAnnotation = field.getAnnotation("javax.persistence.Column");
            StringBuilder annotationStringBuilder = new StringBuilder();
            if (typeName.equals("LocalDateTime")) {
                annotationStringBuilder.append("@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)");
            }

            // StringUtils.equals compatible idea 193 version
	        if (StringUtils.equals(typeName,"Date") || StringUtils.equals(typeName,"LocalDate")) {
		        annotationStringBuilder.append("@CheckDate ");
		        this.importClass("CheckDate");
	        }
	        if (StringUtils.containsIgnoreCase(name,"idCard")) {
		        annotationStringBuilder.append("@CheckIdCard ");
		        this.importClass("CheckIdCard");
	        }
	        if (StringUtils.containsIgnoreCase(name,"mail")) {
		        annotationStringBuilder.append("@CheckEmail ");
		        this.importClass("CheckEmail");
	        }
	        if (StringUtils.containsIgnoreCase(name,"phone") || StringUtils.containsIgnoreCase(name,"mobile")) {
		        annotationStringBuilder.append("@CheckMobile ");
		        this.importClass("CheckMobile");
	        }

	        if (null != psiAnnotation) {
		        PsiAnnotationMemberValue memberLengthValue = psiAnnotation.findAttributeValue("length");
		        PsiAnnotationMemberValue nullableValue = psiAnnotation.findAttributeValue("nullable");
		        PsiAnnotationMemberValue columnDefinition = psiAnnotation.findAttributeValue("columnDefinition");

		        if (null != memberLengthValue && "String".equals(typeName) ) {
			        int length = Integer.parseInt(memberLengthValue.getText());
			        annotationStringBuilder.append("@Size(max = ").append(length).append(") ");
			        this.importClass("javax.validation.constraints.Size");
		        }else if("Integer".equals(typeName) ) {
			        annotationStringBuilder.append("@Max(Integer.MAX_VALUE) ");
			        this.importClass("javax.validation.constraints.Size");
		        }

		        if ("false".equals(nullableValue.getText())){
			        if ("String".equals(typeName) && !name.equalsIgnoreCase("id")) {
				        annotationStringBuilder.append("@NotBlank ");
				        this.importClass("javax.validation.constraints.NotBlank");
			        }else if(StringUtils.containsAny(typeName,"List","Map","Set") ){
				        annotationStringBuilder.append("@NotEmpty ");
				        this.importClass("javax.validation.constraints.NotEmpty");
			        }  else if (!name.equalsIgnoreCase("id")){
				        annotationStringBuilder.append("@NotNull ");
				        this.importClass("javax.validation.constraints.NotNull");
			        }
		        }

		        String comment = getComment(columnDefinition.getText());
		        if (StringUtils.isNotBlank(comment)){
			        annotationStringBuilder.append("@ApiModelProperty(\""+comment+"\")\n ");
			        this.importClass("io.swagger.annotations.ApiModelProperty;");
		        }
	        }

	        PsiField cField = elementFactory.createFieldFromText(annotationStringBuilder.toString() + "private " + typeName + " " + name + ";", (PsiElement)null);
            aClass.add(cField);
            PsiMethod normalSetter = elementFactory.createMethodFromText(this.createSetter(name, type.getCanonicalText()), field);
            PsiMethod getter = elementFactory.createMethodFromText(this.createGetter(name, type.getCanonicalText()), field);
            if (0 == aClass.findMethodsByName(normalSetter.getName()).length) {
                aClass.add(normalSetter);
            }

            if (0 == aClass.findMethodsByName(getter.getName()).length) {
                aClass.add(getter);
            }
        }

        return this;
    }

	private PsiMethod getConstructeurWithParam(PsiElementFactory psiElementFactory, PsiClass aClass, PsiField[] fields) {
        PsiMethod constructor = psiElementFactory.createConstructor(aClass.getQualifiedName());
        constructor.getModifierList().setModifierProperty("public", true);
        Stream.of(fields).forEach((psiField) -> {
            PsiParameter builderParameter = psiElementFactory.createParameter(psiField.getName(), psiField.getType());
            constructor.getParameterList().add(builderParameter);
        });
        return constructor;
    }

    private String createBuilderSetter(String className, String name, String type) {
        return "public " + className + " " + name + "(" + type + " " + name + ") {this." + name + " = " + name + ";return this;}";
    }

    private String createSetter(@NotNull String name, String type) {

        String var10000 = name.substring(0, 1).toUpperCase();
        return "public void set" + var10000 + name.substring(1) + "(" + type + " " + name + ") {this." + name + " = " + name + ";}";
    }

    private String createGetter(String name, String type) {
        return "public " + type + " get" + name.substring(0, 1).toUpperCase() + name.substring(1) + "() {return this." + name + ";}";
    }

    private String getComment(String columnDefinition) {
	    String pattern = "(?<=COMMENT )\\S+";
	    Pattern r = Pattern.compile(pattern);
	    Matcher m = r.matcher(columnDefinition);
	    if (m.find()){
		    return m.group(0).replace("'","").replace("\"","");
	    }
	    return "";
    }

    public static class And {
        private PsiClass psiClass;

        public And(PsiClass psiClass) {
            this.psiClass = psiClass;
        }

        public void and(Consumer<PsiClass> callback) {
            callback.accept(this.psiClass);
        }
    }
}
