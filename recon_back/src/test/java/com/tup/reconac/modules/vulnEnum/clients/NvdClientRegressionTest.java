package com.tup.reconac.modules.vulnEnum.clients;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class NvdClientRegressionTest {
    @Test void convertsLegacyAndPreservesOtherComponents() {
        assertEquals("cpe:2.3:a:openbsd:openssh:6.6.1p1:*:*:*:*:*:*:*",
                NvdCpeQuery.formatted("cpe:/a:openbsd:openssh:6.6.1p1"));
        assertEquals("cpe:2.3:a:v:p:1:u:e:en:sw:ts:th:other",
                NvdCpeQuery.formatted("cpe:/a:v:p:1:u:~e~sw~ts~th~other:en"));
        assertEquals("cpe:2.3:a:v:foo\\:bar:1:*:*:*:*:*:*:*",
                NvdCpeQuery.formatted("cpe:/a:v:foo%3abar:1"));
    }

    @Test void sendsLegacyCpeAsFormattedNameWithoutDoubleEncoding() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> {
            String query = URLDecoder.decode(request.getURI().getRawQuery(), StandardCharsets.UTF_8);
            assertTrue(query.contains("cpeName=cpe:2.3:a:openbsd:openssh:6.6.1p1:*:*:*:*:*:*:*"));
            assertFalse(request.getURI().getRawQuery().contains("%25"));
        }).andRespond(withSuccess("{\"totalResults\":0,\"vulnerabilities\":[]}", MediaType.APPLICATION_JSON));
        assertTrue(new NvdClient(builder, JsonMapper.builder().build())
                .findByCpe("cpe:/a:openbsd:openssh:6.6.1p1", "test-key").isEmpty());
        server.verify();
    }

    @Test void usesVirtualMatchForMissingVersion() {
        assertEquals("virtualMatchString", NvdCpeQuery.parameter("cpe:/a:igor_sysoev:nginx"));
        assertEquals("virtualMatchString", NvdCpeQuery.parameter("cpe:2.3:o:linux:linux_kernel:*:*:*:*:*:*:*:*"));
        assertEquals("cpeName", NvdCpeQuery.parameter("cpe:/a:apache:http_server:2.4.7"));
    }

    @Test void permanentRejectionIsNotRetriedAndRetainsReasonWithoutKey() {
        var builder = RestClient.builder();
        var server = MockRestServiceServer.bindTo(builder).build();
        server.expect(request -> {}).andRespond(withStatus(HttpStatus.NOT_FOUND)
                .header("message", "Invalid cpeName parameter, test-key"));
        var error = assertThrows(NvdLookupException.class, () ->
                new NvdClient(builder, JsonMapper.builder().build())
                        .findByCpe("cpe:/a:openbsd:openssh:6.6.1p1", "test-key"));
        assertTrue(error.getMessage().contains("NVD HTTP 404: Invalid cpeName parameter"));
        assertFalse(error.getMessage().contains("test-key"));
        server.verify();
    }
}
