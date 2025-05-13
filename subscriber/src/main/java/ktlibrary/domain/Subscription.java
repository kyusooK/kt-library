package ktlibrary.domain;

import ktlibrary.domain.SubscriptionApplied;
import ktlibrary.domain.SubscriptionFailed;
import ktlibrary.SubscriberApplication;
import javax.persistence.*;
import java.util.List;
import lombok.Data;
import java.util.Date;
import java.time.LocalDate;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;


@Entity
@Table(name="Subscription_table")
@Data

//<<< DDD / Aggregate Root
public class Subscription  {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    
    
    
private Long id;    
    
    @Embedded
private BookId bookId;    
    
    @Embedded
private UserId userId;    
    
    
private Boolean isSubscription;    
    
    
private Date startSubscription;    
    
    
private Date endSubscription;    
    
    
private String webUrl;

    @PostPersist
    public void onPostPersist(){


        SubscriptionApplied subscriptionApplied = new SubscriptionApplied(this);
        subscriptionApplied.publishAfterCommit();



        SubscriptionFailed subscriptionFailed = new SubscriptionFailed(this);
        subscriptionFailed.publishAfterCommit();

    
    }

    public static SubscriptionRepository repository(){
        SubscriptionRepository subscriptionRepository = SubscriberApplication.applicationContext.getBean(SubscriptionRepository.class);
        return subscriptionRepository;
    }



//<<< Clean Arch / Port Method
    public void cancelSubscription(CancelSubscriptionCommand cancelSubscriptionCommand){
        
        //implement business logic here:
        

        ktlibrary.external.SubscriptionQuery subscriptionQuery = new ktlibrary.external.SubscriptionQuery();
        // subscriptionQuery.set??()        
          = SubscriptionApplication.applicationContext
            .getBean(ktlibrary.external.Service.class)
            .subscription(subscriptionQuery);

        SubscriptionCanceled subscriptionCanceled = new SubscriptionCanceled(this);
        subscriptionCanceled.publishAfterCommit();
    }
//>>> Clean Arch / Port Method

//<<< Clean Arch / Port Method
    public static void failSubscription(OutOfPoint outOfPoint){
        
        //implement business logic here:
        
        /** Example 1:  new item 
        Subscription subscription = new Subscription();
        repository().save(subscription);

        SubscriptionFailed subscriptionFailed = new SubscriptionFailed(subscription);
        subscriptionFailed.publishAfterCommit();
        */

        /** Example 2:  finding and process
        
        // if outOfPoint.userId exists, use it
        
        // ObjectMapper mapper = new ObjectMapper();
        // Map<Long, Object> pointMap = mapper.convertValue(outOfPoint.getUserId(), Map.class);

        repository().findById(outOfPoint.get???()).ifPresent(subscription->{
            
            subscription // do something
            repository().save(subscription);

            SubscriptionFailed subscriptionFailed = new SubscriptionFailed(subscription);
            subscriptionFailed.publishAfterCommit();

         });
        */

        
    }
//>>> Clean Arch / Port Method


}
//>>> DDD / Aggregate Root
