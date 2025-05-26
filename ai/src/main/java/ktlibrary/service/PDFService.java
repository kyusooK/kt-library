package ktlibrary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.annotation.PostConstruct;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    
    @Value("${app.storage.path:./storage}")
    private String storagePath;
    
    @Value("${server.port:8080}")
    private String serverPort;
    
    @Value("${app.base-url:http://localhost}")
    private String baseUrl;

    private static final String FONT_PATH = "fonts/NanumSquareR.ttf";

    @PostConstruct
    public void init() {
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            if (storagePath == null || storagePath.trim().isEmpty()) {
                storagePath = "./storage"; // 기본값 설정
                logger.info("스토리지 경로가 설정되지 않아 기본값으로 설정: {}", storagePath);
            }
            
            // 루트 스토리지 디렉토리 생성
            Path rootDir = Paths.get(storagePath);
            if (!Files.exists(rootDir)) {
                Files.createDirectories(rootDir);
                logger.info("루트 스토리지 디렉토리 생성: {}", rootDir.toAbsolutePath());
            }
            
            // PDF 저장소 디렉토리 생성
            Path pdfDir = Paths.get(storagePath, "pdfs");
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
                logger.info("PDF 디렉토리 생성: {}", pdfDir.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("스토리지 디렉토리 생성 실패: {}", e.getMessage(), e);
        }
    }

    /**
     * 책 제목을 안전한 파일 이름으로 변환합니다.
     * 파일 시스템에서 사용할 수 없는 특수 문자를 제거하고 공백을 언더스코어로 변환합니다.
     * 
     * @param bookName 책 제목
     * @return 안전한 파일 이름
     */
    private String createSafeFileName(String bookName) {
        if (bookName == null || bookName.trim().isEmpty()) {
            return "book_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        // 1. 파일명으로 사용할 수 없는 특수문자 제거
        String safeFileName = bookName
                .replaceAll("[\\\\/:*?\"<>|]", "") // 윈도우에서 파일명에 사용할 수 없는 문자 제거
                .replaceAll("[^a-zA-Z0-9가-힣\\s._-]", "") // 알파벳, 숫자, 한글, 공백, 점, 언더스코어, 하이픈만 허용
                .trim(); // 앞뒤 공백 제거
        
        // 2. 공백을 언더스코어로 변환
        safeFileName = safeFileName.replaceAll("\\s+", "_");
        
        // 3. 길이 제한 (최대 50자)
        if (safeFileName.length() > 50) {
            safeFileName = safeFileName.substring(0, 50);
        }
        
        // 4. 빈 문자열이거나 특수문자 제거 후 길이가 0인 경우 기본값 사용
        if (safeFileName.isEmpty()) {
            return "book_" + UUID.randomUUID().toString().substring(0, 8);
        }
        
        return safeFileName;
    }

    /**
     * 책 내용, 이미지, 요약을 기반으로 PDF를 생성하고 파일명을 반환합니다.
     * @param content 책 내용
     * @param imageUrl 표지 이미지 URL
     * @param summary 요약 내용
     * @param bookName 책 제목
     * @return 생성된 PDF 파일명 (확장자 제외)
     */
    public String generatePdf(String content, String imageUrl, String summary, String bookName) {
        logger.info("PDF 생성 시작: {}", bookName);
        
        try {
            // 경로 확인 및 재설정
            if (storagePath == null || storagePath.trim().isEmpty()) {
                storagePath = "./storage";
                logger.info("스토리지 경로가 설정되지 않아 기본값으로 설정: {}", storagePath);
            }
            
            // PDF 디렉토리 확인 및 생성
            Path pdfDir = Paths.get(storagePath, "pdfs");
            if (!Files.exists(pdfDir)) {
                Files.createDirectories(pdfDir);
                logger.info("PDF 디렉토리 생성: {}", pdfDir.toAbsolutePath());
            }
            
            // 책 제목을 기반으로 파일명 생성
            String safeFileName = createSafeFileName(bookName);
            String fileName = safeFileName + ".pdf";
            Path pdfPath = pdfDir.resolve(fileName);
            
            // 파일명 중복 확인 및 처리
            int counter = 1;
            while (Files.exists(pdfPath)) {
                safeFileName = createSafeFileName(bookName) + "_" + counter;
                fileName = safeFileName + ".pdf";
                pdfPath = pdfDir.resolve(fileName);
                counter++;
            }
            
            // PDF 생성
            createPdf(content, imageUrl, summary, bookName, pdfPath.toString());
            
            // 확장자를 제외한 파일명 반환 (웹 URL 생성용)
            return safeFileName;
        } catch (Exception e) {
            logger.error("PDF 생성 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 발생 시 백업으로 텍스트 파일 생성
            try {
                String errorFileName = "error_" + UUID.randomUUID().toString().substring(0, 8);
                
                Path textFilePath = Paths.get(storagePath, "pdfs", errorFileName + ".txt");
                String errorContent = "PDF 생성 중 오류 발생\n\n"
                        + "제목: " + bookName + "\n\n"
                        + "이미지 URL: " + imageUrl + "\n\n"
                        + "요약:\n" + summary + "\n\n"
                        + "내용:\n" + content;
                Files.writeString(textFilePath, errorContent);
                return errorFileName;
            } catch (IOException textError) {
                logger.error("백업 텍스트 파일 생성 실패: {}", textError.getMessage());
                return "error";
            }
        }
    }
    
    /**
     * 파일명을 기반으로 웹에서 접근 가능한 URL을 생성합니다.
     * @param fileName 파일명 (확장자 제외)
     * @return 웹 URL
     */
    public String generateWebUrl(String fileName) {
        return baseUrl + ":" + serverPort + "/pdfs/" + fileName;
    }

    /**
     * iText를 사용하여 PDF를 생성합니다.
     */
    private void createPdf(String content, String imageUrl, String summary, String bookName, String outputPath) throws DocumentException, IOException {
        // 1. PDF 문서 생성
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        FileOutputStream outputStream = new FileOutputStream(outputPath);
        PdfWriter writer = PdfWriter.getInstance(document, outputStream);
        writer.setInitialLeading(12.5f);

        // 2. 문서 열기
        document.open();
        
        // 3. 최소한 하나의 더미 페이지 추가 (빈 페이지 방지)
        boolean contentAdded = false;

        try {
            // 4. 폰트 설정 (기본 폰트로 시작하고 커스텀 폰트 로드 시도)
            Font titleFont, normalFont, subtitleFont;
            
            try {
                // 커스텀 폰트 로드 시도
                BaseFont baseFont = null;
                
                try {
                    // 방법 1: ClassPathResource 사용
                    baseFont = BaseFont.createFont(
                        new org.springframework.core.io.ClassPathResource(FONT_PATH).getURL().toString(),
                        BaseFont.IDENTITY_H, 
                        BaseFont.EMBEDDED
                    );
                    logger.info("ClassPathResource를 통해 폰트 로드 성공");
                } catch (Exception e1) {
                    logger.warn("ClassPathResource 폰트 로드 실패, 다른 방법 시도: {}", e1.getMessage());
                    
                    try {
                        // 방법 2: 직접 경로 지정
                        baseFont = BaseFont.createFont(
                            "src/main/resources/" + FONT_PATH,
                            BaseFont.IDENTITY_H, 
                            BaseFont.EMBEDDED
                        );
                        logger.info("직접 경로 지정으로 폰트 로드 성공");
                    } catch (Exception e2) {
                        logger.warn("직접 경로 폰트 로드 실패: {}", e2.getMessage());
                        // 방법 3: 기본 폰트 사용
                        throw new Exception("커스텀 폰트 로드 실패");
                    }
                }
                
                titleFont = new Font(baseFont, 20, Font.BOLD);
                normalFont = new Font(baseFont, 12, Font.NORMAL);
                subtitleFont = new Font(baseFont, 16, Font.BOLD);
            } catch (Exception e) {
                // 기본 폰트 사용 (폰트 로드 실패 시)
                logger.warn("커스텀 폰트 로드 실패, 기본 폰트 사용: {}", e.getMessage());
                titleFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
                normalFont = new Font(Font.FontFamily.HELVETICA, 12, Font.NORMAL);
                subtitleFont = new Font(Font.FontFamily.HELVETICA, 16, Font.BOLD);
            }
            
            // 5. 제목 추가
            Paragraph title = new Paragraph(bookName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);
            contentAdded = true;

            // 6. 표지 이미지 추가 (있는 경우)
            if (imageUrl != null && !imageUrl.isEmpty()) {
                try {
                    Image coverImage = Image.getInstance(new URL(imageUrl));
                    coverImage.setAlignment(Element.ALIGN_CENTER);
                    // 적절한 크기로 조정
                    float width = document.getPageSize().getWidth() - 100;
                    float height = 300;
                    coverImage.scaleToFit(width, height);
                    document.add(coverImage);
                    document.add(new Paragraph("\n")); // 이미지와 텍스트 사이 간격
                } catch (Exception e) {
                    logger.error("이미지 추가 실패: {}", e.getMessage());
                    document.add(new Paragraph("이미지를 불러올 수 없습니다.", normalFont));
                }
            }

            // 7. 요약 섹션 추가
            document.add(new Paragraph("요약", subtitleFont));
            document.add(new Paragraph("\n"));
            
            if (summary != null && !summary.isEmpty()) {
                Paragraph summaryParagraph = new Paragraph();
                summaryParagraph.setFont(normalFont);
                summaryParagraph.add(summary);
                document.add(summaryParagraph);
            } else {
                document.add(new Paragraph("요약 내용이 없습니다.", normalFont));
            }
            
            document.add(new Paragraph("\n\n"));
            
            // 8. 새 페이지 추가
            document.newPage();
            
            // 9. 본문 내용 추가
            document.add(new Paragraph("본문", subtitleFont));
            document.add(new Paragraph("\n"));
            
            if (content != null && !content.isEmpty()) {
                Paragraph contentParagraph = new Paragraph();
                contentParagraph.setFont(normalFont);
                // 줄바꿈을 유지하며 텍스트 추가
                String[] lines = content.split("\n");
                for (int i = 0; i < lines.length; i++) {
                    contentParagraph.add(lines[i]);
                    if (i < lines.length - 1) {
                        contentParagraph.add(Chunk.NEWLINE);
                    }
                }
                document.add(contentParagraph);
            } else {
                document.add(new Paragraph("본문 내용이 없습니다.", normalFont));
            }
        } catch (Exception e) {
            logger.error("PDF 내용 추가 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 발생 시에도 최소한 한 페이지 생성 (빈 페이지 방지)
            if (!contentAdded) {
                document.add(new Paragraph("PDF 생성 중 오류가 발생했습니다."));
                document.add(new Paragraph("오류 메시지: " + e.getMessage()));
            }
        } finally {
            try {
                // 10. 문서 닫기
                if (document.isOpen()) {
                    document.close();
                }
                writer.close();
                outputStream.close();
                logger.info("PDF 파일 생성 완료: {}", outputPath);
            } catch (Exception e) {
                logger.error("PDF 문서 닫기 중 오류 발생: {}", e.getMessage(), e);
            }
        }
    }
} 