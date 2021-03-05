package org.entando.kubernetes.model.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.entando.kubernetes.exception.EntandoBundleJobErrors;
import org.entando.kubernetes.exception.EntandoComponentManagerException;
import org.junit.jupiter.api.Test;

class JobResultTest {

    @Test
    void shouldReturnTrueIfHasAtLeastOneException() {

        JobResult jobResult = JobResult.builder().installException(new EntandoComponentManagerException()).build();
        assertTrue(jobResult.hasException());

        jobResult = JobResult.builder().rollbackException(new EntandoComponentManagerException()).build();
        assertTrue(jobResult.hasException());
    }

    @Test
    void shouldInstallErrorCodeIfExceptionIsPresent() {

        int errorCode = EntandoBundleJobErrors.GENERIC.getCode();

        JobResult jobResult = JobResult.builder().installException(new EntandoComponentManagerException(errorCode))
                .build();
        assertThat(jobResult.getInstallErrorCode()).isEqualTo(errorCode);

        jobResult = JobResult.builder().rollbackException(new EntandoComponentManagerException(errorCode))
                .build();
        assertThat(jobResult.getRollbackErrorCode()).isEqualTo(errorCode);
    }

    @Test
    void shouldInstallErrorMessageIfExceptionIsPresent() {

        String errorMex = "big error";

        JobResult jobResult = JobResult.builder().installException(new EntandoComponentManagerException(errorMex))
                .build();
        assertThat(jobResult.getInstallErrorMessage()).isEqualTo(errorMex);

        jobResult = JobResult.builder().rollbackException(new EntandoComponentManagerException(errorMex))
                .build();
        assertThat(jobResult.getRollbackErrorMessage()).isEqualTo(errorMex);
    }

    @Test
    void shouldCorrectlyFormatErrorMessageIfExceptionIsPresent() {

        String errorMex = "big error";

        JobResult jobResult = JobResult.builder().installException(new EntandoComponentManagerException(errorMex))
                .build();
        assertThat(jobResult.getInstallError()).isEqualTo("code: 100 --- message: " + errorMex);

        jobResult = JobResult.builder().rollbackException(new EntandoComponentManagerException(errorMex))
                .build();
        assertThat(jobResult.getRollbackError()).isEqualTo("code: 100 --- message: " + errorMex);
    }

    @Test
    void shouldSetEntandoComponentManagerExceptionWithEveryKindOfExceptionIsReceived() {

        JobResult jobResult = new JobResult().setInstallException(new EntandoComponentManagerException());
        assertThat(jobResult.getInstallException()).isInstanceOf(EntandoComponentManagerException.class);

        jobResult = new JobResult().setInstallException(new Exception());
        assertThat(jobResult.getInstallException()).isInstanceOf(EntandoComponentManagerException.class);

        jobResult = new JobResult().setRollbackException(new EntandoComponentManagerException());
        assertThat(jobResult.getRollbackException()).isInstanceOf(EntandoComponentManagerException.class);

        jobResult = new JobResult().setRollbackException(new Exception());
        assertThat(jobResult.getRollbackException()).isInstanceOf(EntandoComponentManagerException.class);
    }

    @Test
    void shouldClearExceptions() {

        JobResult jobResult = JobResult.builder().installException(new EntandoComponentManagerException(""))
                .rollbackException(new EntandoComponentManagerException()).build();

        assertNotNull(jobResult.getInstallException());
        assertNotNull(jobResult.getRollbackException());

        jobResult.clearException();

        assertNull(jobResult.getInstallException());
        assertNull(jobResult.getRollbackException());
    }
}
