package kr.spartaclub.aifriends.service;

import kr.spartaclub.aifriends.domain.ChatLog;
import kr.spartaclub.aifriends.dto.ChatLogResponse;
import kr.spartaclub.aifriends.repository.ChatLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대화 기록(ChatLog) 비즈니스 로직을 처리하는 서비스 계층입니다.
 * 채팅 내역의 저장과 슬라이스(Slice) 기반 페이징 조회를 담당합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 조회용(읽기 전용) 트랜잭션을 적용합니다.
public class ChatLogService {

    private final ChatLogRepository chatLogRepository;

    /**
     * 사용자와 AI 교감(채팅) 데이터를 기록합니다.
     * 
     * @param soulmateId 대화가 속한 이성친구의 고유 ID
     * @param speaker    발화 주체 ("USER" 또는 "AI")
     * @param message    실제 대화 내용
     * @return 저장된 채팅 기록의 응답 DTO
     */
    @Transactional // 데이터를 삽입해야 하므로 쓰기용 트랜잭션을 적용합니다.
    public ChatLogResponse saveLog(Long soulmateId, String speaker, String message) {
        // ChatLog 엔티티 생성 시 생성자나 Builder를 사용 (id와 createdAt은 자동 생성되므로 null 처리)
        ChatLog chatLog = new ChatLog(null, soulmateId, speaker, message, null);
        
        // 엔티티 저장
        ChatLog saved = chatLogRepository.save(chatLog);
        
        // 반환용 DTO로 변환
        return ChatLogResponse.from(saved);
    }

    /**
     * 특정 이성친구와의 대화 기록을 과거순(또는 최신순)으로 Slice 페이징하여 조회합니다.
     * 무한 스크롤(Infinite Storing) UI에 최적화된 Slice를 반환합니다.
     * 
     * @param soulmateId 조회할 이성친구 고유 ID
     * @param pageable   페이지 번호, 크기, 정렬 정보 (프론트/컨트롤러에서 전달)
     * @return 다음 페이지 여부(hasNext)를 포함하는 DTO의 Slice 묶음
     */
    public Slice<ChatLogResponse> getChatLogs(Long soulmateId, Pageable pageable) {
        // Repository에서 엔티티 Slice를 반환받음
        Slice<ChatLog> chatLogSlice = chatLogRepository.findBySoulmateIdOrderByCreatedAtDesc(soulmateId, pageable);
        
        // 엔티티 내부 데이터를 노출하지 않도록 DTO로 매핑(map)하여 반환
        return chatLogSlice.map(ChatLogResponse::from);
    }
}
