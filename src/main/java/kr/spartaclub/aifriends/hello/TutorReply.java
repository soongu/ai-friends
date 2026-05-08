package kr.spartaclub.aifriends.hello;

import java.util.List;

/**
 * Day 4 과제 1 — {@code /api/hello-ai/v3} 의 구조화 응답 record.
 *
 * <p>Day 3 끝 시점까지는 {@code (topicTag, reply)} 두 String 필드로 LLM 의 평문 응답을 그대로 담아내는
 * 구조였다. Day 4 과제 1 에서 BeanOutputConverter 를 도입하면서 시그니처를 LLM 이 진짜 채우는
 * {@code (answer, suggestedQuestions)} 로 진화시켰다 — Spring AI 가 이 record 를 분석해 JSON Schema 를
 * 자동 생성하고, 사용자 프롬프트 끝에 format 지시문을 자동 주입한다.</p>
 *
 * <p>시스템 프롬프트({@code prompts/hello/tutor-v3-structured.st}) 가 각 필드의 의미를 자연어로 알려주고,
 * 이 record 가 형식을 강제한다 — record 가 형식, 시스템 프롬프트가 내용. 한 쪽이라도 빠지면 LLM 이
 * 빈 배열로 채워버리는 흔한 실패 모드가 발생한다.</p>
 *
 * @param answer             튜터의 답변 본문 (3~6 문장, 평문)
 * @param suggestedQuestions 학생이 이어서 던질 만한 짧은 후속 질문 후보 (보통 2~3 개)
 */
public record TutorReply(
        String answer,
        List<String> suggestedQuestions
) { }
