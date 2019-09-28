package br.com.c8tech.jlib.i18n.apt;

public class AnnotationProcessException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = -8002587263042439853L;

    public AnnotationProcessException() {
    }

    public AnnotationProcessException(String pMessage) {
        super(pMessage);
    }

    public AnnotationProcessException(Throwable pCause) {
        super(pCause);
    }

    public AnnotationProcessException(String pMessage, Throwable pCause) {
        super(pMessage, pCause);
    }

    public AnnotationProcessException(String pMessage, Throwable pCause,
            boolean pEnableSuppression, boolean pWritableStackTrace) {
        super(pMessage, pCause, pEnableSuppression, pWritableStackTrace);
    }

}
