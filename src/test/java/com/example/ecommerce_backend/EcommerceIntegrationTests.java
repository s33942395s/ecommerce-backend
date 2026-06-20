package com.example.ecommerce_backend;

import com.example.ecommerce_backend.model.User;
import com.example.ecommerce_backend.repository.OrderRepository;
import com.example.ecommerce_backend.repository.ProductRepository;
import com.example.ecommerce_backend.repository.UserRepository;
import com.example.ecommerce_backend.util.JwtUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:ecommerce_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "app.jwt.secret=01234567890123456789012345678901"
})
class EcommerceIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @BeforeEach
    void cleanDatabase() {
        // Keep each test isolated from data created by previous tests.
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void loginShouldReturnJwtToken() throws Exception {
        // Login test: register a user, then verify login returns token, username, and role.
        register("login_user", "123456");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", "login_user",
                                "password", "123456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.username", is("login_user")))
                .andExpect(jsonPath("$.role", is("USER")));
    }

    @Test
    void productWriteApisShouldRequireAdminRole() throws Exception {
        // Permission test: product write APIs require a token and ADMIN role.
        String userToken = createUserAndToken("normal_user", "USER");
        String adminToken = createUserAndToken("admin_user", "ADMIN");

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody("No Token Product", "10.00", 1))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody("User Product", "10.00", 1))))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody("Admin Product", "10.00", 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Admin Product")))
                .andExpect(jsonPath("$.stock", is(1)));
    }

    @Test
    void productCrudShouldWorkForAdmin() throws Exception {
        // Product feature test: ADMIN can create, read, update, list, and soft-delete products.
        String adminToken = createUserAndToken("product_admin", "ADMIN");

        long productId = createProduct(adminToken, "Original Product", "100.00", 10);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Original Product")))
                .andExpect(jsonPath("$.stock", is(10)));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody("Updated Product", "120.00", 5))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Updated Product")))
                .andExpect(jsonPath("$.stock", is(5)));

        mockMvc.perform(get("/api/products")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(delete("/api/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound());
    }

    @Test
    void orderShouldDeductStockAndRejectWhenStockIsNotEnough() throws Exception {
        // Order stock test: first order succeeds, second order fails, and stock never goes negative.
        String adminToken = createUserAndToken("stock_admin", "ADMIN");
        String buyerToken = createUserAndToken("buyer", "USER");
        long productId = createProduct(adminToken, "Limited Product", "50.00", 1);

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderBody(productId, 1))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount", is(50.0)));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(0)));

        mockMvc.perform(post("/api/orders")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(orderBody(productId, 1))))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(0)));
    }

    @Test
    void concurrentOrdersShouldNotOversellStock() throws Exception {
        //多使用者下單測試庫存鎖
        String adminToken = createUserAndToken("concurrent_admin", "ADMIN");
        long productId = createProduct(adminToken, "Concurrent Product", "30.00", 3);

        int requestCount = 10;
        String[] buyerTokens = new String[requestCount];
        for (int i = 0; i < requestCount; i++) {
            buyerTokens[i] = createUserAndToken("concurrent_buyer_" + i, "USER");
        }

        ExecutorService executor = Executors.newFixedThreadPool(requestCount);
        CountDownLatch ready = new CountDownLatch(requestCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(requestCount);
        AtomicInteger successCount = new AtomicInteger();
        AtomicInteger failedCount = new AtomicInteger();

        for (int i = 0; i < requestCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    ready.countDown();
                    start.await(5, TimeUnit.SECONDS);

                    MvcResult result = mockMvc.perform(post("/api/orders")
                                    .header("Authorization", "Bearer " + buyerTokens[index])
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(json(orderBody(productId, 1))))
                            .andReturn();

                    int status = result.getResponse().getStatus();
                    if (status == 200) {
                        successCount.incrementAndGet();
                    } else if (status == 400) {
                        failedCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    failedCount.incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        org.assertj.core.api.Assertions.assertThat(successCount.get()).isEqualTo(3);
        org.assertj.core.api.Assertions.assertThat(failedCount.get()).isEqualTo(7);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stock", is(0)));
    }

    private void register(String username, String password) throws Exception {
        // Use the real register API to cover controller, JSON binding, and password hashing.
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "username", username,
                                "password", password
                        ))))
                .andExpect(status().isCreated());
    }

    private String createUserAndToken(String username, String role) {
        // Test helper: create a user with a specific role for ADMIN/USER permission branches.
        User user = new User();
        user.setUsername(username);
        user.setPassword(jwtUtil.encodePassword("123456"));
        user.setRole(role);
        userRepository.save(user);

        return jwtUtil.generateToken(username, role);
    }

    private long createProduct(String adminToken, String name, String price, int stock) throws Exception {
        // Create products through the real API so JwtInterceptor and ProductController are covered.
        MvcResult result = mockMvc.perform(post("/api/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(productBody(name, price, stock))))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }

    private Map<String, Object> productBody(String name, String price, int stock) {
        // Product API request body.
        return Map.of(
                "name", name,
                "price", price,
                "stock", stock
        );
    }

    private Map<String, Object> orderBody(long productId, int quantity) {
        // Order API request body.
        return Map.of(
                "items", new Object[] {
                        Map.of(
                                "productId", productId,
                                "quantity", quantity
                        )
                }
        );
    }

    private String json(Object value) throws Exception {
        // Convert objects to JSON strings for MockMvc requests.
        return objectMapper.writeValueAsString(value);
    }
}
