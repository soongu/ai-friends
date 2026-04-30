package kr.spartaclub.aifriends.repository;

import kr.spartaclub.aifriends.domain.GameStateEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Day 11 Step 3 — {@link GameStateEntry} JPA Repository.
 *
 * <p>도구가 호출하는 쿼리는 단 하나 — "이 playerId 의 가장 최근 저장본 1건".
 * append-only 테이블이라 같은 playerId 에 여러 row 가 쌓이지만, 도구는 항상 최신 1건만 본다.</p>
 */
public interface GameStateEntryRepository extends JpaRepository<GameStateEntry, Long> {

    /**
     * playerId 의 가장 최근 저장본을 조회한다 (없으면 Optional.empty).
     *
     * <p>Spring Data JPA 의 {@code findFirstBy...OrderBy...Desc} 컨벤션은
     * "정렬 후 1건" 을 의미한다 — 별도 LIMIT 1 직접 작성 없이 자동으로 LIMIT 절이 붙는다.</p>
     */
    Optional<GameStateEntry> findFirstByPlayerIdOrderByCreatedAtDesc(Long playerId);
}
