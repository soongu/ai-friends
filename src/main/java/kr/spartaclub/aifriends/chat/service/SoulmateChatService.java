package kr.spartaclub.aifriends.chat.service;

import kr.spartaclub.aifriends.chat.dto.AiReply;
import kr.spartaclub.aifriends.common.exception.BusinessException;
import kr.spartaclub.aifriends.common.exception.ErrorCode;
import kr.spartaclub.aifriends.domain.Soulmate;
import kr.spartaclub.aifriends.repository.SoulmateRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;

/**
 * 소꿉친구 페르소나 ChatClient 를 사용하는 서비스.
 *
 * <p>Day 3 Step 3 — 호출 시점에 {userName}·{mood} 같은 동적 값을 꽂을 수 있도록
 * .system(Consumer) 람다 + PromptSystemSpec.text/param 으로 덮어쓰는 학습용 PoC 시그니처를 만들었다.</p>
 *
 * <p>Day 4 Step 5 — 응답 타입을 {@code String} 에서 {@link AiReply} record 로 교체했다.
 * {@code .call().entity(AiReply.class)} 가 BeanOutputConverter 로 JSON Schema 를
 * 자동 주입하고 응답을 record 인스턴스로 역직렬화까지 마쳐 돌려준다.</p>
 *
 * <p>Day 5 Step 5 — chat 시그니처에 {@code conversationId} 를 추가했다 (학습용 자유도 정책).</p>
 *
 * <p>Day 5 Step 6 (수렴) — {@link #chat(Long, String)} 가 prod 진입점으로 자랐다.
 * ai-friends 도메인은 *한 이성친구당 자루 하나* 라 conversationId 는 soulmateId 의 함수
 * ({@code String.valueOf(soulmateId)}) 로 결정된다. system 프롬프트는 Day 3 에서 만들어둔
 * {@code prompts/soulmate/system-v1.st} (페르소나 슬롯) + {@code fewshot-v1.st} (예시 2 개) 를
 * ClassPathResource 로 로딩해 {@link Soulmate} 엔티티의 컬럼을 슬롯에 박는다.
 * 학습용 PoC 시그니처({@link #chat(String, String, String, String)}) 는 {@code @Deprecated} 로 보존한다.</p>
 */
@Service
public class SoulmateChatService {

    private final ChatClient soulmateChatClient;
    private final SoulmateRepository soulmateRepository;
    private final Resource systemV1Resource;
    private final Resource fewshotV1Resource;

    public SoulmateChatService(ChatClient soulmateChatClient,
                               SoulmateRepository soulmateRepository,
                               @Value("classpath:prompts/soulmate/system-v1.st") Resource systemV1Resource,
                               @Value("classpath:prompts/soulmate/fewshot-v1.st") Resource fewshotV1Resource) {
        this.soulmateChatClient = soulmateChatClient;
        this.soulmateRepository = soulmateRepository;
        this.systemV1Resource = systemV1Resource;
        this.fewshotV1Resource = fewshotV1Resource;
    }

    /**
     * Day 3 Step 3 ~ Day 5 Step 5 학습용 PoC 시그니처.
     *
     * <p>도메인을 모르는 깡통 페르소나(`{userName}` · `{mood}` 슬롯) 위에서
     * conversationId 자유도 정책 (UUID 발급) 을 시연하기 위한 자리.
     * ai-friends 의 실제 도메인은 *한 이성친구당 자루 하나* 라 이 시그니처는 prod 에선 쓰이지 않는다.
     * Day 5 Step 5 의 {@link kr.spartaclub.aifriends.chat.controller.SoulmateChatController}
     * 가 학습용 진입점으로 이 메서드를 호출한다.</p>
     *
     * @deprecated Day 5 Step 6 의 {@link #chat(Long, String)} 가 prod 진입점.
     *             학습용 시연을 위해 보존만.
     */
    @Deprecated
    public AiReply chat(String conversationId, String anonymizedUserName, String mood, String userMessage) {
        return soulmateChatClient.prompt()
                .system(system -> system
                        .text("""
                                너는 {userName} 님의 AI 친구야.
                                유저의 현재 기분은 '{mood}' 이야.
                                답변은 3문장 이내로, 반말로 친근하게 해.
                                유저가 이어서 보낼 만한 짧은 답장 후보(choices) 를 2~3개 함께 제안해.
                                이번 한 턴으로 너에 대한 호감도(affectionDelta) 가 -5~+5 사이에서 얼마나 변할지 정수로 추정해.
                                """)
                        .param("userName", anonymizedUserName)
                        .param("mood", mood))
                .user(userMessage)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(AiReply.class);
    }

    /**
     * Day 5 Step 6 — prod 진입점.
     *
     * <p>도메인 정책: 한 이성친구당 자루 하나 → conversationId = {@code String.valueOf(soulmateId)}.
     * system 프롬프트는 Day 3 에서 도입한 외부 파일(system-v1.st + fewshot-v1.st)을 로딩해
     * Soulmate 엔티티의 페르소나 컬럼(gender · name · personalityKeywords · hobbies · speechStyles)을
     * 슬롯에 박는다. 응답 포맷(JSON Schema)은 Day 4 의 BeanOutputConverter 가 자동 주입하므로
     * system-v1.st 에는 # Format 섹션이 없다.</p>
     *
     * @param soulmateId  대화 상대 캐릭터 ID. 자루 식별자도 겸한다.
     * @param userMessage 사용자가 이번 턴에 입력한 메시지
     * @return 캐릭터의 대사 + choices + affectionDelta 가 담긴 record
     */
    public AiReply chat(Long soulmateId, String userMessage) {
        Soulmate soulmate = soulmateRepository.findById(soulmateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SOULMATE_NOT_FOUND));
        String conversationId = String.valueOf(soulmateId);
        String systemText = readResource(systemV1Resource) + "\n\n" + readResource(fewshotV1Resource);

        return soulmateChatClient.prompt()
                .system(system -> system
                        .text(systemText)
                        .param("gender", soulmate.getGender())
                        .param("characterName", soulmate.getName())
                        .param("personality", soulmate.getPersonalityKeywords())
                        .param("hobbies", soulmate.getHobbies())
                        .param("speechStyles", soulmate.getSpeechStyles()))
                .user(userMessage)
                .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationId))
                .call()
                .entity(AiReply.class);
    }

    private String readResource(Resource resource) {
        try {
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("프롬프트 리소스 로딩 실패: " + resource, e);
        }
    }
}
