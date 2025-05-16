package ktlibrary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

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

import javax.annotation.PostConstruct;

@Service
public class PDFService {

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
            
            // 제목 추가
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 24);
            
            // 제목 가운데 정렬을 위한 계산
            float titleWidth = bookName.length() * 14; // 대략적인 제목 너비
            float titleX = (width - titleWidth) / 2;
            if (titleX < 50) titleX = 50;
            
            contentStream.newLineAtOffset(titleX, height - 50);
            contentStream.showText(bookName);
            contentStream.endText();
            
            // 이미지 추가 시도
            try {
                if (imageUrl != null && !imageUrl.trim().isEmpty() && !imageUrl.contains("default-cover")) {
                    // 원격 이미지 다운로드
                    System.out.println("[PDFService] 표지 이미지 다운로드 시도: " + imageUrl);
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
                    System.out.println("[PDFService] 표지 이미지 추가 완료");
                } else {
                    // 이미지 없음 텍스트 표시
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                    contentStream.newLineAtOffset((width - 200) / 2, height / 2);
                    contentStream.showText("표지 이미지 없음");
                    contentStream.endText();
                }
            } catch (Exception e) {
                System.err.println("[PDFService] 표지 이미지 추가 실패: " + e.getMessage());
                
                // 이미지 추가 실패 시 텍스트로 대체
                contentStream.beginText();
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.newLineAtOffset((width - 200) / 2, height / 2);
                contentStream.showText("표지 이미지 로드 실패");
                contentStream.endText();
            }
            
            // 페이지 하단에 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(width / 2 - 10, 30);
            contentStream.showText("1");
            contentStream.endText();
        } finally {
            contentStream.close();
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
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 18);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText(bookName + " - 요약");
            contentStream.endText();
            
            yPosition -= 40;
            
            // 구분선 추가
            contentStream.setLineWidth(1f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(width - margin, yPosition);
            contentStream.stroke();
            
            yPosition -= 30;
            
            // 요약 내용 추가
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 12);
            contentStream.setLeading(14.5f);
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
                            contentStream.showText(line.toString());
                            contentStream.newLine();
                            line = new StringBuilder();
                            charCount = 0;
                        }
                    }
                    
                    if (line.length() > 0) {
                        contentStream.showText(line.toString());
                        contentStream.newLine();
                    }
                    
                    contentStream.newLine(); // 문단 사이 줄바꿈
                }
            } else {
                contentStream.showText("요약 내용이 없습니다.");
            }
            
            contentStream.endText();
            
            // 페이지 하단에 페이지 번호 추가
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(width / 2 - 10, 30);
            contentStream.showText("2");
            contentStream.endText();
        } finally {
            contentStream.close();
        }
    }
    
    /**
     * 책 내용 페이지를 생성합니다 (3페이지 이후).
     */
    private void createContentPages(PDDocument document, String content) throws IOException {
        if (content == null || content.isEmpty()) {
            System.out.println("[PDFService] 내용이 없어 내용 페이지를 생성하지 않습니다.");
            return;
        }
        
        // 첫 내용 페이지 추가
        PDPage contentPage = new PDPage(PDRectangle.A4);
        document.addPage(contentPage);
        
        float fontSize = 11;
        float leading = 14.5f;
        
        PDPageContentStream contentStream = null;
        // 이미 2페이지가 있으므로 내용은 3페이지부터 시작
        int pageCount = 3;
        
        try {
            contentStream = new PDPageContentStream(document, contentPage);
            
            // 페이지 크기 가져오기
            float width = contentPage.getMediaBox().getWidth();
            float height = contentPage.getMediaBox().getHeight();
            float margin = 50;
            float yStart = height - margin;
            float yPosition = yStart;
            
            // 제목 추가
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA_BOLD, 16);
            contentStream.newLineAtOffset(margin, yPosition);
            contentStream.showText("책 내용");
            contentStream.endText();
            
            // 구분선 추가
            yPosition -= 20;
            contentStream.setLineWidth(1f);
            contentStream.moveTo(margin, yPosition);
            contentStream.lineTo(width - margin, yPosition);
            contentStream.stroke();
            
            yPosition -= 30; // 제목과 내용 사이 간격
            
            // 내용 추가
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
            contentStream.setLeading(leading);
            contentStream.newLineAtOffset(margin, yPosition);
            
            // 텍스트 줄바꿈 처리
            String[] paragraphs = content.split("\n");
            
            for (int i = 0; i < paragraphs.length; i++) {
                String paragraph = paragraphs[i];
                
                // 현재 y 위치가 하단 여백보다 작으면 새 페이지 생성
                if (yPosition < margin + 50) {
                    // 현재 스트림 종료
                    contentStream.endText();
                    contentStream.close();
                    
                    // 새 페이지 생성
                    contentPage = new PDPage(PDRectangle.A4);
                    document.addPage(contentPage);
                    pageCount++;
                    
                    // 새 스트림 생성
                    contentStream = new PDPageContentStream(document, contentPage);
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                    contentStream.setLeading(leading);
                    contentStream.newLineAtOffset(margin, yStart);
                    yPosition = yStart;
                }
                
                if (paragraph.trim().isEmpty()) {
                    contentStream.newLine();
                    yPosition -= leading;
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
                        contentStream.showText(line.toString());
                        contentStream.newLine();
                        yPosition -= leading;
                        line = new StringBuilder();
                        charPosition = 0;
                        
                        // 페이지 넘김 체크
                        if (yPosition < margin + 50) {
                            // 현재 스트림 종료
                            contentStream.endText();
                            contentStream.close();
                            
                            // 새 페이지 생성
                            contentPage = new PDPage(PDRectangle.A4);
                            document.addPage(contentPage);
                            pageCount++;
                            
                            // 새 스트림 생성
                            contentStream = new PDPageContentStream(document, contentPage);
                            contentStream.beginText();
                            contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                            contentStream.setLeading(leading);
                            contentStream.newLineAtOffset(margin, yStart);
                            yPosition = yStart;
                        }
                    }
                }
                
                // 남은 텍스트 처리
                if (line.length() > 0) {
                    contentStream.showText(line.toString());
                    contentStream.newLine();
                    yPosition -= leading;
                }
                
                // 문단 사이 줄바꿈
                contentStream.newLine();
                yPosition -= leading;
                
                // 페이지 넘김 체크
                if (yPosition < margin + 50 && i < paragraphs.length - 1) {
                    // 현재 스트림 종료
                    contentStream.endText();
                    contentStream.close();
                    
                    // 새 페이지 생성
                    contentPage = new PDPage(PDRectangle.A4);
                    document.addPage(contentPage);
                    pageCount++;
                    
                    // 새 스트림 생성
                    contentStream = new PDPageContentStream(document, contentPage);
                    contentStream.beginText();
                    contentStream.setFont(PDType1Font.HELVETICA, fontSize);
                    contentStream.setLeading(leading);
                    contentStream.newLineAtOffset(margin, yStart);
                    yPosition = yStart;
                }
            }
            
            // 페이지 번호 추가
            contentStream.endText();
            contentStream.beginText();
            contentStream.setFont(PDType1Font.HELVETICA, 10);
            contentStream.newLineAtOffset(width / 2 - 10, 30);
            contentStream.showText(String.valueOf(pageCount));
            contentStream.endText();
            
        } finally {
            if (contentStream != null) {
                contentStream.close();
            }
        }
        
        System.out.println("[PDFService] PDF 생성 완료: 총 " + pageCount + "페이지");
    }
    
    /**
     * URL에서 이미지를 다운로드합니다.
     */
    private byte[] downloadImage(URL url) throws IOException {
        try (InputStream in = url.openStream()) {
            return in.readAllBytes();
        }
    }
} 