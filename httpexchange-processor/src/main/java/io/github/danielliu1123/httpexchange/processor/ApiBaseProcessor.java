package io.github.danielliu1123.httpexchange.processor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
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
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.javapoet.AnnotationSpec;
import org.springframework.javapoet.JavaFile;
import org.springframework.javapoet.MethodSpec;
import org.springframework.javapoet.ParameterSpec;
import org.springframework.javapoet.TypeName;
import org.springframework.javapoet.TypeSpec;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.function.SingletonSupplier;
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

    private static final String DEFAULT_CLASS_SUFFIX = "Base";
    private static final String CONFIG_FILE_NAME = "httpexchange-processor.properties";
    private static final String DUMMY_FILE_NAME = "httpexchange-processor.tmp";

    private static final AntPathMatcher matcher = new AntPathMatcher(".");
    private final SingletonSupplier<ProcessorProperties> properties = SingletonSupplier.of(this::loadProperties);

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (properties.obtain().enabled()) {
            for (Element element : roundEnv.getRootElements()) {
                // Perhaps just process the interfaces to enhance compile performance?
                processElement(annotations, element);
            }
        }
        return true;
    }

    private ProcessorProperties loadProperties() {
        Properties prop = new Properties();
        try {
            FileObject virtualFile =
                    processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", DUMMY_FILE_NAME);
            String classOutputPath = virtualFile.toUri().getPath().replace(DUMMY_FILE_NAME, "");
            File dir = Finder.findProjectDir(new File(classOutputPath), 20);
            File file = new File(dir, CONFIG_FILE_NAME);
            if (file.exists() && file.isFile()) {
                try (InputStream is = file.toURI().toURL().openStream()) {
                    prop.load(is);
                }
            }
        } catch (IOException ignored) {
            // No-op
        }
        return ProcessorProperties.of(prop);
    }

    private static boolean isInterface(Element element) {
        return element.getKind() == ElementKind.INTERFACE;
    }

    private static boolean isGenericType(Element element) {
        if (element instanceof TypeElement te) {
            return !te.getTypeParameters().isEmpty();
        }
        return false;
    }

    private void processElement(Set<? extends TypeElement> annotations, Element element) {
        if (!isTargetPackage(element)) {
            return;
        }
        if (isInterface(element) && !isGenericType(element)) {
            processAnnotations(annotations, element);
        } else {
            processNonInterfaceElement(annotations, element);
        }
    }

    private boolean isTargetPackage(Element element) {
        List<String> targetPatterns = properties.obtain().packages();
        if (ObjectUtils.isEmpty(targetPatterns)) {
            return true;
        }
        String pkg = processingEnv
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        return targetPatterns.stream().anyMatch(pattern -> matcher.match(pattern, pkg));
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
        String generatedClassName = getGeneratedClassName(element);
        TypeSpec.Builder result = TypeSpec.classBuilder(generatedClassName)
                .addModifiers(Modifier.ABSTRACT)
                .addSuperinterface(TypeName.get(element.asType()))
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", ApiBaseProcessor.class.getName())
                        .addMember("comments", "$S", "Generated by httpexchange-processor, DO NOT modify!")
                        .build())
                .addJavadoc(
                        """
                                Generated default implementation for the server-side.

                                <p>
                                How to use:
                                <pre>{@code
                                @RestController
                                public class $L extends $L {
                                    // ...
                                }
                                }</pre>
                                """,
                        generatedClassName + "Impl",
                        generatedClassName);
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            result.addModifiers(Modifier.PUBLIC);
        }
        return result;
    }

    private String getGeneratedClassName(Element element) {
        String suffix = properties.obtain().suffix();
        String prefix = properties.obtain().prefix();
        boolean hasSuffix = StringUtils.hasText(suffix);
        boolean hasPrefix = StringUtils.hasText(prefix);
        if (!hasPrefix && !hasSuffix) {
            // Use <ClassName>Base as default class name
            return element.getSimpleName() + DEFAULT_CLASS_SUFFIX;
        }
        return (hasPrefix ? prefix : "") + element.getSimpleName() + (hasSuffix ? suffix : "");
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
