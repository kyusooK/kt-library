package ktlibrary.domain;

import java.util.Map;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.ObjectMapper;

import ktlibrary.PlatformApplication;
import lombok.Data;

@Entity
@Table(name = "Book_table")
@Data
//<<< DDD / Aggregate Root
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String bookName;

    private String category;

    private Boolean isBestSeller;

    private String pdfPath;

    private Integer subscriptionCount;

    private String authorName;

    private String webUrl;

    public static BookRepository repository() {
        BookRepository bookRepository = PlatformApplication.applicationContext.getBean(
            BookRepository.class
        );
        return bookRepository;
    }

    //<<< Clean Arch / Port Method
    public static void registerBook(Published published) {


        // 출간 준비됨 이벤트 발행에 따른 도서 정보 등록
        Book book = new Book();
        book.setBookName(published.getBookName());
        book.setAuthorName(published.getAuthorId());
        book.setPdfPath(published.getPdfPath());
        book.setWebUrl(published.getWebUrl());
        book.setCategory(published.getCategory());
        book.setIsBestSeller(false);
        book.setSubscriptionCount(0);
        
        repository().save(book);

        BookRegistered bookRegistered = new BookRegistered(book);
        bookRegistered.publishAfterCommit();


    }

    //>>> Clean Arch / Port Method
    //<<< Clean Arch / Port Method
    public static void grantBadge(SubscriptionApplied subscriptionApplied) {
        
        ObjectMapper mapper = new ObjectMapper();
        Map<Long, Object> bookMap = mapper.convertValue(subscriptionApplied.getBookId(), Map.class);

        // 구독시 선택한 도서ID와 일치하는 도서정보를 조회
        repository().findById(Long.valueOf(bookMap.get("id").toString())).ifPresent(book->{
            
            // 특정 도서의 구독 신청이 진행될 때마다 구독숫자가 증가
            if(book.getSubscriptionCount() == null){
                book.setSubscriptionCount(1);
                repository().save(book);
            }else{
                book.setSubscriptionCount(book.getSubscriptionCount() + 1);
                repository().save(book);
            }


            // 구독 숫자가 일정 횟수에 도달하면 베스트셀러처리
            if(book.getSubscriptionCount() == 3){
                book.setIsBestSeller(true);

                BadgeGranted badgeGranted = new BadgeGranted(book);
                badgeGranted.publishAfterCommit();
            }
         });

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
