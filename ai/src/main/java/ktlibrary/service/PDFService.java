package ktlibrary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class PDFService {

    private static final Logger logger = LoggerFactory.getLogger(PDFService.class);
    
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
            
            // 임시 파일 디렉토리 생성
            Path tempDir = Paths.get(storagePath, "temp");
            if (!Files.exists(tempDir)) {
                Files.createDirectories(tempDir);
                logger.info("임시 파일 디렉토리 생성: {}", tempDir.toAbsolutePath());
            }
        } catch (Exception e) {
            logger.error("스토리지 디렉토리 생성 실패: {}", e.getMessage(), e);
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
            
            // 파일명 생성 (타임스탬프 기반 - 한글 처리 걱정 없음)
            String fileName = "book_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            
            Path pdfPath = pdfDir.resolve(fileName);
            
            // HTML 생성 및 PDF 변환
            String html = generateHtml(content, imageUrl, summary, bookName);
            createPdfFromHtml(html, pdfPath.toString());
            
            return pdfPath.toAbsolutePath().toString();
        } catch (Exception e) {
            logger.error("PDF 생성 중 오류 발생: {}", e.getMessage(), e);
            
            // 오류 발생 시 백업으로 텍스트 파일 생성
            try {
                String errorFileName = "error_" + UUID.randomUUID().toString().substring(0, 8) + ".txt";
                
                Path textFilePath = Paths.get(storagePath, "pdfs", errorFileName);
                String errorContent = "PDF 생성 중 오류 발생\n\n"
                        + "제목: " + bookName + "\n\n"
                        + "이미지 URL: " + imageUrl + "\n\n"
                        + "요약:\n" + summary + "\n\n"
                        + "내용:\n" + content;
                Files.writeString(textFilePath, errorContent, StandardCharsets.UTF_8);
                return textFilePath.toAbsolutePath().toString();
            } catch (IOException textError) {
                logger.error("백업 텍스트 파일 생성 실패: {}", textError.getMessage());
                return "/storage/pdfs/error.txt";
            }
        }
    }
    
    /**
     * 책 내용을 HTML로 변환합니다.
     */
    private String generateHtml(String content, String imageUrl, String summary, String bookName) {
        StringBuilder html = new StringBuilder();
        
        // XML 파서가 더 잘 처리할 수 있도록 간단한 HTML 구조 사용
        html.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        html.append("<!DOCTYPE html>\n");
        html.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        html.append("<head>\n");
        html.append("<title>").append(escapeHtml(bookName)).append("</title>\n");
        html.append("<style>\n");
        html.append("body { font-family: Arial, sans-serif; margin: 0; padding: 0; }\n");
        html.append(".cover { text-align: center; page-break-after: always; padding: 20mm; height: 257mm; }\n");
        html.append(".summary { page-break-after: always; padding: 20mm; }\n");
        html.append(".content { padding: 20mm; }\n");
        html.append("</style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 표지 페이지
        html.append("<div class=\"cover\">\n");
        html.append("<h1>").append(escapeHtml(bookName)).append("</h1>\n");
        
        // 이미지 추가
        if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            try {
                String base64Image = getBase64Image(imageUrl);
                if (base64Image != null) {
                    html.append("<img src=\"data:image/jpeg;base64,").append(base64Image).append("\" />\n");
                } else {
                    html.append("<p>Image not available</p>\n");
                }
            } catch (Exception e) {
                logger.error("이미지 처리 오류: {}", e.getMessage());
                html.append("<p>Image loading error</p>\n");
            }
        } else {
            html.append("<p>No image available</p>\n");
        }
        
        html.append("</div>\n");
        
        // 요약 페이지
        html.append("<div class=\"summary\">\n");
        html.append("<h2>").append(escapeHtml(bookName)).append(" - Summary</h2>\n");
        
        if (summary != null && !summary.trim().isEmpty()) {
            // 요약 내용을 문단으로 분할
            String[] paragraphs = summary.split("\n");
            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    html.append("<p>").append(escapeHtml(paragraph)).append("</p>\n");
                }
            }
        } else {
            html.append("<p>No summary available</p>\n");
        }
        
        html.append("</div>\n");
        
        // 내용 페이지
        html.append("<div class=\"content\">\n");
        html.append("<h2>Book Content</h2>\n");
        
        if (content != null && !content.trim().isEmpty()) {
            // 내용을 문단으로 분할
            String[] paragraphs = content.split("\n");
            for (String paragraph : paragraphs) {
                if (!paragraph.trim().isEmpty()) {
                    html.append("<p>").append(escapeHtml(paragraph)).append("</p>\n");
                }
            }
        } else {
            html.append("<p>No content available</p>\n");
        }
        
        html.append("</div>\n");
        
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * HTML을 PDF로 변환합니다.
     */
    private void createPdfFromHtml(String html, String outputPath) throws DocumentException, IOException {
        Document document = new Document(PageSize.A4);
        PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream(outputPath));
        document.open();
        
        InputStream is = new ByteArrayInputStream(html.getBytes(StandardCharsets.UTF_8));
        XMLWorkerHelper.getInstance().parseXHtml(writer, document, is, StandardCharsets.UTF_8);
        
        document.close();
        logger.info("PDF 파일 생성 완료: {}", outputPath);
    }
    
    /**
     * 이미지 URL에서 Base64 인코딩 문자열을 가져옵니다.
     */
    private String getBase64Image(String imageUrl) {
        try {
            URL url = new URL(imageUrl);
            byte[] imageBytes = downloadImage(url);
            return Base64.getEncoder().encodeToString(imageBytes);
        } catch (Exception e) {
            logger.error("이미지 다운로드 실패: {}", e.getMessage());
            return null;
        }
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
     * HTML 특수 문자를 이스케이프합니다.
     */
    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
} 