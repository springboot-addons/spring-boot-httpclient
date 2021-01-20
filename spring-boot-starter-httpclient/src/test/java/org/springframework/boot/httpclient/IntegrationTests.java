package org.springframework.boot.httpclient;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.Base64;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import io.github.springboot.httpclient.interceptors.headers.RequestHeadersProviders;
import io.github.springboot.httpclient.interceptors.headers.RequestHeadersProviders.RequestHeadersStorage;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@AutoConfigureMockMvc
public class IntegrationTests {
	private String base64ClientCredentials = new String(Base64.getEncoder().encode("admin:admin".getBytes()));

    @Autowired
    private MockMvc mockMvc;
    
    @Test
    public void integrationTestsFront() throws Exception {
        this.mockMvc.perform(get("/front/header")
        		.header("Authorization", "Basic " + base64ClientCredentials)
        		.header("X-TEST-KEY", "srules"))
        	.andExpect(status().isOk())
        	.andExpect(header().string("X-TEST-RESPONSE", "yeah"))
        	;
    }
    
    @Test
    public void integrationTestsFrontAsync() throws Exception {
        this.mockMvc.perform(get("/front/async-header")
        		.header("Authorization", "Basic " + base64ClientCredentials)
        		.header("X-TEST-KEY", "srules"))
        	.andExpect(status().isOk())
        	.andExpect(header().string("X-TEST-RESPONSE", "yeah"))
        	;
    }
    
    @Test
    public void integrationTestsZback() throws Exception {
        this.mockMvc.perform(get("/back/header")
        		.header("Authorization", "Basic " + base64ClientCredentials)
        		)
        	.andExpect(status().is(400))
        	.andExpect(header().doesNotExist("X_TEST_RESPONSE"))
        	;
    }

    
}
