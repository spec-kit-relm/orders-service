package com.relm.orders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class OrdersApiTest {

    @Autowired MockMvc mvc;

    String body(UUID cart) {
        return """
            {"cartId":"%s","lines":[{"sku":"SKU-RED-MUG","quantity":2,"unitPrice":{"amountMinor":1250,"currency":"EUR"}}]}
            """.formatted(cart);
    }

    @Test
    @DisplayName("orders-api 1.2.0: placing an order computes the total in minor units")
    void placesOrder() throws Exception {
        mvc.perform(post("/v1/orders").header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total.amountMinor").value(2500))
                .andExpect(jsonPath("$.total.currency").value("EUR"));
    }

    @Test
    @DisplayName("BE-2: replaying the same Idempotency-Key returns the same order")
    void replaysSameOrder() throws Exception {
        UUID key = UUID.randomUUID();
        String req = body(UUID.randomUUID());
        MvcResult first = mvc.perform(post("/v1/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(req)).andExpect(status().isCreated()).andReturn();
        String id = com.jayway.jsonpath.JsonPath.read(first.getResponse().getContentAsString(), "$.id");
        mvc.perform(post("/v1/orders").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(req))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("BE-2: same Idempotency-Key with a different body is rejected with problem+json")
    void rejectsReusedKey() throws Exception {
        UUID key = UUID.randomUUID();
        mvc.perform(post("/v1/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID()))).andExpect(status().isCreated());
        mvc.perform(post("/v1/orders").header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON).content(body(UUID.randomUUID())))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.type").value("https://errors.relm.example/orders/idempotency-key-reused"));
    }
}
