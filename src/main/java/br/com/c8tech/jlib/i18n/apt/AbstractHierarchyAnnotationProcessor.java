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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;

public abstract class AbstractHierarchyAnnotationProcessor<M extends TypeAnnotatedMethodDescriptor, T extends TypeAnnotatedDescriptor<M>>
        extends AbstractProcessor {
    boolean processed;

    protected Set<ElementKind> allowedTypeKinds() {
        return Set.of(ElementKind.INTERFACE, ElementKind.CLASS);
    }

    private Set<M> collectMethodAnnotatedDescriptor(
            ExecutableElement pMethodElement) {

        Set<M> descriptorsPerAnnotation = new HashSet<>(1);

        processingEnv.getElementUtils().getAllAnnotationMirrors(pMethodElement)
                .stream()
                // bring only those who are tagged by one of those children
                // annotations
                .filter(ann -> getChildrenAnnotationTypes()
                        .contains(ann.getAnnotationType().toString()))
                .forEach(annotation -> {

                    Map<String, Object> values = processingEnv.getElementUtils()
                            .getElementValuesWithDefaults(annotation).entrySet()
                            .stream()
                            .collect(Collectors.toMap(
                                    ek -> ek.getKey().getSimpleName()
                                            .toString(),
                                    ev -> ev.getValue().getValue(), (a, b) -> a,
                                    TreeMap::new));

                    String name = pMethodElement.getSimpleName().toString();

                    M descr = createMethodAnnotatedDescriptor(values,
                            TypeAnnotatedMethodDescriptor.builder()
                                    .annotationName(annotation
                                            .getAnnotationType().toString())
                                    .methodElement(pMethodElement)
                                    .name(name)
                                    .qualifiedReturnType(
                                            pMethodElement.getReturnType())
                                    .qualifiedParameterTypes(
                                            pMethodElement.getParameters())
                                    .build());
                    if (descr != null
                            && isValidCandidate(descr, pMethodElement)) {
                        descriptorsPerAnnotation.add(descr);
                    }
                });

        return descriptorsPerAnnotation;

    }

    private T collectTypeAnnotateDescriptor(TypeElement pTargetElement,
            Element pClassAnnotationElement) {

        if (!isTargetValid(pTargetElement)) {
            return null;
        }

        String packageName = ((PackageElement) pTargetElement
                .getEnclosingElement()).getQualifiedName().toString();
        String qualifiedName = pTargetElement.getQualifiedName().toString();
        String simpleName = pTargetElement.getSimpleName().toString();
        List<M> methodInfos = new ArrayList<>();

        for (Element method : pTargetElement.getEnclosedElements()) {
            if (method instanceof ExecutableElement) {
                Set<M> methodDescriptors = collectMethodAnnotatedDescriptor(
                        (ExecutableElement) method);
                if (!methodDescriptors.isEmpty()) {
                    methodInfos.addAll(methodDescriptors);
                } else {
                    return null;
                }
            }
        }

        return createTypeAnnotatedDescriptor(TypeAnnotatedDescriptor
                .<M> builder().classAnnotationElement(pClassAnnotationElement)
                .packageName(packageName).qualifiedName(qualifiedName)
                .simpleName(simpleName).methodDescriptors(methodInfos)
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

    protected boolean info(String pErrorMessage, Element pElement) {
        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                pErrorMessage, pElement);
        return true;
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

    protected abstract Set<String> getChildrenAnnotationTypes();

    protected abstract String getParentAnnotationType();

    @Override
    public final Set<String> getSupportedAnnotationTypes() {
        String parentAnnotation = getParentAnnotationType();
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

    public boolean isAssignable(TypeMirror type, Class<?> clazz) {
        TypeMirror superType = processingEnv.getElementUtils()
                .getTypeElement(clazz.getCanonicalName()).asType();
        return processingEnv.getTypeUtils().isAssignable(type, superType);
    }

    public boolean isProcessed() {
        return processed;
    }

    public boolean isTargetValid(TypeElement pTypeElement) {

        if (!allowedTypeKinds().contains(pTypeElement.getKind())) {
            error("The annotation '" + getParentAnnotationType()
                    + "' can be used only on " + allowedTypeKinds(),
                    pTypeElement);
            return false;
        }
        return true;
    }

    protected abstract boolean isValidCandidate(M pMethodDescriptor,
            ExecutableElement pMethodElement);

    @Override
    public boolean process(Set<? extends TypeElement> pAnnotations,
            RoundEnvironment pRoundEnvironment) {

        pAnnotations.stream().findFirst().ifPresent(annotation -> {
            for (Element targetElement : pRoundEnvironment
                    .getElementsAnnotatedWith(annotation)) {
                processTargetElement(targetElement, annotation);
            }
        });
        return isProcessed();
    }

    private void processTargetElement(Element pTargetElement,
            TypeElement pAnnotation) {

        TypeElement targetTypeElement = (TypeElement) pTargetElement;

        T typeDescriptor = collectTypeAnnotateDescriptor(targetTypeElement, pAnnotation);
        if (typeDescriptor != null && processMessageBundleDescriptor(typeDescriptor)) {
            processed = true;
        } else {
            error("Could not process interface info for type '" + pAnnotation
                    + "'!", targetTypeElement);
            processed = false;
        }

    }

    protected abstract boolean processMessageBundleDescriptor(T interfaceInfo);

}
