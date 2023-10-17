package com.freemanan.starter.httpexchange.apt;

import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.web.server.ResponseStatusException;

/**
 * @author Freeman
 */
@SupportedSourceVersion(SourceVersion.RELEASE_17)
@SupportedAnnotationTypes({
    "org.springframework.web.service.annotation.HttpExchange",
    "org.springframework.web.service.annotation.GetExchange",
    "org.springframework.web.service.annotation.PostExchange",
    "org.springframework.web.service.annotation.PutExchange",
    "org.springframework.web.service.annotation.DeleteExchange",
    "org.springframework.web.service.annotation.PatchExchange",
})
public class ApiBaseProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getRootElements()) {
            // TODO(Freeman): perhaps we just need to process the interfaces to enhance compile performance?
            processElement(annotations, element);
        }
        return true;
    }

    private boolean isInterface(Element element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    private void processElement(Set<? extends TypeElement> annotations, Element element) {
        if (isInterface(element)) {
            processAnnotations(annotations, element);
        } else {
            processNonInterfaceElement(annotations, element);
        }
    }

    private void processNonInterfaceElement(Set<? extends TypeElement> annotations, Element element) {
        for (Element enclosedElement : element.getEnclosedElements()) {
            processElement(annotations, enclosedElement);
        }
    }

    private void processAnnotations(Set<? extends TypeElement> annotations, Element element) {
        TypeSpec.Builder classBuilder = createClassBuilder(element);
        boolean isNeedGenerateJavaFile = hasAnnotationMatched(annotations, element);

        for (Element enclosedElement : element.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                isNeedGenerateJavaFile =
                        processMethodElement(annotations, classBuilder, enclosedElement) || isNeedGenerateJavaFile;
            } else if (enclosedElement.getKind() == ElementKind.INTERFACE) {
                processElement(annotations, enclosedElement);
            }
        }

        if (isNeedGenerateJavaFile) {
            generateJavaFile(element, classBuilder);
        }
    }

    private boolean hasAnnotationMatched(Set<? extends TypeElement> annotations, Element element) {
        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            if (isAnnotationMatched(annotations, annotationMirror)) {
                return true;
            }
        }
        return false;
    }

    private TypeSpec.Builder createClassBuilder(Element element) {
        TypeSpec.Builder result = TypeSpec.classBuilder(element.getSimpleName() + "Base")
                .addModifiers(Modifier.ABSTRACT)
                .addSuperinterface(TypeName.get(element.asType()))
                .addJavadoc(
                        """
                                Server side base implementation.

                                <p>
                                Usage:
                                <pre>{@code
                                @RestController
                                public class $L extends $L {
                                    // ...
                                }
                                }</pre>
                                """,
                        element.getSimpleName() + "Controller",
                        element.getSimpleName() + "Base");
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            result.addModifiers(Modifier.PUBLIC);
        }
        return result;
    }

    private boolean processMethodElement(
            Set<? extends TypeElement> annotations, TypeSpec.Builder classBuilder, Element enclosedElement) {
        for (AnnotationMirror annotation : enclosedElement.getAnnotationMirrors()) {
            if (!enclosedElement.getModifiers().contains(Modifier.DEFAULT)
                    && isAnnotationMatched(annotations, annotation)) {
                MethodSpec methodSpec = buildMethodSpec((ExecutableElement) enclosedElement);
                classBuilder.addMethod(methodSpec);
                return true;
            }
        }
        return false;
    }

    private boolean isAnnotationMatched(Set<? extends TypeElement> annotations, AnnotationMirror annotation) {
        return annotations.stream().anyMatch(a -> a.getQualifiedName()
                .toString()
                .equals(annotation.getAnnotationType().toString()));
    }

    private MethodSpec buildMethodSpec(ExecutableElement methodElement) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                        methodElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(methodElement.getReturnType()))
                .addAnnotation(Override.class);

        addParametersToMethodBuilder(methodElement, methodBuilder);
        methodBuilder.addStatement("throw new $T($T.NOT_IMPLEMENTED)", ResponseStatusException.class, HttpStatus.class);

        return methodBuilder.build();
    }

    private void addParametersToMethodBuilder(ExecutableElement methodElement, MethodSpec.Builder methodBuilder) {
        for (VariableElement parameter : methodElement.getParameters()) {
            methodBuilder.addParameter(ParameterSpec.builder(
                            TypeName.get(parameter.asType()),
                            parameter.getSimpleName().toString())
                    .build());
        }
    }

    @SneakyThrows
    private void generateJavaFile(Element element, TypeSpec.Builder classBuilder) {
        String originalPackageName = processingEnv
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        JavaFile javaFile =
                JavaFile.builder(originalPackageName, classBuilder.build()).build();
        javaFile.writeTo(processingEnv.getFiler());
    }
}