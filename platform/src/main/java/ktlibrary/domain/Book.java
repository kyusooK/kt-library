package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import ktlibrary.PlatformApplication;
import ktlibrary.domain.BadgeGranted;
import ktlibrary.domain.BookRegistered;
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
        //implement business logic here:

        /** Example 1:  new item 
        Book book = new Book();
        repository().save(book);

        BookRegistered bookRegistered = new BookRegistered(book);
        bookRegistered.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        // if published.manuscriptIdmanuscriptIdllmId exists, use it
        
        // ObjectMapper mapper = new ObjectMapper();
        // Map<Long, Object> publishingMap = mapper.convertValue(published.getManuscriptId(), Map.class);
        // Map<Long, Object> publishingMap = mapper.convertValue(published.getManuscriptId(), Map.class);
        // Map<, Object> publishingMap = mapper.convertValue(published.getLlmId(), Map.class);

        repository().findById(published.get???()).ifPresent(book->{
            
            book // do something
            repository().save(book);

            BookRegistered bookRegistered = new BookRegistered(book);
            bookRegistered.publishAfterCommit();

         });
        */

    }

    //>>> Clean Arch / Port Method
    //<<< Clean Arch / Port Method
    public static void grantBadge(SubscriptionApplied subscriptionApplied) {
        //implement business logic here:

        /** Example 1:  new item 
        Book book = new Book();
        repository().save(book);

        BadgeGranted badgeGranted = new BadgeGranted(book);
        badgeGranted.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        // if subscriptionApplied.bookIduserId exists, use it
        
        // ObjectMapper mapper = new ObjectMapper();
        // Map<Long, Object> subscriptionMap = mapper.convertValue(subscriptionApplied.getBookId(), Map.class);
        // Map<Long, Object> subscriptionMap = mapper.convertValue(subscriptionApplied.getUserId(), Map.class);

        repository().findById(subscriptionApplied.get???()).ifPresent(book->{
            
            book // do something
            repository().save(book);

            BadgeGranted badgeGranted = new BadgeGranted(book);
            badgeGranted.publishAfterCommit();

         });
        */

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
