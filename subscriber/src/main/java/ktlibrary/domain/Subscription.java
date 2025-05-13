package ktlibrary.domain;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.PostPersist;
import javax.persistence.Table;

import com.fasterxml.jackson.databind.ObjectMapper;

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

        this.isSubscription = true;
        
        // 구독 시작 날짜를 현재 날짜로 설정
        this.startSubscription = new Date();
        
        // 구독 종료 날짜를 한달 후로 설정
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(this.startSubscription);
        calendar.add(Calendar.MONTH, 1);
        this.endSubscription = calendar.getTime();

        SubscriptionApplied subscriptionApplied = new SubscriptionApplied(this);
        subscriptionApplied.publishAfterCommit();
    
    }

    public static SubscriptionRepository repository(){
        SubscriptionRepository subscriptionRepository = SubscriberApplication.applicationContext.getBean(SubscriptionRepository.class);
        return subscriptionRepository;
    }



//<<< Clean Arch / Port Method
    public void cancelSubscription(CancelSubscriptionCommand cancelSubscriptionCommand){
        
        repository().findById(this.getId()).ifPresent(subscription ->{
            this.setIsSubscription(false);
            this.setStartSubscription(null);
            this.setEndSubscription(null);

            SubscriptionCanceled subscriptionCanceled = new SubscriptionCanceled(this);
            subscriptionCanceled.publishAfterCommit();
        });
    }
//>>> Clean Arch / Port Method

//<<< Clean Arch / Port Method
    public static void failSubscription(OutOfPoint outOfPoint){
        
        ObjectMapper mapper = new ObjectMapper();
        Map<Long, Object> subscriptionMap = mapper.convertValue(outOfPoint.getSubscriptionId(), Map.class);

        repository().findById(Long.valueOf(subscriptionMap.get("id").toString())).ifPresent(subscription->{
            
            subscription.setIsSubscription(false);
            subscription.setStartSubscription(null);
            subscription.setEndSubscription(null);
            repository().save(subscription);

            SubscriptionFailed subscriptionFailed = new SubscriptionFailed(subscription);
            subscriptionFailed.publishAfterCommit();

        });
        
    }
//>>> Clean Arch / Port Method


}
//>>> DDD / Aggregate Root
