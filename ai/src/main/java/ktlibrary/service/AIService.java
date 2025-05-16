package ktlibrary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class AIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AIService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * PDF 내용을 기반으로 표지 이미지 생성에 사용할 프롬프트를 생성합니다.
     * @param content 책 내용
     * @return 이미지 생성을 위한 프롬프트
     */
    public String generateCoverImagePrompt(String content) {
        String prompt = "책 내용을 기반으로 표지 이미지를 생성해주세요: " + content.substring(0, Math.min(500, content.length()));
        
        Map<String, Object> requestBody = createChatCompletionRequest(prompt);
        String response = callOpenAI(requestBody);
        
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * 책 내용을 기반으로 카테고리를 분류합니다.
     * @param content 책 내용
     * @return 분류된 카테고리
     */
    public String categorizeContent(String content) {
        String prompt = "다음 책 내용의 장르를 분류해주세요. 소설, 시, 에세이, 자기계발, 역사, 과학, 경제, 철학 중 하나로 답변해주세요: " 
            + content.substring(0, Math.min(1000, content.length()));
        
        Map<String, Object> requestBody = createChatCompletionRequest(prompt);
        String response = callOpenAI(requestBody);
        
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * 책 내용을 요약합니다.
     * @param content 책 내용
     * @return 요약된 내용
     */
    public String summarizeContent(String content) {
        String prompt = "다음 책 내용을 300자 이내로 요약해주세요: " + content.substring(0, Math.min(2000, content.length()));
        
        Map<String, Object> requestBody = createChatCompletionRequest(prompt);
        String response = callOpenAI(requestBody);
        
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        } catch (Exception e) {
            throw new RuntimeException("AI 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * PDF 변환 및 웹 URL 생성을 시뮬레이션하는 메서드
     * @param content 책 내용
     * @return 생성된 웹 URL
     */
    public String convertToPdfAndGenerateWebUrl(String content) {
        // 실제 구현에서는 PDF 변환 서비스와 연동
        // 여기서는 시뮬레이션한 URL 반환
        return "https://kt-library.com/books/" + System.currentTimeMillis();
    }

    /**
     * PDF 경로 생성을 시뮬레이션하는 메서드
     * @param content 책 내용
     * @param imageUrl 표지 이미지 URL
     * @param summary 요약 내용
     * @return 생성된 PDF 경로
     */
    public String generatePdfPath(String content, String imageUrl, String summary) {
        // 실제 구현에서는 PDF 생성 서비스와 연동
        // 여기서는 시뮬레이션한 경로 반환
        return "/storage/pdfs/" + System.currentTimeMillis() + ".pdf";
    }

    private Map<String, Object> createChatCompletionRequest(String prompt) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "gpt-3.5-turbo");
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "당신은 도서 출판을 돕는 AI 비서입니다.");
        
        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);
        
        requestBody.put("messages", List.of(systemMessage, userMessage));
        return requestBody;
    }

    private String callOpenAI(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("OpenAI API 호출 중 오류 발생", e);
        }
    }
} 