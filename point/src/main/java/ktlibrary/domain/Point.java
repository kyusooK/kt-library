package ktlibrary.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.persistence.*;

import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ktlibrary.PointApplication;
import ktlibrary.domain.OutOfPoint;
import ktlibrary.domain.PointBought;
import ktlibrary.domain.PointDecreased;
import ktlibrary.domain.RegisterPointGained;
import lombok.Data;

@Entity
@Table(name = "Point_table")
@Data
//<<< DDD / Aggregate Root
public class Point {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private Integer point;

    private Boolean isSubscribe;

    @Embedded
    private UserId userId;

    public static PointRepository repository() {
        PointRepository pointRepository = PointApplication.applicationContext.getBean(
            PointRepository.class
        );
        return pointRepository;
    }

    //<<< Clean Arch / Port Method
    public static void gainRegisterPoint(UserRegistered userRegistered) {

        Point point = new Point();
        point.setPoint(1000);
        point.setUserId(new UserId(userRegistered.getId()));
        repository().save(point);

        RegisterPointGained registerPointGained = new RegisterPointGained(point);
        registerPointGained.publishAfterCommit();

    }

    //>>> Clean Arch / Port Method
    //<<< Clean Arch / Port Method
    public static void decreasePoint(SubscriptionApplied subscriptionApplied) {
        
        ObjectMapper mapper = new ObjectMapper();
        // 발행된 이벤트에서 추출한한 userId와 bookId를 Map 형태로 변환
        Map<Long, Object> userMap = mapper.convertValue(subscriptionApplied.getUserId(), Map.class);
        Map<Long, Object> bookMap = mapper.convertValue(subscriptionApplied.getBookId(), Map.class);

        // Map에서 id 값을 추출하여 Long 타입으로 변환
        Long bookId = Long.valueOf(bookMap.get("id").toString());
        Long userId = Long.valueOf(userMap.get("id").toString());

        RestTemplate restTemplate = new RestTemplate();
        
        // 도서Id, 구독자Id를 조회하여 정보 추출출
        String bookServiceUrl = "http://localhost:8087/books/" + bookId;
        String userServiceUrl = "http://localhost:8086/users/" + userId;

        ResponseEntity<Map> bookResponse = restTemplate.getForEntity(bookServiceUrl, Map.class);
        ResponseEntity<Map> userResponse = restTemplate.getForEntity(userServiceUrl, Map.class);

        // 구독자 정보가 동일한 Point 정보를 조회
        repository().findByUserId(new UserId(userId)).ifPresent(point->{
            // 해당 구독자가 구독권 보유하였는지 확인하고 보유하였을 경우 포인트 감소를 하지 않도록 처리
            Object isPurchase = userResponse.getBody().get("isPurchase");
            if(isPurchase != null && (Boolean)isPurchase == true){
            }else{
                // 베스트셀러 여부에 따라 차감할 포인트의 양을 분류류
                Object isBestSeller = bookResponse.getBody().get("isBestSeller");
                if(isBestSeller != null && (Boolean)isBestSeller == true){
                    // 베스트셀러인 경우 1500포인트 필요
                    if(point.getPoint() >= 1500){
                        point.setPoint(point.getPoint() - 1500);
                        repository().save(point);
                        PointDecreased pointDecreased = new PointDecreased(point);
                        pointDecreased.publishAfterCommit();
                    }else{
                        // 포인트가 부족하면 포인트 부족 이벤트를 발행.
                        OutOfPoint outOfPoint = new OutOfPoint(point);
                        outOfPoint.publishAfterCommit();
                    }
                }else{
                    // 일반 책인 경우 1000포인트 필요
                    if(point.getPoint() >= 1000){
                        point.setPoint(point.getPoint() - 1000);
                        repository().save(point);
                        PointDecreased pointDecreased = new PointDecreased(point);
                        pointDecreased.publishAfterCommit();
                    }else{
                        OutOfPoint outOfPoint = new OutOfPoint(point);
                        outOfPoint.publishAfterCommit();
                    }
                }
            }
        });
    }
    //>>> Clean Arch / Port Method

}
//>>> DDD / Aggregate Root
