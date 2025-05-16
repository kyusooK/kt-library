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
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

@Service
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    
    // 폰트 정의 (PDType1Font는 한글을 지원하지 않으므로 제거)
    private PDType0Font koreanFont;
    private PDType0Font koreanBoldFont;
    
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
            // 폰트 로드
            try {
                // PDFBox에 포함된 기본 폰트를 사용
                InputStream fontStream = PDFService.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");
                if (fontStream != null) {
                    koreanFont = PDType0Font.load(document, fontStream);
                    fontStream.close();
                    
                    fontStream = PDFService.class.getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Bold.ttf");
                    if (fontStream != null) {
                        koreanBoldFont = PDType0Font.load(document, fontStream);
                        fontStream.close();
                    } else {
                        koreanBoldFont = koreanFont; // 볼드 폰트가 없으면 일반 폰트로 대체
                    }
                    
                    logger.info("[PDFService] Liberation Sans 폰트 로드 완료");
                } else {
                    logger.error("[PDFService] PDFBox 내장 폰트를 찾을 수 없습니다.");
                    throw new IOException("PDFBox 내장 폰트를 찾을 수 없습니다.");
                }
            } catch (Exception e) {
                logger.error("[PDFService] 폰트 초기화 오류: {}", e.getMessage());
                throw new IOException("폰트 초기화 실패: " + e.getMessage(), e);
            }
            
            // 1페이지: 표지 이미지
            createCoverImagePage(document, imageUrl, bookName);
            
            // 2페이지: 요약 내용
            createSummaryPage(document, summary, bookName);
            
            // 3페이지 이후: 책 내용
            createContentPages(document, content);
            
            // PDF 저장
            document.save(pdfPath.toFile());
            System.out.println("[PDFService] PDF 파일 생성 완료: " + pdfPath.toAbsolutePath());
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
            
            // 제목 추가 (한글 폰트 사용)
            contentStream.beginText();
            contentStream.setFont(koreanBoldFont, TITLE_FONT_SIZE);
            
            // 제목 가운데 정렬을 위한 계산 (한글은 약 14pt 너비로 계산)
            float titleWidth = bookName.length() * 14; // 대략적인 제목 너비
            float titleX = (width - titleWidth) / 2;
            if (titleX < 50) titleX = 50;
            
            contentStream.newLineAtOffset(titleX, height - 50);
            try {
                contentStream.showText(safeText(bookName, koreanBoldFont));
            } catch (Exception e) {
                // 폰트 관련 오류가 발생한 경우 기본 텍스트 표시
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
                    contentStream.setFont(koreanBoldFont, 20);
                    contentStream.newLineAtOffset((width - 200) / 2, height / 2);
                    contentStream.showText(safeText("표지 이미지 없음", koreanBoldFont));
                    contentStream.endText();  // 명시적으로 endText() 호출
                }
            } catch (Exception e) {
                logger.error("[PDFService] 표지 이미지 추가 실패: {}", e.getMessage());
                
                // 이미지 추가 실패 시 텍스트로 대체
                contentStream.beginText();
                contentStream.setFont(koreanBoldFont, 20);
                contentStream.newLineAtOffset((width - 200) / 2, height / 2);
                contentStream.showText(safeText("표지 이미지 로드 실패", koreanBoldFont));
                contentStream.endText();  // 명시적으로 endText() 호출
            }
            
            // 페이지 하단에 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(koreanFont, SMALL_FONT_SIZE);
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
            
            // 제목 추가 (한글 폰트 사용)
            contentStream.beginText();
            contentStream.setFont(koreanBoldFont, HEADING1_FONT_SIZE);
            contentStream.newLineAtOffset(margin, yPosition);
            try {
                contentStream.showText(safeText(bookName + " - 요약", koreanBoldFont));
            } catch (Exception e) {
                // 한글 문자가 지원되지 않는 경우 대체 텍스트 사용
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
            
            // 요약 내용 추가 (한글 폰트 사용)
            contentStream.beginText();
            contentStream.setFont(koreanFont, NORMAL_FONT_SIZE);
            contentStream.setLeading(LEADING);
            contentStream.newLineAtOffset(margin, yPosition);
            
            // 텍스트 줄바꿈 처리 (한글 텍스트는 고정 길이로 처리)
            if (summary != null && !summary.isEmpty()) {
                String[] paragraphs = summary.split("\n");
                for (String paragraph : paragraphs) {
                    if (paragraph.trim().isEmpty()) {
                        contentStream.newLine();
                        continue;
                    }
                    
                    // 한글 처리를 위해 한 글자씩 처리
                    int charCount = 0;
                    StringBuilder line = new StringBuilder();
                    char[] chars = paragraph.toCharArray();
                    
                    for (int i = 0; i < chars.length; i++) {
                        line.append(chars[i]);
                        charCount++;
                        
                        // 한 줄에 약 60자 (한글 기준)
                        if (charCount >= 60) {
                            try {
                                contentStream.showText(safeText(line.toString(), koreanFont));
                            } catch (Exception e) {
                                logger.warn("[PDFService] 텍스트 렌더링 실패: {}", e.getMessage());
                            }
                            contentStream.newLine();
                            line = new StringBuilder();
                            charCount = 0;
                        }
                    }
                    
                    if (line.length() > 0) {
                        try {
                            contentStream.showText(safeText(line.toString(), koreanFont));
                        } catch (Exception e) {
                            logger.warn("[PDFService] 텍스트 렌더링 실패: {}", e.getMessage());
                        }
                        contentStream.newLine();
                    }
                    
                    contentStream.newLine(); // 문단 사이 줄바꿈
                }
            } else {
                try {
                    contentStream.showText(safeText("요약 내용이 없습니다.", koreanFont));
                } catch (Exception e) {
                    contentStream.showText("No summary available.");
                }
            }
            
            contentStream.endText();  // 명시적으로 endText() 호출
            
            // 페이지 하단에 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(koreanFont, SMALL_FONT_SIZE);
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
            
            // 제목 추가 (한글 폰트 사용)
            contentStream.beginText();
            contentStream.setFont(koreanBoldFont, HEADING2_FONT_SIZE);
            contentStream.newLineAtOffset(margin, yPosition);
            try {
                contentStream.showText(safeText("책 내용", koreanBoldFont));
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
            
            // 내용 추가 - 새로운 텍스트 블록 시작 (한글 폰트 사용)
            contentStream.beginText();
            contentStream.setFont(koreanFont, NORMAL_FONT_SIZE);
            contentStream.setLeading(LEADING);
            contentStream.newLineAtOffset(margin, yPosition);
            
            // 텍스트 줄바꿈 처리
            String[] paragraphs = content.split("\n");
            boolean needNewPage = false;
            
            for (int i = 0; i < paragraphs.length; i++) {
                String paragraph = paragraphs[i];
                
                // 현재 y 위치가 하단 여백보다 작으면 새 페이지 생성
                if (yPosition < margin + 50 || needNewPage) {
                    // 현재 텍스트 블록 종료
                    contentStream.endText();  // 명시적으로 endText() 호출
                    
                    // 페이지 번호 추가
                    contentStream.beginText();
                    contentStream.setFont(koreanFont, SMALL_FONT_SIZE);
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
                    contentStream.setFont(koreanFont, NORMAL_FONT_SIZE);
                    contentStream.setLeading(LEADING);
                    contentStream.newLineAtOffset(margin, yStart);
                    yPosition = yStart;
                    needNewPage = false;
                }
                
                if (paragraph.trim().isEmpty()) {
                    contentStream.newLine();
                    yPosition -= LEADING;
                    continue;
                }
                
                // 한글 처리를 위해 한 글자씩 처리
                int charPosition = 0;
                StringBuilder line = new StringBuilder();
                char[] chars = paragraph.toCharArray();
                
                for (int j = 0; j < chars.length; j++) {
                    line.append(chars[j]);
                    charPosition++;
                    
                    // 적당한 길이마다 줄바꿈 (한글은 약 60자 정도)
                    if (charPosition >= 60) {
                        try {
                            contentStream.showText(safeText(line.toString(), koreanFont));
                        } catch (Exception e) {
                            logger.warn("[PDFService] 내용 텍스트 렌더링 실패: {}", e.getMessage());
                        }
                        contentStream.newLine();
                        yPosition -= LEADING;
                        line = new StringBuilder();
                        charPosition = 0;
                        
                        // 페이지 넘김 체크
                        if (yPosition < margin + 50) {
                            needNewPage = true;
                            break;
                        }
                    }
                }
                
                // 현재 단락에서 페이지 넘김이 필요한 경우 다음 반복으로 넘어감
                if (needNewPage) {
                    i--;  // 현재 단락을 다음 페이지에서 다시 처리
                    continue;
                }
                
                // 남은 텍스트 처리
                if (line.length() > 0) {
                    try {
                        contentStream.showText(safeText(line.toString(), koreanFont));
                    } catch (Exception e) {
                        logger.warn("[PDFService] 내용 텍스트 렌더링 실패: {}", e.getMessage());
                    }
                    contentStream.newLine();
                    yPosition -= LEADING;
                }
                
                // 문단 사이 줄바꿈
                contentStream.newLine();
                yPosition -= LEADING;
                
                // 페이지 넘김 체크
                if (yPosition < margin + 50 && i < paragraphs.length - 1) {
                    needNewPage = true;
                }
            }
            
            // 마지막 텍스트 블록 종료
            contentStream.endText();  // 명시적으로 endText() 호출
            
            // 마지막 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(koreanFont, SMALL_FONT_SIZE);
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
     * 한글을 포함한 텍스트를 폰트에서 지원하는 문자만으로 변환합니다.
     * 지원되지 않는 문자는 대체 문자로 변환됩니다.
     */
    private String safeText(String text, PDType0Font font) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        
        StringBuilder safe = new StringBuilder();
        for (char c : text.toCharArray()) {
            try {
                // 해당 문자가 폰트에서 지원되는지 확인
                font.encode(String.valueOf(c));
                safe.append(c);
            } catch (IllegalArgumentException | IOException e) {
                // 한글 등 지원되지 않는 문자는 로마자로 변환 또는 대체 문자 사용
                if (Character.isLetter(c)) {
                    char replacement = (c >= '가' && c <= '힣') ? '?' : c;
                    safe.append(replacement);
                } else {
                    safe.append(c); // 기호나 숫자는 그대로 유지
                }
            }
        }
        
        return safe.toString();
    }
} 