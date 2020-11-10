/*
 * Copyright 2019-Present Entando Inc. (http://www.entando.com) All rights reserved.
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

package org.entando.kubernetes.model.job;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public enum JobStatus {

    INSTALL_CREATED,
    INSTALL_IN_PROGRESS,
    INSTALL_COMPLETED,
    INSTALL_ERROR,

    INSTALL_ROLLBACK,
    INSTALL_ROLLBACK_IN_PROGRESS,
    INSTALL_ROLLBACK_ERROR,

    UNINSTALL_CREATED,
    UNINSTALL_IN_PROGRESS,
    UNINSTALL_ERROR,
    UNINSTALL_COMPLETED;

    public boolean isOfType(JobType type) {
        return type.matches(this);
    }

    public boolean isAny(Set<JobStatus> possible) {
        return possible.contains(this);
    }

    public static List<JobStatus> getCompletedStatuses() {
        List<JobStatus> statuses = new ArrayList<>();
        statuses.add(INSTALL_COMPLETED);
        statuses.add(INSTALL_ERROR);
        statuses.add(INSTALL_ROLLBACK);
        statuses.add(INSTALL_ROLLBACK_ERROR);
        statuses.add(UNINSTALL_COMPLETED);
        statuses.add(UNINSTALL_ERROR);
        return statuses;
    }
}
