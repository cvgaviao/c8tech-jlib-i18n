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

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.joor.CompileOptions;
import org.joor.Reflect;
import org.joor.ReflectException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import br.com.c8tech.jlib.i18n.apt.MessageBundleGeneratorAnnotationProcessor;

public class MessageBundleAnnotationProcessorTest {

    @Test
    @DisplayName("Fails when annotation is used on classes")
    public void ensureAnnotationProcessorFailsUsingWithClass()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        try {
            Reflect.compile("br.com.c8tech.jlib.i18n.apt.NoInterface",
                    "package br.com.c8tech.jlib.i18n.apt; "
                            + "import br.com.c8tech.jlib.i18n.annotations.MessageBundle;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.Message;\n"
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
        try {
            Reflect.compile(
                    "br.com.c8tech.jlib.i18n.apt.SourceResourceBundleWithTwoMethods",
                    "package br.com.c8tech.jlib.i18n.apt;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.MessageBundle;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.Message;\n"
                            + "@MessageBundle \n"
                            + "public interface SourceResourceBundleWithTwoMethods {\n"
                            + "    @Message(value = \"funcionou m1 {0} ! \", locale = \"pt-BR\")\n"
                            + "    @Message(value = \"worked m1 {0} ! \", locale = \"en-US\")\n"
                            + "    public String m1(String pZero);\n" + "\n"
                            + "    @Message(value = \"worked m2 {0} ! \", locale = \"en-US\")\n"
                            + "    public String m2(String pZero);\n" + "\n"
                            + "    @Message(value = \"worked {0} {1}!\", locale = \"en-US\")\n"
                            + "    @Message(value = \"funcionou {0} {1}!\", locale = \"pt-BR\")\n"
                            + "    public String m3(String pZero, String pOne);\n"
                            + "}",
                    new CompileOptions().options("-source", "8")
                            .processors(processor))
                    .type();
        } catch (ReflectException e) {
        }

        assertTrue(processor.isProcessed());

    }

    @Test
    public void ensureAnnotationProcessorCreatesResourceBundleClassAndPropertiesWithDefaultLocale() {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        try {
            Reflect.compile(
                    "br.com.c8tech.jlib.i18n.apt.SourceResourceBundleWithTwoMethods",
                    "package br.com.c8tech.jlib.i18n.apt;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.MessageBundle;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.Message;\n"
                            + "@MessageBundle \n"
                            + "public interface SourceResourceBundleWithTwoMethods {\n"
                            + "    @Message(value = \"worked {0} ! \")\n"
                            + "    public String m1(String pZero);\n" + "\n"
                            + "    @Message(value = \"funcionou {0} {1}!\")\n"
                            + "    public String m2(String pZero, String pOne);\n"
                            + "}",
                    new CompileOptions().options("-source", "11")
                            .processors(processor))
                    .type();
        } catch (ReflectException e) {
        }

        assertTrue(processor.isProcessed());

    }

    @Test
    public void ensureAnnotationProcessorFailsWithWrongMethodReturn()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        Assertions.assertThrows(org.joor.ReflectException.class, () -> {
            Reflect.compile(
                    "br.com.c8tech.jlib.i18n.apt.SourceEmptyResourceBundle",
                    "package br.com.c8tech.jlib.i18n.apt; "
                            + "import br.com.c8tech.jlib.i18n.annotations.MessageBundle;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.Message;\n"
                            + "@MessageBundle " + "@Other "
                            + "public interface SourceEmptyResourceBundle {"
                            + "@Message(\" worked {0} ! \")"
                            + " public void m(String pZero);" + "}",
                    new CompileOptions().options("-source", "8")
                            .processors(processor))
                    .type();
        });
    }

    @Test
    public void ensureAnnotationProcessorWillFailWithouJava8Plus()
            throws Exception {
        MessageBundleGeneratorAnnotationProcessor processor = new MessageBundleGeneratorAnnotationProcessor();
        try {
            Reflect.compile("br.com.c8tech.jlib.i18n.apt.Source7NOK",
                    "package br.com.c8tech.jlib.i18n.apt; "
                            + "import br.com.c8tech.jlib.i18n.annotations.MessageBundle;\n"
                            + "import br.com.c8tech.jlib.i18n.annotations.Message;\n"
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
