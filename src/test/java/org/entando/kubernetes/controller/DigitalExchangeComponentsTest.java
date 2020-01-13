package org.entando.kubernetes.controller;

import org.entando.kubernetes.service.digitalexchange.component.DigitalExchangeComponentsService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class DigitalExchangeComponentsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DigitalExchangeComponentsService componentsService;


    @Test
    public void shouldStart() {

    }

}
