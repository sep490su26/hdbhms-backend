package com.sep490.hdbhms.scheduling.application.handler;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ScheduledTaskFailureClassifier {

    public boolean retryable(Exception exception) {
        if (containsCause(exception, IllegalArgumentException.class)) {
            return false;
        }
        ResponseStatusException responseStatusException = findCause(exception, ResponseStatusException.class);
        if (responseStatusException != null) {
            return retryableStatus(responseStatusException.getStatusCode());
        }
        if (containsCause(exception, DataIntegrityViolationException.class)) {
            return false;
        }
        if (exception instanceof IllegalStateException
                && exception.getMessage() != null
                && exception.getMessage().startsWith("No scheduled task handler registered")) {
            return false;
        }
        return true;
    }

    private boolean retryableStatus(HttpStatusCode statusCode) {
        if (statusCode.is5xxServerError()) {
            return true;
        }
        int value = statusCode.value();
        return value == 408 || value == 409 || value == 423 || value == 425 || value == 429;
    }

    private boolean containsCause(Throwable throwable, Class<? extends Throwable> causeType) {
        return findCause(throwable, causeType) != null;
    }

    private <T extends Throwable> T findCause(Throwable throwable, Class<T> causeType) {
        Throwable current = throwable;
        while (current != null) {
            if (causeType.isInstance(current)) {
                return causeType.cast(current);
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return null;
    }
}
