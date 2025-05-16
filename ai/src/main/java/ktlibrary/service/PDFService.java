package ktlibrary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Normalizer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@Service
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    
    // 기본 폰트 정의 (한글을 지원하지 않지만 안정적인 PDType1Font 사용)
    private static final PDType1Font NORMAL_FONT = PDType1Font.HELVETICA;
    private static final PDType1Font BOLD_FONT = PDType1Font.HELVETICA_BOLD;
    
    private static final float TITLE_FONT_SIZE = 24;
    private static final float HEADING1_FONT_SIZE = 18;
    private static final float HEADING2_FONT_SIZE = 16;
    private static final float NORMAL_FONT_SIZE = 11;
    private static final float SMALL_FONT_SIZE = 10;
    private static final float LEADING = 14.5f;
    
    @Value("${app.storage.path:./storage}")
    private String storagePath;

    @PostConstruct
    public void init() {
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            if (storagePath == null || storagePath.trim().isEmpty()) {
                storagePath = "./storage"; // 기본값 설정
                System.out.println("[PDFService] 스토리지 경로가 설정되지 않아 기본값으로 설정: " + storagePath);
            }
            
            // 루트 스토리지 디렉토리 생성
            Path rootDir = Paths.get(storagePath);
            if (!Files.exists(rootDir)) {
                Files.createDirectories(rootDir);
                System.out.println("[PDFService] 루트 스토리지 디렉토리 생성: " + rootDir.toAbsolutePath());
            }
            
            // PDF 저장소 디렉토리 생성
            Path pdfDir = Paths.get(storagePath, "pdfs");
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
                System.out.println("[PDFService] PDF 디렉토리 생성: " + pdfDir.toAbsolutePath());
            }
        } catch (Exception e) {
            System.err.println("[PDFService] 스토리지 디렉토리 생성 실패: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * 책 내용, 이미지, 요약을 기반으로 PDF를 생성하고 경로를 반환합니다.
     * @param content 책 내용
     * @param imageUrl 표지 이미지 URL
     * @param summary 요약 내용
     * @param bookName 책 제목
     * @return 생성된 PDF 경로
     */
    public String generatePdf(String content, String imageUrl, String summary, String bookName) {
        System.out.println("[PDFService] PDF 생성 시작: " + bookName);
        
        try {
            // 경로 확인 및 재설정
            if (storagePath == null || storagePath.trim().isEmpty()) {
                storagePath = "./storage";
                System.out.println("[PDFService] 스토리지 경로가 설정되지 않아 기본값으로 설정: " + storagePath);
            }
            
            // PDF 디렉토리 확인 및 생성
            Path pdfDir = Paths.get(storagePath, "pdfs");
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
                System.out.println("[PDFService] PDF 디렉토리 생성: " + pdfDir.toAbsolutePath());
            }
            
            // 파일명 생성 (책 제목 기반)
            String safeFileName = createSafeFileName(bookName);
            String timestamp = String.valueOf(System.currentTimeMillis());
            String fileName = safeFileName + "_" + timestamp + ".pdf";
            
            Path pdfPath = pdfDir.resolve(fileName);
            
            // PDF 생성
            createPdfDocument(pdfPath, content, imageUrl, summary, bookName);
            
            return pdfPath.toAbsolutePath().toString();
        } catch (IOException e) {
            System.err.println("[PDFService] PDF 생성 중 오류 발생: " + e.getMessage());
            e.printStackTrace();
            
            // 오류 발생 시 백업으로 텍스트 파일 생성
            try {
                String safeFileName = createSafeFileName(bookName);
                String timestamp = String.valueOf(System.currentTimeMillis());
                String errorFileName = safeFileName + "_error_" + timestamp + ".txt";
                
                Path textFilePath = Paths.get(storagePath, "pdfs", errorFileName);
                String errorContent = "PDF 생성 중 오류 발생\n\n"
                        + "제목: " + bookName + "\n\n"
                        + "이미지 URL: " + imageUrl + "\n\n"
                        + "요약:\n" + summary + "\n\n"
                        + "내용:\n" + content;
                Files.writeString(textFilePath, errorContent, StandardCharsets.UTF_8);
                return textFilePath.toAbsolutePath().toString();
            } catch (IOException textError) {
                System.err.println("[PDFService] 백업 텍스트 파일 생성 실패: " + textError.getMessage());
                return "/storage/pdfs/error.txt";
            }
        }
    }
    
    /**
     * 안전한 파일명을 생성합니다.
     */
    private String createSafeFileName(String bookName) {
        if (bookName == null || bookName.trim().isEmpty()) {
            return "unnamed_book";
        }
        
        // 파일명에 사용할 수 없는 문자 제거
        String normalized = Normalizer.normalize(bookName, Normalizer.Form.NFD);
        String safe = normalized.replaceAll("[^a-zA-Z0-9가-힣ㄱ-ㅎㅏ-ㅣ\\s]", "_")
                               .replaceAll("\\s+", "_")
                               .trim();
        
        // 파일명 길이 제한 (50자)
        if (safe.length() > 50) {
            safe = safe.substring(0, 50);
        }
        
        return safe;
    }
    
    /**
     * PDF 문서를 생성합니다.
     */
    private void createPdfDocument(Path pdfPath, String content, String imageUrl, String summary, String bookName) throws IOException {
        try (PDDocument document = new PDDocument()) {
            // 1페이지: 표지 이미지
            createCoverImagePage(document, imageUrl, bookName);
            
            // 2페이지: 요약 내용
            createSummaryPage(document, summary, bookName);
            
            // 3페이지 이후: 책 내용
            createContentPages(document, content);
            
            // PDF 저장
            document.save(pdfPath.toFile());
            logger.info("[PDFService] PDF 파일 생성 완료: {}", pdfPath.toAbsolutePath());
        }
    }

    /**
     * 표지 이미지 페이지를 생성합니다 (1페이지).
     */
    private void createCoverImagePage(PDDocument document, String imageUrl, String bookName) throws IOException {
        PDPage coverPage = new PDPage(PDRectangle.A4);
        document.addPage(coverPage);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, coverPage);
        try {
            // 페이지 크기 가져오기
            float width = coverPage.getMediaBox().getWidth();
            float height = coverPage.getMediaBox().getHeight();
            
            // 제목 추가
            contentStream.beginText();
            contentStream.setFont(BOLD_FONT, TITLE_FONT_SIZE);
            
            // 변환된 제목 가져오기
            String displayTitle = safeText(bookName, BOLD_FONT);
            
            // 제목 길이에 따라 가운데 정렬 계산
            float titleWidth = BOLD_FONT.getStringWidth(displayTitle) / 1000 * TITLE_FONT_SIZE;
            float titleX = (width - titleWidth) / 2;
            if (titleX < 50) titleX = 50;
            
            contentStream.newLineAtOffset(titleX, height - 50);
            try {
                contentStream.showText(displayTitle);
                
                // 원래 한글 제목이 로마자로 변환되었으면, 하단에 원제를 English Title이라고 표시
                if (hasKorean(bookName)) {
                    contentStream.endText();
                    contentStream.beginText();
                    contentStream.setFont(NORMAL_FONT, SMALL_FONT_SIZE);
                    contentStream.newLineAtOffset(titleX, height - 80);
                    contentStream.showText("(영어로 변환된 제목입니다)");
                }
            } catch (Exception e) {
                logger.error("[PDFService] 제목 텍스트 렌더링 실패: {}", e.getMessage());
                contentStream.showText("Book Cover");
            }
            contentStream.endText();  // 명시적으로 endText() 호출
            
            // 이미지 추가 시도
            try {
                if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.contains("default-cover")) {
                    // 원격 이미지 다운로드
                    logger.info("[PDFService] 표지 이미지 다운로드 시도: {}", imageUrl);
                    URL url = new URL(imageUrl);
                    PDImageXObject image = PDImageXObject.createFromByteArray(document, downloadImage(url), "Cover Image");

                    // 이미지 크기 조정 (페이지의 80% 크기로 확대)
                    float imgWidth = width * 0.8f;
                    float imgHeight = (imgWidth / image.getWidth()) * image.getHeight();
                    
                    // 이미지가 너무 크면 높이 제한
                    if (imgHeight > height * 0.7f) {
                        imgHeight = height * 0.7f;
                        imgWidth = (imgHeight / image.getHeight()) * image.getWidth();
                    }
                    
                    // 이미지를 페이지 중앙에 배치
                    float imgX = (width - imgWidth) / 2;
                    float imgY = (height - 100 - imgHeight) / 2;
                    
                    contentStream.drawImage(image, imgX, imgY, imgWidth, imgHeight);
                    logger.info("[PDFService] 표지 이미지 추가 완료");
                } else {
                    // 이미지 없음 텍스트 표시
                    contentStream.beginText();
                    contentStream.setFont(BOLD_FONT, 20);
                    contentStream.newLineAtOffset((width - 200) / 2, height / 2);
                    contentStream.showText(safeText("표지 이미지 없음", BOLD_FONT));
                    contentStream.endText();  // 명시적으로 endText() 호출
                }
            } catch (Exception e) {
                logger.error("[PDFService] 표지 이미지 추가 실패: {}", e.getMessage());
                
                // 이미지 추가 실패 시 텍스트로 대체
                contentStream.beginText();
                contentStream.setFont(BOLD_FONT, 20);
                contentStream.newLineAtOffset((width - 200) / 2, height / 2);
                contentStream.showText(safeText("표지 이미지 로드 실패", BOLD_FONT));
                contentStream.endText();  // 명시적으로 endText() 호출
            }
            
            // 페이지 하단에 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(NORMAL_FONT, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(width / 2 - 10, 30);
            contentStream.showText("1");
            contentStream.endText();  // 명시적으로 endText() 호출
        } finally {
            // 항상 contentStream 닫기
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
    
    /**
     * 요약 페이지를 생성합니다 (2페이지).
     */
    private void createSummaryPage(PDDocument document, String summary, String bookName) throws IOException {
        PDPage summaryPage = new PDPage(PDRectangle.A4);
        document.addPage(summaryPage);
        
        PDPageContentStream contentStream = new PDPageContentStream(document, summaryPage);
        try {
            // 페이지 크기 가져오기
            float width = summaryPage.getMediaBox().getWidth();
            float height = summaryPage.getMediaBox().getHeight();
            float margin = 50;
            float yStart = height - margin;
            float yPosition = yStart;
            
            // 제목 추가
            contentStream.beginText();
            contentStream.setFont(BOLD_FONT, HEADING1_FONT_SIZE);
            contentStream.newLineAtOffset(margin, yPosition);
            try {
                String summaryTitle = safeText(bookName + " - 요약", BOLD_FONT);
                contentStream.showText(summaryTitle);
            } catch (Exception e) {
                logger.error("[PDFService] 요약 제목 텍스트 렌더링 실패: {}", e.getMessage());
                contentStream.showText("Summary");
            }
            contentStream.endText();  // 명시적으로 endText() 호출
            
            yPosition -= 40;
            
            // 구분선 추가
            contentStream.setLineWidth(1f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(width - margin, yPosition);
            contentStream.stroke();
            
            yPosition -= 30;
            
            // 요약 내용 추가
            contentStream.beginText();
            contentStream.setFont(NORMAL_FONT, NORMAL_FONT_SIZE);
            contentStream.setLeading(LEADING);
            contentStream.newLineAtOffset(margin, yPosition);
            
            // 텍스트 줄바꿈 처리 (로마자로 변환된 텍스트 사용)
            if (summary != null && !summary.isEmpty()) {
                try {
                    // 요약 내용 전체를 로마자로 변환
                    String convertedSummary = safeText(summary, NORMAL_FONT);
                    
                    // 적절한 길이로 나누어 줄바꿈 처리
                    int lineLength = 80; // 영문 기준 한 줄 길이
                    for (int i = 0; i < convertedSummary.length(); i += lineLength) {
                        int endIndex = Math.min(i + lineLength, convertedSummary.length());
                        String line = convertedSummary.substring(i, endIndex);
                        contentStream.showText(line);
                        contentStream.newLine();
                    }
                } catch (Exception e) {
                    logger.warn("[PDFService] 텍스트 렌더링 실패: {}", e.getMessage());
                    contentStream.showText("Summary text rendering failed.");
                }
            } else {
                try {
                    contentStream.showText(safeText("요약 내용이 없습니다.", NORMAL_FONT));
                } catch (Exception e) {
                    contentStream.showText("No summary available.");
                }
            }
            
            contentStream.endText();  // 명시적으로 endText() 호출
            
            // 페이지 하단에 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(NORMAL_FONT, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(width / 2 - 10, 30);
            contentStream.showText("2");
            contentStream.endText();  // 명시적으로 endText() 호출
        } finally {
            // 항상 contentStream 닫기
            if (contentStream != null) {
                contentStream.close();
            }
        }
    }
    
    /**
     * 책 내용 페이지를 생성합니다 (3페이지 이후).
     */
    private void createContentPages(PDDocument document, String content) throws IOException {
        if (content == null || content.isEmpty()) {
            logger.info("[PDFService] 내용이 없어 내용 페이지를 생성하지 않습니다.");
            return;
        }
        
        // 첫 내용 페이지 추가
        PDPage contentPage = new PDPage(PDRectangle.A4);
        document.addPage(contentPage);
        
        // 이미 2페이지가 있으므로 내용은 3페이지부터 시작
        int pageCount = 3;
        
        PDPageContentStream contentStream = null;
        float yPosition = 0;
        float width = 0;
        float height = 0;
        float margin = 50;
        
        try {
            contentStream = new PDPageContentStream(document, contentPage);
            
            // 페이지 크기 가져오기
            width = contentPage.getMediaBox().getWidth();
            height = contentPage.getMediaBox().getHeight();
            float yStart = height - margin;
            yPosition = yStart;
            
            // 제목 추가
            contentStream.beginText();
            contentStream.setFont(BOLD_FONT, HEADING2_FONT_SIZE);
            contentStream.newLineAtOffset(margin, yPosition);
            try {
                String contentTitle = safeText("책 내용", BOLD_FONT);
                contentStream.showText(contentTitle);
            } catch (Exception e) {
                contentStream.showText("Content");
            }
            contentStream.endText();  // 명시적으로 endText() 호출
            
            // 구분선 추가
            yPosition -= 20;
            contentStream.setLineWidth(1f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(width - margin, yPosition);
            contentStream.stroke();
            
            yPosition -= 30; // 제목과 내용 사이 간격
            
            // 내용 추가 - 새로운 텍스트 블록 시작
            contentStream.beginText();
            contentStream.setFont(NORMAL_FONT, NORMAL_FONT_SIZE);
            contentStream.setLeading(LEADING);
            contentStream.newLineAtOffset(margin, yPosition);
            
            // 내용 전체를 로마자로 변환
            String convertedContent = safeText(content, NORMAL_FONT);
            
            // 라인 단위로 분리
            String[] lines = convertedContent.split("\n");
            boolean needNewPage = false;
            
            int lineLength = 80; // 영문 기준 한 줄 길이
            
            // 각 라인을 처리
            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                
                // 현재 y 위치가 하단 여백보다 작으면 새 페이지 생성
                if (yPosition < margin + 50 || needNewPage) {
                    // 현재 텍스트 블록 종료
                    contentStream.endText();  // 명시적으로 endText() 호출
                    
                    // 페이지 번호 추가
                    contentStream.beginText();
                    contentStream.setFont(NORMAL_FONT, SMALL_FONT_SIZE);
                    contentStream.newLineAtOffset(width / 2 - 10, 30);
                    contentStream.showText(String.valueOf(pageCount));
                    contentStream.endText();  // 명시적으로 endText() 호출
                    
                    // 현재 스트림 종료
                    contentStream.close();
                    
                    // 새 페이지 생성
                    contentPage = new PDPage(PDRectangle.A4);
                    document.addPage(contentPage);
                    pageCount++;
                    
                    // 새 스트림 생성
                    contentStream = new PDPageContentStream(document, contentPage);
                    
                    // 새 텍스트 블록 시작
                    contentStream.beginText();
                    contentStream.setFont(NORMAL_FONT, NORMAL_FONT_SIZE);
                    contentStream.setLeading(LEADING);
                    contentStream.newLineAtOffset(margin, yStart);
                    yPosition = yStart;
                    needNewPage = false;
                }
                
                // 빈 라인 처리
                if (line.trim().isEmpty()) {
                    contentStream.newLine();
                    yPosition -= LEADING;
                    continue;
                }
                
                // 라인 길이가 긴 경우 적절히 분할
                for (int pos = 0; pos < line.length(); pos += lineLength) {
                    int end = Math.min(pos + lineLength, line.length());
                    String segment = line.substring(pos, end);
                    
                    try {
                        contentStream.showText(segment);
                        contentStream.newLine();
                        yPosition -= LEADING;
                        
                        // 페이지 넘김 체크
                        if (yPosition < margin + 50) {
                            needNewPage = true;
                            break;
                        }
                    } catch (Exception e) {
                        logger.warn("[PDFService] 내용 텍스트 렌더링 실패: {}", e.getMessage());
                    }
                }
                
                // 페이지 넘김이 필요하면 다음 라인으로 넘어감
                if (needNewPage) continue;
                
                // 문단 사이 줄바꿈
                contentStream.newLine();
                yPosition -= LEADING;
                
                // 페이지 넘김 체크
                if (yPosition < margin + 50 && i < lines.length - 1) {
                    needNewPage = true;
                }
            }
            
            // 마지막 텍스트 블록 종료
            contentStream.endText();  // 명시적으로 endText() 호출
            
            // 마지막 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(NORMAL_FONT, SMALL_FONT_SIZE);
            contentStream.newLineAtOffset(width / 2 - 10, 30);
            contentStream.showText(String.valueOf(pageCount));
            contentStream.endText();  // 명시적으로 endText() 호출
            
        } finally {
            // 항상 contentStream 닫기
            if (contentStream != null) {
                try {
                    contentStream.close();
                } catch (IOException e) {
                    logger.error("[PDFService] 컨텐츠 스트림 닫기 오류: {}", e.getMessage());
                }
            }
        }
        
        logger.info("[PDFService] PDF 생성 완료: 총 {} 페이지", pageCount);
    }
    
    /**
     * URL에서 이미지를 다운로드합니다.
     */
    private byte[] downloadImage(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }
    
    /**
     * 한글을 포함한 텍스트를 기본 폰트에서도 표시 가능하도록 변환합니다.
     * 한글 문자를 로마자(영문) 문자열로 대체합니다.
     */
    private String safeText(String text, PDType1Font font) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        // 한글 자음/모음에 대응하는 알파벳으로 변환
        StringBuilder result = new StringBuilder();
        
        for (char c : text.toCharArray()) {
            if (c >= '가' && c <= '힣') {
                // 한글을 로마자로 간단히 변환
                result.append(koreanToRoman(c));
            } else if (c > 127) {
                // 기타 유니코드 문자는 '*'로 대체
                result.append('*');
            } else {
                // ASCII 범위 문자는 그대로 사용
                result.append(c);
            }
        }
        
        return result.toString();
    }
    
    /**
     * 한글 한 글자를 로마자로 변환
     */
    private String koreanToRoman(char c) {
        // 한글 초성, 중성, 종성 분리
        int syllableIndex = c - '가';
        
        // 초성 인덱스 (0-18)
        int cho = syllableIndex / (21 * 28);
        // 중성 인덱스 (0-20)
        int jung = (syllableIndex % (21 * 28)) / 28;
        // 종성 인덱스 (0-27), 0은 종성 없음
        int jong = syllableIndex % 28;
        
        // 초성 로마자 변환 (가나다라...)
        String[] choToRoman = {
            "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", 
            "", "j", "jj", "ch", "k", "t", "p", "h"
        };
        
        // 중성 로마자 변환 (ㅏㅐㅑㅒ...)
        String[] jungToRoman = {
            "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", 
            "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
        };
        
        // 종성 로마자 변환 (없음, ㄱ, ㄲ, ㄳ...)
        String[] jongToRoman = {
            "", "k", "k", "k", "n", "n", "n", "t", "l", "l", "l", "l", "l", "l", "l", "l", 
            "p", "p", "t", "t", "t", "t", "t", "t", "t", "k", "t", "p"
        };
        
        StringBuilder roman = new StringBuilder();
        
        // 초성 + 중성 + 종성 결합하여 로마자 반환
        if (cho >= 0 && cho < choToRoman.length) {
            roman.append(choToRoman[cho]);
        }
        
        if (jung >= 0 && jung < jungToRoman.length) {
            roman.append(jungToRoman[jung]);
        }
        
        if (jong > 0 && jong < jongToRoman.length) {
            roman.append(jongToRoman[jong]);
        }
        
        return roman.toString();
    }

    private boolean hasKorean(String text) {
        return text.matches(".*[가-힣].*");
    }
} 