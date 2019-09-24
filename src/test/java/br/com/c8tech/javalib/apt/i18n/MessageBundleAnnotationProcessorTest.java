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

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joor.CompileOptions;
import org.joor.Reflect;
import org.joor.ReflectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class MessageBundleAnnotationProcessorTest {

    @Test
    @DisplayName("Fails when annotation is used on classes")
    public void ensureAnnotationProcessorFailsUsingWithClass()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        try {
            Reflect.compile("br.com.c8tech.javalib.apt.i18n.NoInterface",
                    "package br.com.c8tech.javalib.apt.i18n; "
                            + "@MessageBundle "
                            + "public class SourceClassNOK {"
                            + " private void m() {}" + "}",
                    new CompileOptions().options("-source", "8")
                            .processors(processor))
                    .create().get();

            fail("Annotation should be used only with interfaces");
        } catch (ReflectException expected) {
            assertFalse(processor.isProcessed());
        }

    }

    @Test
    public void ensureAnnotationProcessorCreatesResourceBundleClassAndPropertiesWithMultipleLocales()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        Reflect.compile(
                "br.com.c8tech.javalib.apt.i18n.SourceResourceBundleWithTwoMethods",
                "package br.com.c8tech.javalib.apt.i18n; " + "@MessageBundle "
                        + "public interface SourceResourceBundleWithTwoMethods {"
                        + "@Message(value=\" worked {0} ! \", locale=\"pt-BR\" )"
                        + " public String m1(String pZero);"
                        + "@Message(\" worked {0} {1}! \")"
                        + " public String m2(String pZero, String pOne);" + "}",
                new CompileOptions().options("-source", "8")
                        .processors(processor))
                .type();

        assertTrue(processor.isProcessed());

    }

    @Test
    public void ensureAnnotationProcessorFailsWithWrongMethodReturn()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        try {
            Reflect.compile(
                    "br.com.c8tech.javalib.apt.i18n.SourceEmptyResourceBundle",
                    "package br.com.c8tech.javalib.apt.i18n; "
                            + "@MessageBundle " + "@Other "
                            + "public interface SourceEmptyResourceBundle {"
                            + "@Message(\" worked {0} ! \")"
                            + " public void m(String pZero);" + "}",
                    new CompileOptions().options("-source", "8")
                            .processors(processor))
                    .type();
        } catch (ReflectException e) {
            assertFalse(processor.isProcessed());
        }
    }

    @Test
    public void ensureAnnotationProcessorWillFailWithouJava8Plus()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        try {
            Reflect.compile("br.com.c8tech.javalib.apt.i18n.Source7NOK",
                    "package br.com.c8tech.javalib.apt.i18n; "
                            + "@MessageBundle " + "public class Source7NOK {"
                            + " void m() {}" + "}",
                    new CompileOptions().options("-source", "7")
                            .processors(processor))
                    .create().get();

            fail("Class should not compile with source level 7");
        } catch (ReflectException expected) {
            assertFalse(processor.isProcessed());
        }
    }
}
