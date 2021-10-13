package org.entando.kubernetes.assertionhelper;

import org.assertj.core.api.Java6Assertions;
import org.entando.kubernetes.model.web.response.SimpleRestResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class SimpleRestResponseAssertionHelper {

    public static <T> void assertOnSuccessfulResponse(ResponseEntity<SimpleRestResponse<T>> response, HttpStatus httpStatus) {
        Java6Assertions.assertThat(response.getStatusCode()).isEqualTo(httpStatus);
        Java6Assertions.assertThat(response.getBody().getErrors()).hasSize(0);
        Java6Assertions.assertThat(response.getBody().getMetaData()).isNull();
    }
}
