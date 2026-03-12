package kr.spartaclub.aifriends.controller;

import jakarta.validation.Valid;
import kr.spartaclub.aifriends.common.response.ApiResponse;
import kr.spartaclub.aifriends.dto.ChatLogResponse;
import kr.spartaclub.aifriends.dto.SoulmateCreateRequest;
import kr.spartaclub.aifriends.dto.SoulmateProfileResponse;
import kr.spartaclub.aifriends.dto.SoulmateResponse;
import kr.spartaclub.aifriends.service.ChatLogService;
import kr.spartaclub.aifriends.service.SoulmateService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * 이성친구(Soulmate) 관련 REST API 엔드포인트를 제공하는 컨트롤러입니다.
 * 이 프로젝트는 SPA(Single Page Application) 구조이므로 모든 응답을 JSON 형태로 반환합니다.
 */
@RestController // @Controller + @ResponseBody 기능. 반환값이 모두 JSON 객체로 직렬화됩니다.
@RequestMapping("/api/soulmates") // 이 컨트롤러의 모든 API 기본 경로를 설정합니다.
@RequiredArgsConstructor // final 필드에 대한 생성자를 자동 생성하여 의존성을 주입(DI)받습니다.
public class SoulmateController {

    private final SoulmateService soulmateService;
    private final ChatLogService chatLogService;

    /**
     * [이성친구 생성 API]
     * 사용자가 입력한 정보를 바탕으로 새로운 이성친구를 생성합니다.
     * 
     * @param request @Valid를 통해 DTO의 @NotBlank, @NotEmpty 제약 조건을 어노테이션 기반으로 검증합니다.
     *                요청의 본문(Body) 데이터는 @RequestBody를 통해 객체로 변환됩니다.
     * @return 201 Created 상태 코드와 생성된 데이터, 그리고 헤더에 리소스를 조회할 수 있는 Location URI를 함께 반환합니다.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SoulmateResponse>> createSoulmate(@Valid @RequestBody SoulmateCreateRequest request) {
        SoulmateResponse response = soulmateService.createSoulmate(request);
        
        // 생성된 리소스의 접근 경로(URI)를 명시적으로 만드는 패턴 (RESTful 모범 사례)
        // 예: http://localhost:8080/api/soulmates/1
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();
                
        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    /**
     * [이성친구 단건 (프로필) 조회 API]
     * 특정 PK(id)를 가진 이성친구의 상세 프로필(호감도, 레벨, 뱃지 포함)을 조회합니다.
     * 
     * @param id URI 경로(path)에 포함된 자원 고유 번호를 @PathVariable로 추출합니다.
     * @return 200 OK 상태 코드와 함께 프로필 데이터를 담은 공통 응답 포맷(ApiResponse) 반환
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SoulmateProfileResponse>> getSoulmate(@PathVariable Long id) {
        SoulmateProfileResponse response = soulmateService.getSoulmate(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * [전체 이성친구 목록 조회 API]
     * 생성된 이성친구 목록을 조회합니다.
     * SPA 화면 분기 시 "선택 화면" 등에서 활용될 수 있습니다.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, List<SoulmateResponse>>>> getSoulmates() {
        List<SoulmateResponse> soulmates = soulmateService.getSoulmates();
        // {"soulmates": [...]} 형태로 응답 객체를 래핑하여 전달
        return ResponseEntity.ok(ApiResponse.success(Map.of("soulmates", soulmates)));
    }

    /**
     * [이성친구 대화 기록 조회 API]
     * 특정 이성친구와의 과거 채팅 내역을 무한 스크롤(Slice) 방식으로 조회합니다.
     * 
     * @param id 조회할 이성친구의 고유 번호
     * @param pageable 페이징 정보 (기본값: 최신순 50건)
     * @return 200 OK 와 함께 대화 기록 내역(Slice) 반환
     */
    @GetMapping("/{id}/chat/logs")
    public ResponseEntity<ApiResponse<Slice<ChatLogResponse>>> getChatLogs(
            @PathVariable Long id,
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        
        Slice<ChatLogResponse> chatLogs = chatLogService.getChatLogs(id, pageable);
        return ResponseEntity.ok(ApiResponse.success(chatLogs));
    }
}
