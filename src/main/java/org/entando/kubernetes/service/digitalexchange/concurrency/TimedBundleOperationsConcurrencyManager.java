/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

/**
 * this class represents the component responsible for the concurrency management of the bundle operations (analysis,
 * install, etc.)
 * it is based on a time interval algorithm it is set at each application boot to avoid possible persistent problems
 */

package org.entando.kubernetes.service.digitalexchange.concurrency;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Component;

@Component
public class TimedBundleOperationsConcurrencyManager implements BundleOperationsConcurrencyManager {

    public static final int MAX_OPERATION_MINUTES = 30;

    private final AtomicReference<LocalDateTime> lastOperationStartTime;

    public TimedBundleOperationsConcurrencyManager() {
        this.lastOperationStartTime = new AtomicReference<>();
    }

    public TimedBundleOperationsConcurrencyManager(
            LocalDateTime lastOperationStartTime) {
        this.lastOperationStartTime = new AtomicReference<>(lastOperationStartTime);
    }


    @Override
    public boolean manageStartOperation() {

        try {
            lastOperationStartTime.getAndAccumulate(LocalDateTime.now(), (currentTime, newTime) -> {
                if (null != currentTime && currentTime.isAfter(newTime.minusMinutes(MAX_OPERATION_MINUTES))) {
                    throw new TimedBundleOperationConcurrencyManagerException();
                }
                return newTime;
            });
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    @Override
    public void operationTerminated() {
        lastOperationStartTime.set(null);
    }


    private class TimedBundleOperationConcurrencyManagerException extends RuntimeException {
    }
}
