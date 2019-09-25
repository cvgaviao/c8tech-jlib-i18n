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

import java.util.List;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

import br.com.c8tech.jlib.i18n.apt.ImmutableTypeAnnotatedMethodDescriptor.Builder;

//Make generated class package private
@Value.Style(visibility = ImplementationVisibility.PACKAGE,
        builderVisibility = Value.Style.BuilderVisibility.PACKAGE)

@Value.Immutable()
public interface TypeAnnotatedMethodDescriptor {

    public static Builder builder() {
        return ImmutableTypeAnnotatedMethodDescriptor.builder();
    }

    ExecutableElement methodElement();
    
    String annotationName();

    String name();

    List<VariableElement> qualifiedParameterTypes();

    TypeMirror qualifiedReturnType();

}
