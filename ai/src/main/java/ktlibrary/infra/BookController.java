package ktlibrary.infra;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
public class BookController {

    @Value("${app.storage.path:./storage}")
    private String storagePath;

    /**
     * 웹에서 생성된 HTML 책 내용을 제공합니다.
     */
    @GetMapping(value = "/books/{id}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> getBookContent(@PathVariable String id) {
        try {
            Path htmlPath = Paths.get(storagePath, "web", id + ".html");
            
            if (!Files.exists(htmlPath)) {
                return ResponseEntity.notFound().build();
            }
            
            String content = new String(Files.readAllBytes(htmlPath), StandardCharsets.UTF_8);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<html><body><h1>오류 발생</h1><p>" + e.getMessage() + "</p></body></html>");
        }
    }

    /**
     * 생성된 PDF(텍스트) 파일을 다운로드할 수 있도록 제공합니다.
     */
    @GetMapping(value = "/pdfs/{id}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> getPdfFile(@PathVariable String id) {
        try {
            Path pdfPath = Paths.get(storagePath, "pdfs", id + ".txt");
            
            if (!Files.exists(pdfPath)) {
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new FileSystemResource(pdfPath.toFile());
            return ResponseEntity.ok()
                    .header("Content-Disposition", "attachment; filename=\"" + id + ".txt\"")
                    .body(resource);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 상태 체크 엔드포인트
     */
    @GetMapping("/books/status")
    public ResponseEntity<String> getStatus() {
        return ResponseEntity.ok("북 서비스 정상 작동 중");
    }
} 