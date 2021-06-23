package io.github.springboot.httpclient;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Base64;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class IntegrationTests {
	private String base64ClientCredentials = new String(Base64.getEncoder().encode("admin:admin".getBytes()));

	@Autowired
	private MockMvc mockMvc;

	@Test
	public void integrationTestsFront() throws Exception {
		this.mockMvc
				.perform(get("/front/header").header("Authorization", "Basic " + base64ClientCredentials)
						.header("X-TEST-KEY", "srules"))
				.andExpect(status().isOk()).andExpect(header().string("X-TEST-RESPONSE", "yeah"));
	}

	@Test
	public void integrationTestsFrontAsync() throws Exception {
		this.mockMvc
				.perform(get("/front/async-header").header("Authorization", "Basic " + base64ClientCredentials)
						.header("X-TEST-KEY", "srules"))
				.andExpect(status().isOk()).andExpect(header().string("X-TEST-RESPONSE", "yeah"));
	}

	@Test
	public void integrationTestsFrontMultiAsync() throws Exception {
		this.mockMvc
				.perform(get("/front/multiasync-header").header("Authorization", "Basic " + base64ClientCredentials)
						.header("X-TEST-KEY", "srules"))
				.andExpect(status().isOk()).andExpect(header().stringValues("X-TEST-RESPONSE", "yeah", "again"));
	}

	@Test
	public void integrationTestsZback() throws Exception {
		this.mockMvc.perform(get("/back/header").header("Authorization", "Basic " + base64ClientCredentials))
				.andExpect(status().is(400)).andExpect(header().doesNotExist("X_TEST_RESPONSE"));
	}

}
