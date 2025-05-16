작가 등록 및 승인
```
http :8088/authors email=rbtn110@naver.com authorName=김규수 introductrion=신인작가입니다. feturedWorks=A

http PUT :8088/authors/1/approveauthor isApprove=true
```
집필
```
http :8088/manuscripts title="달빛 속 비밀" content="첫 번째 장: 시작의 끝

빗방울이 창문을 두드리는 소리가 방 안을 가득 채웠다. 김민준은 오래된 책상 앞에 앉아 어둠 속에서 빛나는 모니터만을 바라보고 있었다. 15년 전 그날의 기억이 다시 떠올랐다.

'그때 내가 다르게 선택했다면...'

그는 서랍에서 낡은 사진 한 장을 꺼냈다. 사진 속 웃고 있는 여자는 이제 이 세상 어디에도 없었다. 

'난 네가 보낸 신호를 알아채지 못했어.'

그때 갑자기 울리는 전화벨. 민준은 천천히 수화기를 들었다.

'김민준 씨, 박소연씨의 사건에 대해 몇 가지 더 여쭤봐야 할 것이 있습니다.'

차가운 형사의 목소리. 소연이 사라진 지 15년이 지났지만, 경찰은 여전히 그녀를 찾고 있었다. 그리고 민준은 진실을 알고 있었다.

다음 날 아침, 민준은 오랜만에 그곳을 찾았다. 한강 다리 아래, 소연이 마지막으로 목격된 장소. 그는 주머니에서 작은 열쇠를 꺼내 바라보았다. 이 열쇠가 모든 비밀의 시작이었다.

'이제 모든 것을 밝힐 시간이야.'

민준이 발걸음을 옮기려는 순간, 누군가가 그의 어깨를 잡았다. 놀라 뒤돌아보니, 오랫동안 보지 못했던 얼굴이 그를 바라보고 있었다.

'오랜만이네, 민준아.'

그것은 죽었다고 생각했던 소연의 쌍둥이 자매, 지연이었다." authorId:='{"id":1}' status=WRITING
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