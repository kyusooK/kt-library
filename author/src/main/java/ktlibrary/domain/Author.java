package ktlibrary.domain;

import ktlibrary.domain.AuthorRegistered;
import ktlibrary.AuthorApplication;
import javax.persistence.*;
import java.util.List;
import lombok.Data;
import java.util.Date;
import java.time.LocalDate;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;


@Entity
@Table(name="Author_table")
@Data

//<<< DDD / Aggregate Root
public class Author  {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
        
        
        
    private Long id;    
        
        
    private String email;    
        
        
    private String authorName;    
        
        
    private String introduction;    
        
        
    private String feturedWorks;    
    
    @Enumerated(EnumType.STRING)
    private List<Portfolio> portfolios;    
        
        
    private Boolean isApprove;

    @PostPersist
    public void onPostPersist(){
        AuthorRegistered authorRegistered = new AuthorRegistered(this);
        authorRegistered.publishAfterCommit();
    }

    public static AuthorRepository repository(){
        AuthorRepository authorRepository = AuthorApplication.applicationContext.getBean(AuthorRepository.class);
        return authorRepository;
    }

//<<< Clean Arch / Port Method
    public void approveAuthor(ApproveAuthorCommand approveAuthorCommand){
        
        repository().findById(this.getId()).ifPresent(author->{
            if(approveAuthorCommand.getIsApprove() == true){
                AuthorApproved authorApproved = new AuthorApproved(this);
                authorApproved.publishAfterCommit();
            }
        });
    }
//>>> Clean Arch / Port Method
//<<< Clean Arch / Port Method
    public void disapproveAuthor(DisapproveAuthorCommand disapproveAuthorCommand){
        
        repository().findById(this.getId()).ifPresent(author->{
            if(disapproveAuthorCommand.getIsApprove() == false){
                AuthorDisApproved authorDisApproved = new AuthorDisApproved(this);
                authorDisApproved.publishAfterCommit();
            }
        });
    }
//>>> Clean Arch / Port Method



}
//>>> DDD / Aggregate Root
