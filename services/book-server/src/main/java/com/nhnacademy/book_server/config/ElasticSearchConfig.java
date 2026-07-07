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
                if (hasKoreanAnalyzer(index)) {
                    log.info("ES: high-five 이미 존재");
                    return;
                }

                long documentCount = client.count(c -> c.index(index)).count();
                if (documentCount > 0) {
                    log.warn("ES: high-five 인덱스에 korean_html_analyzer가 없지만 문서가 {}건 있어 재생성하지 않습니다.", documentCount);
                    return;
                }

                client.indices().delete(d -> d.index(index));
                log.info("ES: 잘못 생성된 빈 high-five 인덱스를 삭제했습니다.");
            }

            createIndex(index);
            log.info("ES: high-five 생성 완료");

        } catch (Exception e) {
            log.warn("Elasticsearch 인덱스 초기화 실패. 서버는 계속 실행합니다.", e);
        }
    }

    private boolean hasKoreanAnalyzer(String index) {
        try {
            client.indices().analyze(a -> a
                    .index(index)
                    .analyzer("korean_html_analyzer")
                    .text("자바 스프링 테스트")
            );
            return true;
        } catch (Exception e) {
            log.warn("ES: high-five analyzer 확인 실패. 빈 인덱스라면 재생성합니다.", e);
            return false;
        }
    }

    private void createIndex(String index) throws Exception {
        ClassPathResource resource = new ClassPathResource("Elastic/analysis/high-five.json");

        try (InputStream jsonStream = resource.getInputStream()) {
            client.indices().create(c -> c.index(index).withJson(jsonStream));
        }
    }
}
