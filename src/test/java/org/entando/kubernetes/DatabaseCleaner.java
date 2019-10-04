package org.entando.kubernetes;

import org.entando.kubernetes.repository.DigitalExchangeJobComponentRepository;
import org.entando.kubernetes.repository.DigitalExchangeJobRepository;
import org.entando.kubernetes.repository.DigitalExchangeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.DriverManager;
import java.sql.SQLException;

@Component
public class DatabaseCleaner {

    @Autowired
    DigitalExchangeRepository exchangeRepository;

    @Autowired
    DigitalExchangeJobRepository jobRepository;

    @Autowired
    DigitalExchangeJobComponentRepository jobComponentRepository;

    public void cleanup() {
        jobComponentRepository.deleteAll();
        jobRepository.deleteAll();
        exchangeRepository.deleteAll();
    }

}
