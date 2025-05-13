package ktlibrary.domain;

import java.util.Date;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;

import ktlibrary.SubscriberApplication;
import lombok.Data;


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
    
    }

    public static SubscriptionRepository repository(){
        SubscriptionRepository subscriptionRepository = SubscriberApplication.applicationContext.getBean(SubscriptionRepository.class);
        return subscriptionRepository;
    }



//<<< Clean Arch / Port Method
    public void cancelSubscription(CancelSubscriptionCommand cancelSubscriptionCommand){
        
        //implement business logic here:
 

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
