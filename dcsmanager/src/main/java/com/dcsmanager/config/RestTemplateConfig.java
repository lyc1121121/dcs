package com.dcsmanager.config;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * DCSServer 는 자체 서명(self-signed) 인증서를 사용하므로, 사설 CA 배포 전까지는
 * 인증서 신뢰 검증을 생략한다. 실제 통신 인증은 X-API-KEY 헤더로 이루어진다.
 * DCSServer가 내부적으로 DCSAgent 를 호출하는 시간까지 감안해 소켓 타임아웃을 넉넉히 둔다.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() throws Exception {
        SSLContextBuilder sslBuilder = new SSLContextBuilder()
                .loadTrustMaterial(null, (chain, authType) -> true);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(5000)
                .setConnectionRequestTimeout(5000)
                .setSocketTimeout(30000)
                .build();
        CloseableHttpClient httpClient = HttpClients.custom()
                .setSSLContext(sslBuilder.build())
                .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .setDefaultRequestConfig(requestConfig)
                .build();
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}
