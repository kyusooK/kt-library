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

    private static final String FONT_PATH = "fonts/NanumGothic.ttf";

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
            
            // 파일명 생성
            String fileName = "book_" + UUID.randomUUID().toString().substring(0, 8) + ".pdf";
            Path pdfPath = pdfDir.resolve(fileName);
            
            // PDF 생성
            createPdf(content, imageUrl, summary, bookName, pdfPath.toString());
            
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
                Files.writeString(textFilePath, errorContent);
                return textFilePath.toAbsolutePath().toString();
            } catch (IOException textError) {
                logger.error("백업 텍스트 파일 생성 실패: {}", textError.getMessage());
                return "/storage/pdfs/error.txt";
            }
        }
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

        try {
            // 3. 폰트 설정
            BaseFont baseFont = BaseFont.createFont(
                new org.springframework.core.io.ClassPathResource(FONT_PATH).getURL().toString(),
                BaseFont.IDENTITY_H, 
                BaseFont.EMBEDDED
            );
            Font titleFont = new Font(baseFont, 20, Font.BOLD);
            Font normalFont = new Font(baseFont, 12, Font.NORMAL);
            Font subtitleFont = new Font(baseFont, 16, Font.BOLD);
            
            // 4. 제목 추가
            Paragraph title = new Paragraph(bookName, titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // 5. 표지 이미지 추가 (있는 경우)
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

            // 6. 요약 섹션 추가
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
            
            // 7. 새 페이지 추가
            document.newPage();
            
            // 8. 본문 내용 추가
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
                        contentParagraph.add("\n");
                    }
                }
                document.add(contentParagraph);
            } else {
                document.add(new Paragraph("본문 내용이 없습니다.", normalFont));
            }
        } finally {
            // 9. 문서 닫기
            document.close();
            writer.close();
            outputStream.close();
            logger.info("PDF 파일 생성 완료: {}", outputPath);
        }
    }
} 