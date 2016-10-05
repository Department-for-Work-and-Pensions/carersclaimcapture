package uk.gov.dwp.carersallowance.validations;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.StringUtils;

public class ValidationSummary {
    private List<ValidationError> formErrors;

    public ValidationSummary() {
        formErrors = new ArrayList<>();
    }

    public List<ValidationError> getFormErrors()  { return formErrors; }

    public void addFormError(String id, String displayName, String errorMessage) {
        formErrors.add(new FormValidationError(id, displayName, errorMessage));
    }

    public void reset() {
        formErrors.clear();
    }

    public boolean hasFormErrors() {
        return !formErrors.isEmpty();
    }

    public String getErrorDisplayName(String id) {
        ValidationError error = getError(id);
        if(error == null) {
            return null;
        }
        return error.getDisplayName();
    }

    public String getErrorMessage(String id) {
        ValidationError error = getError(id);
        if(error == null) {
            return null;
        }
        return error.getErrorMessage();
    }

    public ValidationError getError(String id) {
        if(formErrors == null || StringUtils.isEmpty(id)) {
            return null;
        }

        for(ValidationError error: formErrors) {
            if(id.equals(error.getId())) {
                return error;
            }
        }

        return null;
    }

    public boolean hasError(String id) {
        ValidationError error = getError(id);
        return error != null;
    }

    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(this.getClass().getName()).append("@").append(System.identityHashCode(this));
        buffer.append("=[");
        buffer.append(", formErrors = ").append(formErrors);
        buffer.append("]");

        return buffer.toString();
    }
}