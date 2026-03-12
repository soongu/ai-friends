package kr.spartaclub.aifriends.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


import static org.assertj.core.api.Assertions.assertThat;

class ChatLogTest {

    @Test
    @DisplayName("ChatLog 엔티티가 정상적으로 생성되어야 한다")
    void createChatLog() {
        // given & when
        ChatLog chatLog = new ChatLog(null, 100L, "USER", "안녕하세요", null);

        // then
        assertThat(chatLog.getSoulmateId()).isEqualTo(100L);
        assertThat(chatLog.getSpeaker()).isEqualTo("USER");
        assertThat(chatLog.getMessage()).isEqualTo("안녕하세요");
    }
}
