package io.bdrc.iiif.presentation.exceptions;

public class AuthException extends Exception
{
    private static final long serialVersionUID = -5379981810772284216L;
    int status;
    int code;
    String link;
    String developerMessage;
    String message; 

    public AuthException() {
    }

    public AuthException(int status, int code, String message,
            String developerMessage, String link) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = developerMessage;
        this.link = link;
    }

    public AuthException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = null;
        this.link = null;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public void setDeveloperMessage(String developerMessage) {
        this.developerMessage = developerMessage;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}