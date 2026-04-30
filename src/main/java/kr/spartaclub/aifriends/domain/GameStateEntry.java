package kr.spartaclub.aifriends.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Day 11 Step 3 — 게임 진행 상태 스냅샷 엔티티 (저장 1건당 1행, append-only).
 *
 * <p>Day 5 의 {@code chat_log} 테이블과 결을 맞춰 단방향 + 매번 새 row 를 쌓는 구조다.
 * 같은 {@code playerId} 의 가장 최근 1건만 조회 (
 * {@link kr.spartaclub.aifriends.repository.GameStateEntryRepository#findFirstByPlayerIdOrderByCreatedAtDesc})
 * 하므로 update 가 아니라 append 를 택했다 — "지난 대화의 시간선" 이 자연스럽게 남는다.</p>
 *
 * <p>이 엔티티는 LLM 이 직접 보지 않는다. {@code GameStateTool.loadGameState} 가 이 row 를
 * {@link kr.spartaclub.aifriends.tool.dto.GameStateSnapshot} 로 변환해서 LLM 에게 흘려준다 —
 * 도구 함수의 반환은 LLM 이 읽기 좋은 평면 record 로 두는 결.</p>
 */
@Entity
@Table(name = "game_state_entry", indexes = {
        @Index(name = "idx_game_state_player_created", columnList = "player_id, created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class GameStateEntry {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 플레이어 ID (FK 역할 — 본 강의 범위에선 단순 Long) */
    @Column(nullable = false)
    private Long playerId;

    /** 마지막으로 유저가 보낸 메시지 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lastUserMessage;

    /** 마지막으로 캐릭터가 보낸 메시지 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String lastAiMessage;

    /** 대화가 몇 턴까지 진행됐는지 (저장 시점 기준) */
    @Column(nullable = false)
    private int turnCount;

    /** 저장 시각 — 같은 playerId 의 row 들 중 가장 최근 1건을 조회할 때 정렬 키 */
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
