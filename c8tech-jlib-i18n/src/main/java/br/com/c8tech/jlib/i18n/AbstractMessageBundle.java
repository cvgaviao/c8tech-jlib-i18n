package br.com.c8tech.jlib.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.PropertyResourceBundle;

public abstract class AbstractMessageBundle {

    protected class ConcreteResourceBundle extends PropertyResourceBundle {

        public ConcreteResourceBundle(InputStream pStream) throws IOException {
            super(pStream);
        }

    }
}
