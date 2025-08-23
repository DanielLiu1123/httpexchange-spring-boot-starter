package io.github.danielliu1123.httpexchange.processor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
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
    "org.springframework.web.bind.annotation.RequestMapping",
    "org.springframework.web.bind.annotation.GetMapping",
    "org.springframework.web.bind.annotation.PostMapping",
    "org.springframework.web.bind.annotation.PutMapping",
    "org.springframework.web.bind.annotation.DeleteMapping",
    "org.springframework.web.bind.annotation.PatchMapping"
})
@SupportedOptions({ApiBaseProcessor.configOptionName})
public class ApiBaseProcessor extends AbstractProcessor {

    private static final String DEFAULT_CLASS_SUFFIX = "Base";

    // Auto-detect the config file
    private static final String CONFIG_FILE_NAME = "httpexchange-processor.properties";
    private static final String DUMMY_FILE_NAME = "httpexchange-processor.tmp";

    // -AhttpExchangeConfig=${projectDir}/httpexchange-processor.properties
    static final String configOptionName = "httpExchangeConfig";

    private static final AntPathMatcher matcher = new AntPathMatcher(".");

    private ProcessorProperties properties;
    private final Set<String> generatedClasses = ConcurrentHashMap.newKeySet();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);

        properties = loadProperties(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (properties.enabled()) {
            for (Element element : roundEnv.getRootElements()) {
                if (!isGeneratedClass(element)) {
                    processElement(annotations, element);
                }
            }
        }
        return true;
    }

    private boolean isGeneratedClass(Element element) {
        if (element instanceof TypeElement typeElement) {
            return generatedClasses.contains(typeElement.getQualifiedName().toString());
        }
        return false;
    }

    private static ProcessorProperties loadProperties(ProcessingEnvironment processingEnv) {

        var cfg = processingEnv.getOptions().get(configOptionName);

        var prop = StringUtils.hasText(cfg)
                ? buildFromConfig(processingEnv, cfg) // incremental compilation friendly
                : buildFromProcessingEnv(processingEnv);

        return ProcessorProperties.from(prop);
    }

    private static Properties buildFromConfig(ProcessingEnvironment processingEnv, String cfg) {
        var result = new Properties();
        var file = new File(cfg);
        if (!file.exists()) {
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.WARNING, "[http-exchange processor] Config file not found: " + cfg);
            return result;
        }
        if (file.isDirectory()) {
            processingEnv
                    .getMessager()
                    .printMessage(
                            Diagnostic.Kind.WARNING, "[http-exchange processor] Config file is a directory: " + cfg);
            return result;
        }
        try (var is = file.toURI().toURL().openStream()) {
            result.load(is);
        } catch (IOException ignored) {
            // No-op
        }
        return result;
    }

    private static Properties buildFromProcessingEnv(ProcessingEnvironment processingEnv) {
        Properties prop = new Properties();
        FileObject tempFile = null;
        try {
            tempFile = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", DUMMY_FILE_NAME);
            String classOutputPath = tempFile.toUri().getPath().replace(DUMMY_FILE_NAME, "");
            File configFile = Finder.findFile(new File(classOutputPath), CONFIG_FILE_NAME);
            if (configFile != null) {
                try (InputStream is = configFile.toURI().toURL().openStream()) {
                    prop.load(is);
                }
            }
        } catch (IOException ignored) {
            // No-op
        } finally {
            if (tempFile != null) {
                tempFile.delete();
            }
        }
        return prop;
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
        List<String> targetPatterns = properties.packages();
        if (ObjectUtils.isEmpty(targetPatterns)) {
            return true;
        }
        String pkg = processingEnv
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        return targetPatterns.stream().anyMatch(pattern -> pkg.startsWith(pattern) || matcher.match(pattern, pkg));
    }

    private void processNonInterfaceElement(Set<? extends TypeElement> annotations, Element element) {
        for (Element enclosedElement : element.getEnclosedElements()) {
            processElement(annotations, enclosedElement);
        }
    }

    private void processAnnotations(Set<? extends TypeElement> annotations, Element element) {
        TypeSpec.Builder classBuilder = getTypeBuilder(element);
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

    private TypeSpec.Builder getTypeBuilder(Element element) {
        return switch (properties.generatedType()) {
            case INTERFACE:
                yield createInterfaceBuilder(element);
            case ABSTRACT_CLASS:
                yield createClassBuilder(element);
        };
    }

    private TypeSpec.Builder createInterfaceBuilder(Element element) {
        String generatedClassName = getGeneratedClassName(element);
        TypeSpec.Builder result = TypeSpec.interfaceBuilder(generatedClassName)
                .addAnnotation(AnnotationSpec.builder(Generated.class)
                        .addMember("value", "$S", ApiBaseProcessor.class.getName())
                        .build())
                .addJavadoc(
                        """
                                Generated default implementation for the server-side.

                                <p>
                                How to use:
                                <pre>{@code
                                @RestController
                                public class $L implements $L {
                                    // ...
                                }
                                }</pre>
                                """,
                        element.getSimpleName().toString() + "Impl",
                        generatedClassName);
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            result.addModifiers(Modifier.PUBLIC);
        }

        for (AnnotationMirror annotationMirror : element.getAnnotationMirrors()) {
            result.addAnnotation(AnnotationSpec.get(annotationMirror));
        }

        // Incremental compilation (Isolating) must have exactly one originating element.
        // See https://docs.gradle.org/current/userguide/java_plugin.html#isolating_annotation_processors
        result.addOriginatingElement(element);

        return result;
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
                        element.getSimpleName().toString() + "Impl",
                        generatedClassName);
        if (element.getModifiers().contains(Modifier.PUBLIC)) {
            result.addModifiers(Modifier.PUBLIC);
        }

        // Incremental compilation (Isolating) must have exactly one originating element.
        // See https://docs.gradle.org/current/userguide/java_plugin.html#isolating_annotation_processors
        result.addOriginatingElement(element);

        return result;
    }

    private String getGeneratedClassName(Element element) {
        String suffix = properties.suffix();
        String prefix = properties.prefix();
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
                classBuilder.addMethod(buildMethodSpec((ExecutableElement) enclosedElement));
                return true;
            }
        }
        return false;
    }

    private boolean isAnnotationMatched(Set<? extends TypeElement> annotations, AnnotationMirror annotation) {
        for (TypeElement anno : annotations) {
            if (Objects.equals(
                    anno.getQualifiedName().toString(),
                    annotation.getAnnotationType().toString())) {
                return true;
            }
        }
        return false;
    }

    private MethodSpec buildMethodSpec(ExecutableElement methodElement) {
        return switch (properties.generatedType()) {
            case INTERFACE:
                yield buildInterfaceMethodSpec(methodElement);
            case ABSTRACT_CLASS:
                yield buildAbstractClassMethodSpec(methodElement);
        };
    }

    private MethodSpec buildInterfaceMethodSpec(ExecutableElement methodElement) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                        methodElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .returns(TypeName.get(methodElement.getReturnType()))
                .addStatement("throw new $T($T.NOT_IMPLEMENTED)", ResponseStatusException.class, HttpStatus.class)
                .addJavadoc(
                        """
                                $L
                                @see $L#$L($L)
                                """,
                        Optional.ofNullable(processingEnv.getElementUtils().getDocComment(methodElement))
                                .orElse("")
                                .stripTrailing(),
                        ((TypeElement) methodElement.getEnclosingElement())
                                .getQualifiedName()
                                .toString(),
                        methodElement.getSimpleName().toString(),
                        getParameterTypes(methodElement));

        for (VariableElement parameter : methodElement.getParameters()) {
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(
                    TypeName.get(parameter.asType()), parameter.getSimpleName().toString());

            for (AnnotationMirror annotationMirror : parameter.getAnnotationMirrors()) {
                parameterBuilder.addAnnotation(AnnotationSpec.get(annotationMirror));
            }

            methodBuilder.addParameter(parameterBuilder.build());
        }

        for (AnnotationMirror annotationMirror : methodElement.getAnnotationMirrors()) {
            methodBuilder.addAnnotation(AnnotationSpec.get(annotationMirror));
        }

        return methodBuilder.build();
    }

    private static String getParameterTypes(ExecutableElement methodElement) {
        return methodElement.getParameters().stream()
                .map(parameter -> {
                    TypeMirror typeMirror = parameter.asType();
                    if (typeMirror instanceof DeclaredType declaredType) {
                        TypeElement typeElement = (TypeElement) declaredType.asElement();
                        return typeElement.getQualifiedName().toString();
                    } else {
                        return typeMirror.toString();
                    }
                })
                .reduce((p1, p2) -> p1 + ", " + p2)
                .orElse("");
    }

    private MethodSpec buildAbstractClassMethodSpec(ExecutableElement methodElement) {
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(
                        methodElement.getSimpleName().toString())
                .addModifiers(Modifier.PUBLIC)
                .returns(TypeName.get(methodElement.getReturnType()))
                .addStatement("throw new $T($T.NOT_IMPLEMENTED)", ResponseStatusException.class, HttpStatus.class)
                .addAnnotation(Override.class);

        for (VariableElement parameter : methodElement.getParameters()) {
            ParameterSpec.Builder parameterBuilder = ParameterSpec.builder(
                    TypeName.get(parameter.asType()), parameter.getSimpleName().toString());

            methodBuilder.addParameter(parameterBuilder.build());
        }

        return methodBuilder.build();
    }

    @SneakyThrows
    private void generateJavaFile(Element element, TypeSpec.Builder classBuilder) {
        JavaFile javaFile = JavaFile.builder(getOutputPackage(element), classBuilder.build())
                .build();
        javaFile.writeTo(processingEnv.getFiler());
        generatedClasses.add(
                StringUtils.hasText(javaFile.packageName())
                        ? javaFile.packageName() + "." + javaFile.typeSpec().name()
                        : javaFile.typeSpec().name());
    }

    private String getOutputPackage(Element element) {
        String originalPackage = processingEnv
                .getElementUtils()
                .getPackageOf(element)
                .getQualifiedName()
                .toString();
        String outputSubpackage =
                Optional.ofNullable(properties.outputSubpackage()).orElse("");
        if (!StringUtils.hasText(originalPackage)) {
            return outputSubpackage;
        }
        return StringUtils.hasText(outputSubpackage) ? originalPackage + "." + outputSubpackage : originalPackage;
    }
}
