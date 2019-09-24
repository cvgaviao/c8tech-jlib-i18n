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
package br.com.c8tech.javalib.apt.i18n;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

import br.com.c8tech.javalib.apt.TypeAnnotatedDescriptor;
import br.com.c8tech.javalib.apt.i18n.ImmutableMessageBundleDescriptor.Builder;

@Value.Immutable
// Make generated class package private
@Value.Style(visibility = ImplementationVisibility.PACKAGE,
        builderVisibility = Value.Style.BuilderVisibility.PACKAGE)
public interface MessageBundleDescriptor<M extends MessageBundleMethodDescriptor>
        extends TypeAnnotatedDescriptor<M> {

    public static <M extends MessageBundleMethodDescriptor> Builder<M> builder() {
        return ImmutableMessageBundleDescriptor.builder();
    }

    public String getPropertiesBaseName();

    public String getPropertiesBasePath();

}
