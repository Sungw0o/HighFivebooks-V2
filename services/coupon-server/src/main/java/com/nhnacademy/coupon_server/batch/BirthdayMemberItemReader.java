package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;

import java.time.LocalDate;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class BirthdayMemberItemReader implements ItemReader<Long> {
    private final MemberServiceClient memberServiceClient;
    private final int chunkSize;
    private final int targetMonth;

    private int page = 0;
    private Iterator<Long> currentChunkIterator;

    public BirthdayMemberItemReader(MemberServiceClient memberServiceClient, int chunkSize) {
        this.memberServiceClient = memberServiceClient;
        this.chunkSize = chunkSize;
        this.targetMonth = LocalDate.now().getMonthValue();
    }

    @Override
    public Long read() throws Exception {
        if (currentChunkIterator == null || !currentChunkIterator.hasNext()) {
            List<Long> nextChunk = fetchNextPage();

            // 더 이상 데이터가 없으면 null 반환 (Reader 종료)
            if (nextChunk == null || nextChunk.isEmpty()) {
                return null;
            }
            currentChunkIterator = nextChunk.iterator();
        }

        return currentChunkIterator.next();
    }

    private List<Long> fetchNextPage() {
        int currentMonth = LocalDate.now().getMonthValue();
        log.info("Fetching birthday users - Month: {}, Page: {}, Size: {}", targetMonth, page, chunkSize);

        List<Long> userIds = memberServiceClient.getBirthdayUserId(currentMonth, page, chunkSize);
        page++;
        return userIds;
    }
}
