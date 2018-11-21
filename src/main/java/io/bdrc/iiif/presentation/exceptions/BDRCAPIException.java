package io.bdrc.iiif.presentation.exceptions;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BDRCAPIException extends Exception
{
    private static final Logger logger = LoggerFactory.getLogger(Exception.class);
    private static final long serialVersionUID = -5379981810772284216L;
    int status;
    int code;
    String link;
    String developerMessage;
    String message;

    public BDRCAPIException() {
    }

    public BDRCAPIException(int status, int code, String message,
            String developerMessage, String link) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = developerMessage;
        this.link = link;
        if (status == 500) {
            logger.error("error status {}, code {}, message: {}", status, code, message);
        }
    }

    public BDRCAPIException(int status, int code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
        this.developerMessage = null;
        this.link = null;
        if (status == 500) {
            logger.error("error status {}, code {}, message: {}", status, code, message);
        }
    }

    public BDRCAPIException(int status, int code, Exception e) {
        super(e.getMessage());
        this.status = status;
        this.code = code;
        this.message = e.getMessage();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        this.developerMessage = sw.toString();
        this.link = null;
        if (status == 500) {
            logger.error("error status {}, code {}", status, code, e);
        }
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
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