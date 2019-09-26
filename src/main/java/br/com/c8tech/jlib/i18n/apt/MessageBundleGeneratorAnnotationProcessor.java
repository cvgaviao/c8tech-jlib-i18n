/**
 * ============================================================================
 *  Copyright ©  2015-2019,    Cristiano V. Gavião
 *
 *  All rights reserved.
 *  This program and the accompanying materials are made available under
 *  the terms of the Eclipse Public License v1.0 which accompanies this
 *  distribution and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * ============================================================================
 */
package br.com.c8tech.jlib.i18n.apt;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.processing.Processor;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.jooq.lambda.tuple.Tuple;
import org.jooq.lambda.tuple.Tuple2;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeSpec.Builder;

import br.com.c8tech.jlib.i18n.AbstractMessageBundle;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedOptions({ "Properties.baseName", "Properties.basePath" })
public class MessageBundleGeneratorAnnotationProcessor extends
        AbstractHierarchyAnnotationProcessor<MessageBundleMethodDescriptor, MessageBundleDescriptor<MessageBundleMethodDescriptor>> {

    private static final String METHOD_PARAMETERS_REGEX = "(\\{(\\d)\\})";

    private static final Pattern methodParameterFinderPattern = Pattern
            .compile(METHOD_PARAMETERS_REGEX);

    @Override
    protected Set<ElementKind> allowedTypeKinds() {
        return Set.of(ElementKind.INTERFACE);
    }

    private String computePropertiesFileName(String pLocale,
            String pPropertiesBasePath, String pPropertiesBaseName) {
        return pPropertiesBasePath.concat("/").concat(pPropertiesBaseName)
                .concat("_").concat(pLocale).concat(".properties");
    }

    @Override
    protected MessageBundleMethodDescriptor createMethodAnnotatedDescriptor(
            Map<String, Object> pAnnotationValues,
            TypeAnnotatedMethodDescriptor pTypeAnnotatedMethodDescriptor) {

        String locale = null;
        String message = "";
        String key = null;

        if (pTypeAnnotatedMethodDescriptor.annotationName()
                .equals(Message.class.getCanonicalName())) {

            locale = (String) pAnnotationValues.get("locale");
            message = (String) pAnnotationValues.get("value");
            key = (String) pAnnotationValues.get("key");
            if (key == null || key.isEmpty()) {
                key = pTypeAnnotatedMethodDescriptor.name();
            }
        }

        return MessageBundleMethodDescriptor.builder()
                .from(pTypeAnnotatedMethodDescriptor).message(message).key(key)
                .locale(locale).build();
    }

    @Override
    protected MessageBundleDescriptor<MessageBundleMethodDescriptor> createTypeAnnotatedDescriptor(
            TypeAnnotatedDescriptor<MessageBundleMethodDescriptor> pTypeAnnotatedDescriptor) {

        String basename = processingEnv.getOptions().getOrDefault(
                "Properties.baseName", pTypeAnnotatedDescriptor.simpleName());
        String basePath = processingEnv.getOptions()
                .getOrDefault("Properties.basePath", "META-INF/i18n");

        return MessageBundleDescriptor.builder().propertiesBaseName(basename)
                .propertiesBasePath(basePath)
                .annotationClass(pTypeAnnotatedDescriptor.annotationClass())
                .simpleName(pTypeAnnotatedDescriptor.simpleName())
                .packageName(pTypeAnnotatedDescriptor.packageName())
                .qualifiedName(pTypeAnnotatedDescriptor.qualifiedName())
                .targetType(pTypeAnnotatedDescriptor.targetType())
                .methodDescriptors(pTypeAnnotatedDescriptor.methodDescriptors())
                .build();
    }

    private boolean generateMessageBundleImplementationClass(
            MessageBundleDescriptor<MessageBundleMethodDescriptor> pMessageBundleDescriptor) {

        Builder classBuilder = TypeSpec
                .classBuilder(pMessageBundleDescriptor.simpleName() + "Impl")
                .superclass(AbstractMessageBundle.class)
                .addSuperinterface(TypeName
                        .get(pMessageBundleDescriptor.targetType().asType()))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        for (MessageBundleMethodDescriptor methodDescriptor : pMessageBundleDescriptor
                .methodDescriptors()) {

            List<ParameterSpec> parameters = new ArrayList<>(
                    methodDescriptor.qualifiedParameterTypes().size());
            for (VariableElement parameter : methodDescriptor
                    .qualifiedParameterTypes()) {
                ParameterSpec parameterSpec = ParameterSpec
                        .builder(TypeName.get(parameter.asType()),
                                parameter.getSimpleName().toString())
                        .build();
                parameters.add(parameterSpec);
            }

            MethodSpec method = MethodSpec
                    .methodBuilder(methodDescriptor.name())
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .returns(TypeName
                            .get(methodDescriptor.qualifiedReturnType()))
                    .addParameters(parameters)
                    .addStatement(" return $S", "Hello, JavaPoet!").build();

            classBuilder.addMethod(method);
        }

        JavaFile javaFile = JavaFile
                .builder(pMessageBundleDescriptor.packageName(),
                        classBuilder.build())
                .build();

        try {
            javaFile.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            return error(
                    "Failure while saving file: pMessageBundleDescriptor.absolutePath()",
                    pMessageBundleDescriptor.targetType(), e);
        }

        return true;
    }

    private boolean generateMessageBundleProperties(
            MessageBundleDescriptor<MessageBundleMethodDescriptor> pMessageBundleDescriptor) {

        // group the tagged methods by locale, so we can generate one Properties
        // file per locale.
        Map<String, List<MessageBundleMethodDescriptor>> group = pMessageBundleDescriptor
                .methodDescriptors().stream().collect(Collectors
                        .groupingBy(MessageBundleMethodDescriptor::locale));

        for (Entry<String, List<MessageBundleMethodDescriptor>> entry : group
                .entrySet()) {

            if (!generateMessageBundlePropertiesByLocale(entry.getKey(),
                    pMessageBundleDescriptor, entry.getValue())) {
                return false;
            }
        }

        return info("All properties files was sucessfully generated. ",
                pMessageBundleDescriptor.targetType());
    }

    private boolean generateMessageBundlePropertiesByLocale(String pLocale,
            MessageBundleDescriptor<MessageBundleMethodDescriptor> pMessageBundleDescriptor,
            List<MessageBundleMethodDescriptor> pMessageBundleMethodDescriptors) {
        try {

            String resourceName = computePropertiesFileName(pLocale,
                    pMessageBundleDescriptor.getPropertiesBasePath(),
                    pMessageBundleDescriptor.getPropertiesBaseName());

            List<Element> methods = new ArrayList<>();
            Properties properties = new Properties(
                    pMessageBundleMethodDescriptors.size());
            for (MessageBundleMethodDescriptor methodDescriptor : pMessageBundleMethodDescriptors) {
                properties.put(methodDescriptor.key(),
                        methodDescriptor.message());
                methods.add(methodDescriptor.methodElement());
            }

            FileObject f = processingEnv.getFiler().createResource(
                    StandardLocation.CLASS_OUTPUT, "", resourceName,
                    methods.stream().toArray(Element[]::new));

            properties.store(f.openOutputStream(),
                    "File Generated by Annotation Processing");
            return info(
                    "Properties file for locale '" + pLocale
                            + " was sucessfully generated at '"
                            + f.toUri().getPath() + "'",
                    pMessageBundleDescriptor.targetType());
        } catch (IOException e) {
            return error(
                    "An error occurred while trying to save a properties file for '"
                            + pMessageBundleDescriptor.getPropertiesBaseName(),
                    pMessageBundleDescriptor.targetType(), e);
        }

    }

    @Override
    protected Set<Tuple2<Class<? extends Annotation>, Class<? extends Annotation>>> getChildrenAnnotationTypes() {
        return Set.of(Tuple.tuple(Message.class, Messages.class));
    }

    @Override
    protected Class<? extends Annotation> getParentAnnotationType() {
        return MessageBundle.class;
    }

    @Override
    protected boolean isValidCandidate(
            MessageBundleMethodDescriptor pMethodDescriptor,
            ExecutableElement pMethodElement) {

        boolean result = true;

        // check the number of parameter of both message and method

        if (!parameterMatched(pMethodDescriptor.message(),
                pMethodDescriptor.qualifiedParameterTypes().size(),
                pMethodDescriptor, pMethodElement))
            return false;

        if (!isAssignable(pMethodDescriptor.qualifiedReturnType(), String.class)
                && !isAssignable(pMethodDescriptor.qualifiedReturnType(),
                        Throwable.class)) {
            error("method annotated with " + pMethodDescriptor.annotationName()
                    + " must return java.lang.String or a subclass of java.lang.Throwable",
                    pMethodElement);
            result = false;
        }

        return result;

    }

    private boolean parameterMatched(String pMessage, int pMethodCount,
            MessageBundleMethodDescriptor pMethodDescriptor,
            ExecutableElement pMethodElement) {
        Matcher m = methodParameterFinderPattern.matcher(pMessage);
        int count = 0;
        while (m.find())
            count++;
        if (pMethodCount == count) {
            return true;
        } else {
            error("Methods annotated with " + pMethodDescriptor.annotationName()
                    + " must have number of parameters equal to the number of placeholders {d} in message.",
                    pMethodElement);
            return false;
        }
    }

    @Override
    protected boolean processMessageBundleDescriptor(
            MessageBundleDescriptor<MessageBundleMethodDescriptor> pMessageBundleDescriptor) {
        boolean result = true;

        // generate the properties file, one for each locale extracted from the
        // message tagged methods.
        if (generateMessageBundleProperties(pMessageBundleDescriptor)) {

            if (!generateMessageBundleImplementationClass(
                    pMessageBundleDescriptor)) {
                result = error(
                        "Failure occurred while generating the ResourceBundle access implementation.",
                        pMessageBundleDescriptor.targetType());
            }
        } else
            result = error(
                    "Failure occurred while generating the ResourceBundle locale properties files.",
                    pMessageBundleDescriptor.targetType());

        return result;
    }

}
