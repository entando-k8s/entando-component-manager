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
package org.entando.kubernetes.model.digitalexchange;

import lombok.Data;

import java.util.Date;

@Data
public class DigitalExchangeJob {

    private String id;
    private String digitalExchangeId;
    private String digitalExchangeUrl;
    private String componentId;
    private String componentName;
    private String componentVersion;
    private byte[] componentSignature;
    private Date started;
    private Date ended;
    private String user;
    private double progress;
    private JobStatus status;
    private JobType jobType;

}
