package com.training.starter.controller;

import com.training.starter.exception.GlobalExceptionHandler;
import com.training.starter.mapper.ProductMapper;
import com.training.starter.repository.ProductRepository;
import com.training.starter.service.impl.ProductServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private ProductRepository productRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        productRepository = mock(ProductRepository.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        ProductServiceImpl productService = new ProductServiceImpl(productRepository, productMapper);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void search_blankQuery_returnsEstablishedBadRequestEnvelope() throws Exception {
        mockMvc.perform(get("/api/v1/products/search").param("q", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Search text must not be blank"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());

        verify(productRepository, never()).searchByVector(any(), any());
    }
}
