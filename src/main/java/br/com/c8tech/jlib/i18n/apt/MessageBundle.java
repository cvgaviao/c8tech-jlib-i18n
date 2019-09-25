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

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(SOURCE)
@Target(TYPE)
public @interface MessageBundle {

    /**
     * The default locale which messages methods from target interface will be
     * tagged.
     * 
     * @return the default locale of the message bundle.
     */
    String locale() default "en-US";

    /**
     * Returns the base path to properties files for this message bundle. If
     * nothing is specified, the base path will be the qualified class name of
     * the message bundle with dot characters replaced by slash characters.
     *
     * @return the base path to properties files.
     */
    String base() default "";

    /**
     * Returns all other locales than the default one for which properties files
     * are required to be present for this message bundle.
     *
     * @return the locales for which properties files are required to be
     *         present.
     */
    String[] locales() default {};

    /**
     * Returns the location of the template on the class path which should be
     * used for the enum generation.
     *
     * @return the location of the template
     */
    String templateLocation() default "/META-INF/templates/messageBundleEnum.ftl";

}
