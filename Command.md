작가 등록 및 승인
```
http :8088/authors email=rbtn110@naver.com authorName=김규수 introductrion=신인작가입니다. feturedWorks=A

http PUT :8088/authors/1/approveauthor isApprove=true
```
집필
```
http :8088/manuscripts title=A content=가나다라마바사 authorId:='{"id":1}' status=WRITING
http PUT :8088/manuscripts/1/requestpublish status=DONE
```

구독자 가입 및 포인트 지급
```
http :8088/users email=rbtn110@gmail.com userName=규수
http :8088/points/1
```

도서 등록(가정)
```
http :8088/books bookName=도서A category=시 authorName=김규수
```

구독 및 포인트 차감
```
http :8088/subscriptions bookId:='{"id":1}' userId:='{"id":1}'\
http :8088/points
```