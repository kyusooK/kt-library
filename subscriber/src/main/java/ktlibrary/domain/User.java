package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;
import ktlibrary.SubscriberApplication;
import ktlibrary.domain.UserRegistered;
import lombok.Data;

@Entity
@Table(name = "User_table")
@Data
//<<< DDD / Aggregate Root
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String email;

    private String userName;

    private Boolean isPurchase;

    private String message;

    @PostPersist
    public void onPostPersist() {
        UserRegistered userRegistered = new UserRegistered(this);
        userRegistered.publishAfterCommit();
    }

    public static UserRepository repository() {
        UserRepository userRepository = SubscriberApplication.applicationContext.getBean(
            UserRepository.class
        );
        return userRepository;
    }

    //<<< Clean Arch / Port Method
    public void buySubscription(BuySubscriptionCommand buySubscriptionCommand) {
        repository().findById(this.getId()).ifPresent(user->{
            if(buySubscriptionCommand.getIsPurchase() == true){
                this.setIsPurchase(buySubscriptionCommand.getIsPurchase());
                SubscriptionBought subscriptionBought = new SubscriptionBought(this);
                subscriptionBought.publishAfterCommit();
            }
        });
    }
    //>>> Clean Arch / Port Method

    //<<< Clean Arch / Port Method
    public static void guideFeeConversionSuggestion(SubscriptionFailed subscriptionFailed) {
 
        // 발행된 이벤트에서 userId정보를 추출출
        ObjectMapper mapper = new ObjectMapper();
        Map<Long, Object> subscriptionMap = mapper.convertValue(subscriptionFailed.getUserId(), Map.class);

        // 추출한 userId와 동일한 User 정보를 조회 후, 안내 메시지 저장장
        repository().findById(Long.valueOf(subscriptionMap.get("id").toString())).ifPresent(user->{
            
            user.setMessage("포인트가 부족하여 구독 신청에 실패했습니다. 포인트를 충전하세요. 또는 구독권을 결제하여 무제한으로 원하는 도서를 구독하세요!");
            repository().save(user);
        });

    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
