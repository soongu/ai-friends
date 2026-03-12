package kr.spartaclub.aifriends.repository;

import kr.spartaclub.aifriends.domain.ChatLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * ChatLog 엔티티 JPA Repository.
 * 채팅 목록은 Slice 기반 무한 스크롤(count 쿼리 없음).
 */
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {

    /**
     * soulmateId 기준 최근순 Slice 조회 (무한 스크롤용).
     * Pageable.size + 1 건 조회 후 다음 페이지 존재 여부만 반환.
     */
    Slice<ChatLog> findBySoulmateIdOrderByCreatedAtDesc(Long soulmateId, Pageable pageable);
}
