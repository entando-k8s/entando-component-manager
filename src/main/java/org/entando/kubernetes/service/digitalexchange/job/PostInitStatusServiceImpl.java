package org.entando.kubernetes.service.digitalexchange.job;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PostInitStatusServiceImpl implements PostInitStatusService, InitializingBean {

    private PostInitStatus status;
    private boolean finished;
    private static final int MAX_RETIES = 30;
    private int retries = 0;


    @Override
    public boolean shouldRetry() {
        return retries < MAX_RETIES;
    }

    @Override
    public PostInitStatus getStatus() {
        return status;
    }

    @Override
    public boolean isCompleted() {
        return finished;
    }

    @Override
    public void startExecution() {
        finished = false;
        status = PostInitStatus.STARTED;
        retries++;
    }

    @Override
    public void endExecution(PostInitStatus status, boolean finished, boolean noMoreTry) {
        this.status = status;
        if (noMoreTry) {
            retries = MAX_RETIES;
        }
        this.finished = finished;

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        status = PostInitStatus.UNKNOWN;
        finished = false;
    }

}
