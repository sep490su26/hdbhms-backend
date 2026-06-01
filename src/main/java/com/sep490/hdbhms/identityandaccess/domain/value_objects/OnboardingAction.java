package com.sep490.hdbhms.identityandaccess.domain.value_objects;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OnboardingAction {
    String actionKey;
    String label;
    boolean completed;
    int priority;
    String actionUrl;
}
