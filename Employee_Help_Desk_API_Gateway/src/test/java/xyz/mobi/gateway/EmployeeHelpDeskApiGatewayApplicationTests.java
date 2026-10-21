package xyz.mobi.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeHelpDeskApiGatewayApplicationTests {

    @Autowired
    private GatewayProxyController gatewayProxyController;

    @Test
    void contextLoads() {
        assertNotNull(gatewayProxyController);
    }

    @Test
    void testProxyUnreachableHostGracefullyReturnsBadGateway() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/user/health");
        MockHttpServletResponse response = new MockHttpServletResponse();

        gatewayProxyController.proxy(request, response);

        assertEquals(502, response.getStatus());
        assertTrue(response.getContentAsString().contains("Bad Gateway"));
    }
}
