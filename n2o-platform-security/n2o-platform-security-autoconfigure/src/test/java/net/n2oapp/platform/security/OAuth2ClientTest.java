package net.n2oapp.platform.security;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import net.n2oapp.platform.security.autoconfigure.SecurityAutoConfiguration;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Тесты чтения токена jwt и аутентификации
 */
@RunWith(SpringRunner.class)
@SpringBootApplication
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = OAuth2ClientTest.class)
@RestController
@EnableResourceServer
public class OAuth2ClientTest {

    private static final String TOKEN_VALUE = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJkZkRGYlJkU3FDZlVkMGV" +
            "vTXRQV0UzVXk2bko5UFhHRDFFN2Q3MXE5c1pNIn0.eyJqdGkiOiJiNmZjZWZhMC0yZjc1LTRlNTMtYTNjMS02M2ExZjE2ZTYzNjciLCJl" +
            "eHAiOjE1NjMyNTg0MTMsIm5iZiI6MCwiaWF0IjoxNTYzMjU4MTEzLCJpc3MiOiJodHRwOi8vMTcyLjE2LjEuMTMwOjg4ODgvYXV0aC9yZ" +
            "WFsbXMvTVBTIiwiYXVkIjoibXBzIiwic3ViIjoiNjRjMDQ1MDgtMWU2Mi00OGQ3LTk2ZjMtNjc3YWE1NzVmNTNmIiwidHlwIjoiQmVhcm" +
            "VyIiwiYXpwIjoibXBzIiwiYXV0aF90aW1lIjowLCJzZXNzaW9uX3N0YXRlIjoiYzcyZjc2YzQtYWM0ZC00ZjU4LWFmOTUtNDA4MzU4ZTF" +
            "iYzJlIiwiYWNyIjoiMSIsImFsbG93ZWQtb3JpZ2lucyI6W10sInJlc291cmNlX2FjY2VzcyI6e30sIm5hbWUiOiJUZXN0IFRlc3QiLCJn" +
            "aXZlbl9uYW1lIjoiVGVzdCIsImZhbWlseV9uYW1lIjoiVGVzdCIsImVtYWlsIjoidG8uaXJ5YWJvdkBnbWFpbC5jb20iLCJ1c2VybmFtZ" +
            "SI6InRlc3QifQ.AQpCJEQZnAews_F_-VBxIiSUMYmNerx_YulUvOAC6YTRWVVlt4BuuKNHS-0i1RURum5x5C7uDcry59r-3Cil8LRBmms" +
            "KUWNoqoWxr4H2Hfny0eFw8rlLwZeDdV7C-jvpO8Z3FTTHk7PybJIBDYG7pLcNStKtpzBeqTVahRt9vxQKhJ5lb0vdPpKnWtyoRaTTnQ7o" +
            "gNchsSSKfsHpvpkG7Ne_3Rd0JiES80VAH9HA8mCqOpRJ1ic2c-hFdmUvhfXSC0pNGcRzKR5hlk7BC9OX0_s5uk-Qi9kf0S_z5pgsPrJD3" +
            "tt5ey_6UoRXtOL7FCNmrLzTVBOsWu0PTgz0XFdh_w";

    @LocalServerPort
    private String port;
    @Autowired
    private TokenStore tokenStore;
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8787));


    /**
     * Тест чтения токена через {@link TokenStore}
     */
    @Test
    public void readAccessToken() {
        OAuth2AccessToken token = tokenStore.readAccessToken(TOKEN_VALUE);
        assertThat(token, notNullValue());
        assertThat(token.getTokenType(), equalToIgnoringCase("Bearer"));
        assertThat(token.getAdditionalInformation().get("username"), is("test"));
        assertThat(LocalDate.ofInstant(token.getExpiration().toInstant(), ZoneId.systemDefault()),
                Matchers.is(LocalDate.of(2019, 7, 16)));
    }

    /**
     * Тест аутентификации (клиент -> сервер) через токен
     */
    @Test
    public void authRequest() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> exchange = restTemplate.exchange("http://localhost:" + port + "/greeting", HttpMethod.GET, entity, String.class);
        assertThat(exchange.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(exchange.getBody(), is("Hello test"));
    }

    /**
     * Тест прокси аутентификации (сервер -> сервер) через токен
     */
    @Test
    public void authProxyRequest() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(TOKEN_VALUE);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<String> exchange = restTemplate.exchange("http://localhost:" + port + "/proxy/" + port, HttpMethod.GET, entity, String.class);
        assertThat(exchange.getStatusCode().is2xxSuccessful(), is(true));
        assertThat(exchange.getBody(), is("Hello test"));
    }
}
