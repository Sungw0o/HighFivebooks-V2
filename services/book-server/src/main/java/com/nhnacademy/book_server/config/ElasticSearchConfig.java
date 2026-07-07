package com.nhnacademy.book_server.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

// 인덱스 컴포넌트 생성
@Slf4j
@Component
@RequiredArgsConstructor
public class ElasticSearchConfig {

    private final ElasticsearchClient client;

    @PostConstruct
    public void createBookIndex() throws Exception {
        String index = "high-five";

        try {
            boolean exists = client.indices()
                    .exists(e -> e.index(index))
                    .value();

            if (exists) {
                log.info("ES: high-five 이미 존재");
                return;
            }

            ClassPathResource resource = new ClassPathResource("Elastic/analysis/high-five.json");

            try (InputStream jsonStream = resource.getInputStream()) {
                client.indices().create(c -> c.index(index).withJson(jsonStream));
            }

            log.info("ES: high-five 생성 완료");

        } catch (Exception e) {
            log.warn("Elasticsearch 인덱스 초기화 실패. 서버는 계속 실행합니다.", e);
        }
    }
}
