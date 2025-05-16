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
import java.util.ArrayList;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AIService {

    @Value("${openai.api.key}")
    private String apiKey;

    @Value("${openai.api.url:https://api.openai.com/v1/chat/completions}")
    private String apiUrl;
    
    @Value("${openai.api.image-url:https://api.openai.com/v1/images/generations}")
    private String imageApiUrl;

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
        System.out.println("[AIService] 표지 이미지 프롬프트 생성 시작");
        String prompt = "책 내용을 기반으로 표지 이미지를 생성해주세요: " + content.substring(0, Math.min(500, content.length()));
        
        Map<String, Object> requestBody = createChatCompletionRequest(prompt);
        System.out.println("[AIService] OpenAI API 호출 중 (프롬프트 생성)");
        String response = callOpenAI(requestBody);
        
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String result = (String) message.get("content");
            System.out.println("[AIService] 생성된 프롬프트: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[AIService] AI 응답 처리 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("AI 응답 처리 중 오류 발생", e);
        }
    }
    
    /**
     * DALL-E API를 사용하여 이미지를 생성하고 URL을 반환합니다.
     * @param prompt 이미지 생성을 위한 프롬프트
     * @return 생성된 이미지의 URL
     */
    public String generateImage(String prompt) {
        System.out.println("[AIService] 이미지 생성 시작");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("n", 1);
        requestBody.put("size", "1024x1024");
        requestBody.put("model", "dall-e-3");
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            System.out.println("[AIService] DALL-E API 호출 중... (URL: " + imageApiUrl + ")");
            ResponseEntity<String> response = restTemplate.postForEntity(imageApiUrl, entity, String.class);
            System.out.println("[AIService] DALL-E API 응답 코드: " + response.getStatusCode());
            
            Map<String, Object> responseMap = objectMapper.readValue(response.getBody(), Map.class);
            List<Map<String, Object>> data = (List<Map<String, Object>>) responseMap.get("data");
            String imageUrl = (String) data.get(0).get("url");
            System.out.println("[AIService] 생성된 이미지 URL 길이: " + imageUrl.length());
            return imageUrl;
        } catch (Exception e) {
            System.err.println("[AIService] OpenAI 이미지 API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("OpenAI 이미지 API 호출 중 오류 발생", e);
        }
    }

    /**
     * 책 내용을 기반으로 카테고리를 분류합니다.
     * @param content 책 내용
     * @return 분류된 카테고리
     */
    public String categorizeContent(String content) {
        System.out.println("[AIService] 카테고리 분류 시작");
        String prompt = "다음 책 내용의 장르를 분류해주세요. 소설, 시, 에세이, 자기계발, 역사, 과학, 경제, 철학 중 하나로 답변해주세요: " 
            + content.substring(0, Math.min(1000, content.length()));
        
        Map<String, Object> requestBody = createChatCompletionRequest(prompt);
        System.out.println("[AIService] OpenAI API 호출 중 (카테고리 분류)");
        String response = callOpenAI(requestBody);
        
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String result = (String) message.get("content");
            System.out.println("[AIService] 분류된 카테고리: " + result);
            return result;
        } catch (Exception e) {
            System.err.println("[AIService] AI 응답 처리 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("AI 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * 책 내용을 요약합니다.
     * @param content 책 내용
     * @return 요약된 내용
     */
    public String summarizeContent(String content) {
        System.out.println("[AIService] 내용 요약 시작");
        String prompt = "다음 책 내용을 300자 이내로 요약해주세요: " + content.substring(0, Math.min(2000, content.length()));
        
        Map<String, Object> requestBody = createChatCompletionRequest(prompt);
        System.out.println("[AIService] OpenAI API 호출 중 (내용 요약)");
        String response = callOpenAI(requestBody);
        
        try {
            Map<String, Object> responseMap = objectMapper.readValue(response, Map.class);
            List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            String result = (String) message.get("content");
            System.out.println("[AIService] 요약 결과 길이: " + result.length() + "자");
            return result;
        } catch (Exception e) {
            System.err.println("[AIService] AI 응답 처리 중 오류 발생: " + e.getMessage());
            throw new RuntimeException("AI 응답 처리 중 오류 발생", e);
        }
    }

    /**
     * PDF 변환 및 웹 URL 생성을 시뮬레이션하는 메서드
     * @param content 책 내용
     * @return 생성된 웹 URL
     */
    public String convertToPdfAndGenerateWebUrl(String content) {
        System.out.println("[AIService] PDF 변환 및 웹 URL 생성 시작");
        // 실제 구현에서는 PDF 변환 서비스와 연동
        // 여기서는 시뮬레이션한 URL 반환
        String url = "https://kt-library.com/books/" + System.currentTimeMillis();
        System.out.println("[AIService] 생성된 웹 URL: " + url);
        return url;
    }

    /**
     * PDF 경로 생성을 시뮬레이션하는 메서드
     * @param content 책 내용
     * @param imageUrl 표지 이미지 URL
     * @param summary 요약 내용
     * @return 생성된 PDF 경로
     */
    public String generatePdfPath(String content, String imageUrl, String summary) {
        System.out.println("[AIService] PDF 경로 생성 시작");
        // 실제 구현에서는 PDF 생성 서비스와 연동
        // 여기서는 시뮬레이션한 경로 반환
        String path = "/storage/pdfs/" + System.currentTimeMillis() + ".pdf";
        System.out.println("[AIService] 생성된 PDF 경로: " + path);
        return path;
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
        
        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(systemMessage);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        return requestBody;
    }

    private String callOpenAI(Map<String, Object> requestBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        try {
            ResponseEntity<String> response = restTemplate.postForEntity(apiUrl, entity, String.class);
            System.out.println("[AIService] OpenAI API 응답 코드: " + response.getStatusCode());
            return response.getBody();
        } catch (Exception e) {
            System.err.println("[AIService] OpenAI API 호출 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("OpenAI API 호출 중 오류 발생", e);
        }
    }
} 