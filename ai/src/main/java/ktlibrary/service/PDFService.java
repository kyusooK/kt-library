package ktlibrary.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
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
     * 현재 요청의 호스트 정보를 동적으로 가져와서 URL을 생성합니다.
     * @param fileName 파일명 (확장자 제외)
     * @return 웹 URL
     */
    public String generateWebUrl(String fileName) {
        try {
            // 현재 HTTP 요청에서 호스트 정보를 동적으로 가져오기
            ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attr.getRequest();
            
            String scheme = request.getScheme(); // http 또는 https
            String serverName = request.getServerName(); // 호스트명
            int serverPort = request.getServerPort(); // 포트 번호
            String contextPath = request.getContextPath(); // 컨텍스트 패스
            
            // Cloud IDE 환경 감지 및 처리
            String baseUrl = detectCloudEnvironment(scheme, serverName, serverPort, request);
            
            String webUrl = baseUrl + contextPath + "/pdfs/" + fileName;
            logger.info("동적 웹 URL 생성: {}", webUrl);
            return webUrl;
            
        } catch (IllegalStateException e) {
            // HTTP 요청 컨텍스트가 없는 경우 (백그라운드 작업 등)
            logger.warn("HTTP 요청 컨텍스트가 없음, 환경변수 기반 URL 생성 시도: {}", e.getMessage());
            return generateUrlFromEnvironment(fileName);
        } catch (Exception e) {
            logger.warn("동적 URL 생성 실패, 환경변수 기반 URL 생성 시도: {}", e.getMessage());
            return generateUrlFromEnvironment(fileName);
        }
    }
    
    /**
     * Cloud IDE 환경을 감지하고 적절한 베이스 URL을 반환합니다.
     */
    private String detectCloudEnvironment(String scheme, String serverName, int serverPort, HttpServletRequest request) {
        // 1. GitHub Codespaces 감지
        if (serverName.contains("github.dev") || serverName.contains("githubpreview.dev")) {
            return scheme + "://" + serverName;
        }
        
        // 2. GitPod 감지
        if (serverName.contains("gitpod.io")) {
            return scheme + "://" + serverName;
        }
        
        // 3. Cloud9 감지
        if (serverName.contains("cloud9.amazon.com") || serverName.contains("c9users.io")) {
            return scheme + "://" + serverName;
        }
        
        // 4. Replit 감지
        if (serverName.contains("replit.dev") || serverName.contains("repl.co")) {
            return scheme + "://" + serverName;
        }
        
        // 5. CodeSandbox 감지
        if (serverName.contains("codesandbox.io")) {
            return scheme + "://" + serverName;
        }
        
        // 6. 기타 Cloud IDE 환경 감지 (환경변수 확인)
        String cloudUrl = detectFromEnvironmentVariables();
        if (cloudUrl != null) {
            return cloudUrl;
        }
        
        // 7. X-Forwarded-Host 헤더 확인 (프록시 환경)
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        String forwardedProto = request.getHeader("X-Forwarded-Proto");
        if (forwardedHost != null) {
            String protocol = forwardedProto != null ? forwardedProto : scheme;
            return protocol + "://" + forwardedHost;
        }
        
        // 8. Host 헤더 확인
        String hostHeader = request.getHeader("Host");
        if (hostHeader != null && !hostHeader.equals(serverName + ":" + serverPort)) {
            return scheme + "://" + hostHeader;
        }
        
        // 9. 기본 URL 생성
        if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        } else {
            return scheme + "://" + serverName + ":" + serverPort;
        }
    }
    
    /**
     * 환경변수에서 Cloud IDE URL을 감지합니다.
     */
    private String detectFromEnvironmentVariables() {
        // GitHub Codespaces
        String codespaceName = System.getenv("CODESPACE_NAME");
        if (codespaceName != null) {
            return "https://" + codespaceName + "-8084.app.github.dev";
        }
        
        // GitPod
        String gitpodWorkspaceUrl = System.getenv("GITPOD_WORKSPACE_URL");
        if (gitpodWorkspaceUrl != null) {
            return gitpodWorkspaceUrl.replace("https://", "https://8084-");
        }
        
        // Replit
        String replitSlug = System.getenv("REPL_SLUG");
        String replitOwner = System.getenv("REPL_OWNER");
        if (replitSlug != null && replitOwner != null) {
            return "https://" + replitSlug + "." + replitOwner + ".repl.co";
        }
        
        return null;
    }

    /**
     * HTTP 요청 컨텍스트 없이 환경변수만으로 URL을 생성합니다.
     */
    private String generateUrlFromEnvironment(String fileName) {
        try {
            // 환경변수에서 Cloud IDE URL 감지
            String cloudUrl = detectFromEnvironmentVariables();
            if (cloudUrl != null) {
                String webUrl = cloudUrl + "/pdfs/" + fileName;
                logger.info("환경변수 기반 웹 URL 생성: {}", webUrl);
                return webUrl;
            }
            
            // Cloud IDE 환경변수가 없는 경우 기본값 사용
            String fallbackUrl = "http://localhost:" + this.serverPort + "/pdfs/" + fileName;
            logger.warn("기본 폴백 URL 사용: {}", fallbackUrl);
            return fallbackUrl;
            
        } catch (Exception e) {
            logger.error("환경변수 기반 URL 생성 실패: {}", e.getMessage());
            // 최후의 수단으로 상대 경로 반환
            return "/pdfs/" + fileName;
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