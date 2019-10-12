package net.n2oapp.platform.loader.client;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit4.SpringRunner;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.MOCK)
public class ClientLoaderTest {
    @Rule
    public WireMockRule wireMockRule = new WireMockRule(options().port(8787));

    @Autowired
    private ClientLoader jsonClientLoader;
    @Autowired
    private ClientLoader simpleClientLoader;
    @Value("test.json")
    private ClassPathResource json;
    @Value("test.txt")
    private ClassPathResource text;

    @Test
    public void simpleLoad() throws URISyntaxException {
        stubFor(post(urlMatching("/simple/.*/.*"))
                .willReturn(aResponse()
                        .withStatus(200)));
        simpleClientLoader.load(new URI("http://localhost:8787"), "foo", "bar", text);
        verify(postRequestedFor(urlEqualTo("/simple/foo/bar"))
                .withHeader("Content-Type", containing("text/plain"))
                .withRequestBody(equalTo("Hello")));
    }

    @Test
    public void jsonLoad() throws URISyntaxException {
        stubFor(post(urlMatching("/load/.*/.*"))
                .willReturn(aResponse()
                        .withStatus(200)));
        jsonClientLoader.load(new URI("http://localhost:8787"), "foo", "bar", json);
        verify(postRequestedFor(urlEqualTo("/load/foo/bar"))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(equalToJson("[{\"code\":\"code1\",\"name\":\"name1\"},{\"code\":\"code2\",\"name\":\"name2\"}]")));
    }

    @Test
    public void run() {
        //success
        stubFor(post(urlMatching("/load/sub/test1")).willReturn(aResponse().withStatus(200)));
        stubFor(post(urlMatching("/simple/sub/test2")).willReturn(aResponse().withStatus(200)));
        ClientLoaderRunner runner = new ClientLoaderRunner(Arrays.asList(jsonClientLoader, simpleClientLoader));
        runner.add("http://localhost:8787", "sub", "test1", "test.json")
                .add("http://localhost:8787", "sub", "test2", "test.txt", SimpleClientLoader.class);
        LoaderReport report = runner.run();
        assertThat(report.isSuccess(), is(true));

        //fail fast
        stubFor(post(urlMatching("/load/sub/test1")).willReturn(aResponse().withStatus(500)));
        stubFor(post(urlMatching("/simple/sub/test2")).willReturn(aResponse().withStatus(200)));
        runner.setFailFast(true);
        report = runner.run();
        assertThat(report.getFails().size(), is(1));
        assertThat(report.getSuccess().size(), is(0));
        assertThat(report.getAborted().size(), is(1));

        //fail tolerance
        stubFor(post(urlMatching("/load/sub/test1")).willReturn(aResponse().withStatus(500)));
        stubFor(post(urlMatching("/simple/sub/test2")).willReturn(aResponse().withStatus(200)));
        runner.setFailFast(false);
        report = runner.run();
        assertThat(report.getFails().size(), is(1));
        assertThat(report.getSuccess().size(), is(1));
        assertThat(report.getAborted().size(), is(0));
    }
}
