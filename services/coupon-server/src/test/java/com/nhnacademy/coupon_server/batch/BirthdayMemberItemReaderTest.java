package com.nhnacademy.coupon_server.batch;

import com.nhnacademy.coupon_server.service.client.MemberServiceClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BirthdayMemberItemReaderTest {

    @Mock
    private MemberServiceClient memberServiceClient;

    @Test
    @DisplayName("Reader가 페이징 처리를 하며 데이터를 정상적으로 읽어오는지 테스트")
    void readSuccess() throws Exception {
        int chunkSize = 2;
        BirthdayMemberItemReader reader = new BirthdayMemberItemReader(memberServiceClient, chunkSize);

        when(memberServiceClient.getBirthdayUserId(anyInt(), eq(0), eq(chunkSize)))
                .thenReturn(List.of(1L, 2L));
        when(memberServiceClient.getBirthdayUserId(anyInt(), eq(1), eq(chunkSize)))
                .thenReturn(List.of(3L));
        when(memberServiceClient.getBirthdayUserId(anyInt(), eq(2), eq(chunkSize)))
                .thenReturn(Collections.emptyList());

        assertThat(reader.read()).isEqualTo(1L);
        assertThat(reader.read()).isEqualTo(2L);

        assertThat(reader.read()).isEqualTo(3L);

        assertThat(reader.read()).isNull();

        verify(memberServiceClient, times(3)).getBirthdayUserId(anyInt(), anyInt(), eq(chunkSize));
    }
}