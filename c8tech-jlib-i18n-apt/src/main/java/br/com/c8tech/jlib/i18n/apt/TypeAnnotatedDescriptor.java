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

import java.lang.annotation.Annotation;
import java.util.Collection;

import javax.lang.model.element.TypeElement;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

import br.com.c8tech.jlib.i18n.apt.ImmutableTypeAnnotatedDescriptor.Builder;

//Make generated class package private
@Value.Style(jdkOnly = true, visibility = ImplementationVisibility.PACKAGE,
        builderVisibility = Value.Style.BuilderVisibility.PACKAGE)
@Value.Immutable
public interface TypeAnnotatedDescriptor<M extends TypeAnnotatedMethodDescriptor> {

    public static <M extends TypeAnnotatedMethodDescriptor> Builder<M> builder() {
        return ImmutableTypeAnnotatedDescriptor.builder();
    }

    Class<? extends Annotation> annotationClass();

    String packageName();

    String qualifiedName();

    String simpleName();

    TypeElement targetType();

    Collection<M> methodDescriptors();

}
