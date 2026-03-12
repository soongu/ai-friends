package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.dto.ChatLogResponse;
import kr.spartaclub.aifriends.repository.ChatLogRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatLogServiceTest {

    @Mock
    private ChatLogRepository chatLogRepository;

    @InjectMocks
    private ChatLogService chatLogService;

    @Test
    @DisplayName("채팅 로그 저장 성공")
    void saveLog_success() {
        // given
        ChatLog savedMock = new ChatLog(1L, 100L, "USER", "안녕하세요", null);
        given(chatLogRepository.save(any(ChatLog.class))).willReturn(savedMock);

        // when
        ChatLogResponse response = chatLogService.saveLog(100L, "USER", "안녕하세요");

        // then
        assertThat(response.speaker()).isEqualTo("USER");
        assertThat(response.message()).isEqualTo("안녕하세요");
    }

    @Test
    @DisplayName("채팅 로그 Slice 조회 성공")
    void getChatLogs_success() {
        // given
        ChatLog log1 = new ChatLog(2L, 100L, "AI", "반가워!", null);
        ChatLog log2 = new ChatLog(1L, 100L, "USER", "안녕", null);
        PageRequest pageRequest = PageRequest.of(0, 10);
        Slice<ChatLog> sliceMock = new SliceImpl<>(List.of(log1, log2), pageRequest, false);

        given(chatLogRepository.findBySoulmateIdOrderByCreatedAtDesc(eq(100L), eq(pageRequest)))
                .willReturn(sliceMock);

        // when
        Slice<ChatLogResponse> responses = chatLogService.getChatLogs(100L, pageRequest);

        // then
        assertThat(responses.getContent()).hasSize(2);
        assertThat(responses.getContent().get(0).message()).isEqualTo("반가워!");
        assertThat(responses.hasNext()).isFalse();
    }
}
