package com.papairs.docs.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.e2e.fixture.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public abstract class AbstractE2ETest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected static final String USER_ID_HEADER = "X-User-Id";
    protected static final String CONTENT_TYPE_JSON = "application/json";

    protected static final String TEST_USER_1_ID = "ABC123";
    protected static final String TEST_USER_2_ID = "DEF456";
    protected static final String TEST_USER_3_ID = "GHI789";

    protected TestFixtures fixtures;

    @BeforeEach
    public void baseSetup() {
        fixtures = new TestFixtures(mockMvc, objectMapper, USER_ID_HEADER, CONTENT_TYPE_JSON);
    }

    protected String asJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
}
