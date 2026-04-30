package kr.spartaclub.aifriends.tool;

import kr.spartaclub.aifriends.domain.GameStateEntry;
import kr.spartaclub.aifriends.repository.GameStateEntryRepository;
import kr.spartaclub.aifriends.tool.dto.GameStateSnapshot;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

/**
 * Day 11 Step 3 — {@link GameStateTool} 단위 테스트.
 *
 * <p>Repository 를 Mockito 로 모킹해 도구의 두 메서드 (saveGameState, loadGameState) 가
 * 의도한 영속화 동작을 하는지 검증한다.</p>
 *
 * <p>핵심 가르침: {@code @Tool} 메서드는 LLM 이 자율로 호출하지만, 우리 입장에선 평범한
 * 자바 메서드다. 그래서 단위 테스트도 평범하게 짠다 — LLM 시뮬레이션이나 도구 디스패처를
 * 끌어들일 필요가 없다. "도구 본체는 평범한 메서드" 가 학생이 한 번에 잡아야 할 직관이다.</p>
 */
@ExtendWith(MockitoExtension.class)
class GameStateToolTest {

    @Mock
    private GameStateEntryRepository repository;

    @InjectMocks
    private GameStateTool gameStateTool;

    @Test
    @DisplayName("saveGameState - playerId 와 메시지를 받아 새 row 를 append 한다")
    void saveGameState_appendsNewRow() {
        gameStateTool.saveGameState(42L, "오늘 너무 힘들었어", "오늘은 좀 쉬자, 내가 같이 있을게", 7);

        ArgumentCaptor<GameStateEntry> captor = ArgumentCaptor.forClass(GameStateEntry.class);
        then(repository).should().save(captor.capture());

        GameStateEntry saved = captor.getValue();
        assertThat(saved.getPlayerId()).isEqualTo(42L);
        assertThat(saved.getLastUserMessage()).isEqualTo("오늘 너무 힘들었어");
        assertThat(saved.getLastAiMessage()).isEqualTo("오늘은 좀 쉬자, 내가 같이 있을게");
        assertThat(saved.getTurnCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("loadGameState - 저장된 가장 최근 1건을 GameStateSnapshot 으로 변환해 반환한다")
    void loadGameState_returnsLatestSnapshot() {
        GameStateEntry latest = new GameStateEntry(
                100L, 42L, "오늘 너무 힘들었어", "오늘은 좀 쉬자", 7, null);
        given(repository.findFirstByPlayerIdOrderByCreatedAtDesc(42L)).willReturn(Optional.of(latest));

        GameStateSnapshot snapshot = gameStateTool.loadGameState(42L);

        assertThat(snapshot.found()).isTrue();
        assertThat(snapshot.lastUserMessage()).isEqualTo("오늘 너무 힘들었어");
        assertThat(snapshot.lastAiMessage()).isEqualTo("오늘은 좀 쉬자");
        assertThat(snapshot.turnCount()).isEqualTo(7);
    }

    @Test
    @DisplayName("loadGameState - 저장된 기록이 없으면 found=false 인 빈 Snapshot 을 반환한다")
    void loadGameState_noHistory_returnsEmpty() {
        given(repository.findFirstByPlayerIdOrderByCreatedAtDesc(99L)).willReturn(Optional.empty());

        GameStateSnapshot snapshot = gameStateTool.loadGameState(99L);

        assertThat(snapshot.found()).isFalse();
        assertThat(snapshot.lastUserMessage()).isEmpty();
        assertThat(snapshot.lastAiMessage()).isEmpty();
        assertThat(snapshot.turnCount()).isZero();
    }
}
