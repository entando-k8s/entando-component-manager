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
 * this interface represents the component responsible for the concurrency management of the bundle operations
 * (analysis, install, etc.)
 */

package org.entando.kubernetes.service.digitalexchange.concurrency;

import org.entando.kubernetes.exception.digitalexchange.BundleOperationConcurrencyException;

public interface BundleOperationsConcurrencyManager {


    /**
     * if no another operation manage the start of a bundle operation saving that an operation is in execution.
     *
     * @return false if another operation is in execution, true otherwise
     */
    boolean manageStartOperation();

    /**
     * get notified and track that the current bundle operation is terminated.
     */
    void operationTerminated();


    /**
     * check if there is another active bundle operation. in that case throws a BundleOperationConcurrencyException.
     *
     * @throws BundleOperationConcurrencyException if a bundle operation is already running
     */
    default void throwIfAnotherOperationIsRunningOrStartOperation() {

        if (!this.manageStartOperation()) {
            throw new BundleOperationConcurrencyException(
                    "Another bundle operation (analysis, install, etc.) is already"
                            + " running. Multiple concurrent operations are not supported.");
        }
    }

}
