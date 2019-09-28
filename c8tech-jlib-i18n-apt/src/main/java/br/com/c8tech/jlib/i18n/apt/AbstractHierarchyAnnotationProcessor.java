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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

import org.jooq.lambda.tuple.Tuple2;

public abstract class AbstractHierarchyAnnotationProcessor<M extends TypeAnnotatedMethodDescriptor, T extends TypeAnnotatedDescriptor<M>>
        extends AbstractProcessor {
    boolean processed;

    protected Set<ElementKind> allowedTypeKinds() {
        return Set.of(ElementKind.INTERFACE, ElementKind.CLASS);
    }

    private Set<M> collectMethodDescriptorForAnnotation(
            ExecutableElement pMethodElement,
            Class<? extends Annotation> pAnnotation) {

        Set<M> descriptorsPerAnnotation = new HashSet<>(1);

        Optional<Map<? extends ExecutableElement, ? extends AnnotationValue>> rawValuesOpt = filterMethodAnnotations(
                pMethodElement, pAnnotation);
        if (rawValuesOpt.isPresent()) {
            Map<String, Object> values = rawValuesOpt.get().entrySet().stream()
                    .collect(Collectors.toMap(
                            ek -> ek.getKey().getSimpleName().toString(),
                            ev -> ev.getValue().getValue(), (a, b) -> a,
                            TreeMap::new));
            M methodDescr = createMethodAnnotatedDescriptor(values,
                    TypeAnnotatedMethodDescriptor.builder()
                            .annotationName(pAnnotation.getCanonicalName())
                            .methodElement(pMethodElement)
                            .name(pMethodElement.getSimpleName().toString())
                            .qualifiedReturnType(pMethodElement.getReturnType())
                            .qualifiedParameterTypes(
                                    pMethodElement.getParameters())
                            .build());
            if (!isValidMethodCandidate(methodDescr, pMethodElement)) {
                throw new AnnotationProcessException(
                        "The method annotated with " + pAnnotation.getName()
                                + "is not valid");
            }
            descriptorsPerAnnotation.add(methodDescr);
        }
        return descriptorsPerAnnotation;

    }

    private Set<M> collectMethodDescriptorsForAnnotationContainer(
            ExecutableElement pMethodElement,
            Class<? extends Annotation> pContainerAnnotation,
            Class<? extends Annotation> pAnnotation) {
        Set<M> descriptorsPerAnnotation = new HashSet<>();

        Optional<AnnotationValue> rawValueOpt = filterMethodAnnotations(
                pMethodElement, pContainerAnnotation)
                        .flatMap(a -> a.values().stream().findFirst());
        if (rawValueOpt.isPresent()
                && rawValueOpt.get().getValue() instanceof List) {

            List<AnnotationValue> anns = ((List<?>) rawValueOpt.get()
                    .getValue())
                            .stream()
                            .filter(o -> AnnotationValue.class
                                    .isAssignableFrom(o.getClass()))
                            .map(o -> (AnnotationValue) o)
                            .collect(Collectors.toList());

            anns.stream()
                    .forEach(ann -> descriptorsPerAnnotation
                            .add(extractMethodDescriptors(pMethodElement, ann,
                                    pAnnotation)));

        }
        return descriptorsPerAnnotation;
    }

    private T collectTypeAnnotateDescriptor(TypeElement pTargetElement,
            Class<? extends Annotation> pAnnotationClass,
            Set<M> pMethodDescriptors) {

        String packageName = ((PackageElement) pTargetElement
                .getEnclosingElement()).getQualifiedName().toString();
        String qualifiedName = pTargetElement.getQualifiedName().toString();
        String simpleName = pTargetElement.getSimpleName().toString();

        return createTypeAnnotatedDescriptor(TypeAnnotatedDescriptor
                .<M> builder().annotationClass(pAnnotationClass)
                .methodDescriptors(pMethodDescriptors).packageName(packageName)
                .qualifiedName(qualifiedName).simpleName(simpleName)
                .targetType(pTargetElement).build());
    }

    protected abstract M createMethodAnnotatedDescriptor(
            Map<String, Object> pAnnotationValues,
            TypeAnnotatedMethodDescriptor pTypeAnnotatedMethodDescriptor);

    protected abstract T createTypeAnnotatedDescriptor(
            TypeAnnotatedDescriptor<M> pTypeAnnotatedDescriptor);

    protected boolean error(String pErrorMessage, Element pElement) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                pErrorMessage, pElement);
        return false;
    }

    protected boolean error(String pErrorMessage, Element pElement,
            Throwable pThrowable) {
        StringWriter sw = new StringWriter();
        sw.append(pErrorMessage);
        sw.append('\n');
        pThrowable.printStackTrace(new PrintWriter(sw));
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                sw.getBuffer(), pElement);

        return false;
    }

    protected boolean error(String pErrorMessage, Throwable pThrowable) {
        StringWriter sw = new StringWriter();
        sw.append(pErrorMessage);
        sw.append('\n');
        pThrowable.printStackTrace(new PrintWriter(sw));
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                sw.getBuffer());
        return false;
    }

    private M extractMethodDescriptors(ExecutableElement pMethodElement,
            AnnotationValue pAnnotationValue,
            Class<? extends Annotation> pAnnotation) {

        Map<? extends ExecutableElement, ? extends AnnotationValue> rawValuesOpt = processingEnv
                .getElementUtils().getElementValuesWithDefaults(
                        (AnnotationMirror) pAnnotationValue.getValue());

        Map<String, Object> values = rawValuesOpt.entrySet().stream()
                .collect(Collectors.toMap(
                        ek -> ek.getKey().getSimpleName().toString(),
                        ev -> ev.getValue().getValue(), (a, b) -> a,
                        TreeMap::new));

        M methodDescr = createMethodAnnotatedDescriptor(values,
                TypeAnnotatedMethodDescriptor.builder()
                        .annotationName(pAnnotation.getCanonicalName())
                        .methodElement(pMethodElement)
                        .name(pMethodElement.getSimpleName().toString())
                        .qualifiedReturnType(pMethodElement.getReturnType())
                        .qualifiedParameterTypes(pMethodElement.getParameters())
                        .build());

        if (!isValidMethodCandidate(methodDescr, pMethodElement)) {
            throw new AnnotationProcessException("The method annotated with "
                    + pAnnotation.getName() + "is not valid");
        } else {
            return methodDescr;
        }

    }

    private Optional<Map<? extends ExecutableElement, ? extends AnnotationValue>> filterMethodAnnotations(
            ExecutableElement pMethodElement,
            Class<? extends Annotation> pAnnotation) {

        TypeElement anType = processingEnv.getElementUtils()
                .getTypeElement(pAnnotation.getCanonicalName());

        return pMethodElement.getAnnotationMirrors().stream().filter(
                a -> a.getAnnotationType().toString().equals(anType.toString()))
                .findFirst().map(ann -> processingEnv.getElementUtils()
                        .getElementValuesWithDefaults(ann));
    }

    protected abstract Set<Tuple2<Class<? extends Annotation>, Class<? extends Annotation>>> getChildrenAnnotationTypes();

    protected abstract Class<? extends Annotation> getParentAnnotationType();

    @Override
    public final Set<String> getSupportedAnnotationTypes() {
        String parentAnnotation = getParentAnnotationType().getCanonicalName();
        boolean initialized = isInitialized();
        if (parentAnnotation == null) {
            if (initialized)
                processingEnv.getMessager().printMessage(
                        Diagnostic.Kind.WARNING,
                        "No ParentAnnotationType annotation " + "found on "
                                + this.getClass().getName()
                                + ", returning an empty set.");
            return Collections.emptySet();
        } else {
            return Collections.unmodifiableSet(Set.of(parentAnnotation));
        }
    }

    protected boolean info(String pErrorMessage, Element pElement) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                pErrorMessage, pElement);
        return true;
    }

    public boolean isAssignable(TypeMirror type, Class<?> clazz) {
        TypeMirror superType = processingEnv.getElementUtils()
                .getTypeElement(clazz.getCanonicalName()).asType();
        return processingEnv.getTypeUtils().isAssignable(type, superType);
    }

    public boolean isProcessed() {
        return processed;
    }

    public boolean isTargetTypeElementValid(TypeElement pTypeElement) {

        if (!allowedTypeKinds().contains(pTypeElement.getKind())) {
            error("The annotation '" + getParentAnnotationType()
                    + "' can be used only on " + allowedTypeKinds(),
                    pTypeElement);
            return false;
        }
        return true;
    }

    protected abstract boolean isValidMethodCandidate(M pMethodDescriptor,
            ExecutableElement pMethodElement);

    @Override
    public boolean process(Set<? extends TypeElement> pAnnotations,
            final RoundEnvironment pRoundEnvironment) {

        Set<M> methodDescriptors = new HashSet<>();

        getChildrenAnnotationTypes().stream().forEach(t -> {
            // deal with the annotation container (for repetitions)
            if (t.v2 != null) {
                Set<M> md2 = pRoundEnvironment.getElementsAnnotatedWith(t.v2)
                        .stream().filter(e -> e instanceof ExecutableElement)
                        .flatMap(
                                e -> collectMethodDescriptorsForAnnotationContainer(
                                        (ExecutableElement) e, t.v2, t.v1)
                                                .stream())
                        .collect(Collectors.toSet());
                methodDescriptors.addAll(md2);
            }

            // deal with annotations that have no repetitions
            Set<M> md1 = pRoundEnvironment.getElementsAnnotatedWith(t.v1)
                    .stream().filter(e -> e instanceof ExecutableElement)
                    .flatMap(e -> collectMethodDescriptorForAnnotation(
                            (ExecutableElement) e, t.v1).stream())
                    .collect(Collectors.toSet());
            methodDescriptors.addAll(md1);

        });

        // start processing the elements tagged with the parent
        // annotation
        Set<? extends Element> rootElements = pRoundEnvironment
                .getElementsAnnotatedWith(getParentAnnotationType());
        Optional<? extends Element> targetElementOpt = rootElements.stream()
                .findFirst();
        targetElementOpt.ifPresent(targetElement -> {

            if (!(targetElement instanceof TypeElement)
                    || !isTargetTypeElementValid((TypeElement) targetElement)) {
                throw new AnnotationProcessException("The "
                        + targetElement.getSimpleName() + "annotated with "
                        + getParentAnnotationType().getName()
                        + " is not valid.");

            }
            T typeDescriptor = collectTypeAnnotateDescriptor(
                    (TypeElement) targetElement, getParentAnnotationType(),
                    methodDescriptors);
            if (typeDescriptor == null) {
                error("An error have occurred while processing the element tagged with annotation '"
                        + getParentAnnotationType().getCanonicalName() + "'!",
                        targetElement);
            }
            processed = processMessageBundleDescriptor(typeDescriptor);

        });
        return processed;
    }

    protected abstract boolean processMessageBundleDescriptor(T interfaceInfo);

}
