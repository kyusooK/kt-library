# 

## Model
www.msaez.io/#/123912988/storming/5505229cbc01d0187494e4edd4991b3a2

## Before Running Services
### Make sure there is a Kafka server running
```
cd kafka
docker-compose up
```
- Check the Kafka messages:
```
cd infra
docker-compose exec -it kafka /bin/bash
cd /bin
./kafka-console-consumer --bootstrap-server localhost:9092 --topic
```

## Run the backend micro-services
See the README.md files inside the each microservices directory:

- author
- writing
- ai
- point
- subscriber
- platform
- review


## Run API Gateway (Spring Gateway)
```
cd gateway
mvn spring-boot:run
```

## Test by API
- author
```
 http :8088/authors id="id"email="email"authorName="authorName"introduction="introduction"feturedWorks="feturedWorks"isApprove="isApprove"
```
- writing
```
 http :8088/manuscripts id="id"title="title"content="content"AuthorId := '{"id": 0}'Status = "WRITING"
```
- ai
```
 http :8088/publishings id="id"image="image"summaryContent="summaryContent"bookName="bookName"ManuscriptId := '{"id": 0}'pdfPath="pdfPath"authorId="authorId"
```
- point
```
 http :8088/points id="id"point="point"isSubscribe="isSubscribe"UserId := '{"id": 0}'
```
- subscriber
```
 http :8088/users id="id"email="email"userName="userName"isPurchase="isPurchase"
 http :8088/subscriptions id="id"BookId := '{"id": 0}'UserId := '{"id": 0}'isSubscription="isSubscription"startSubscription="startSubscription"endSubscription="endSubscription"webUrl="webURL"
```
- platform
```
 http :8088/books id="id"bookName="bookName"category="category"isBestSeller="isBestSeller"pdfPath="pdfPath"subscriptionCount="subscriptionCount"authorName="authorName"webUrl="webURL"
```
- review
```
 http :8088/reviews id="id"BookId := '{"id": 0}'UserId := '{"id": 0}'content="content"
```


## Run the frontend
```
cd frontend
npm i
npm run serve
```

## Test by UI
Open a browser to localhost:8088

## Required Utilities

- httpie (alternative for curl / POSTMAN) and network utils
```
sudo apt-get update
sudo apt-get install net-tools
sudo apt install iputils-ping
pip install httpie
```

- kubernetes utilities (kubectl)
```
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
```

- aws cli (aws)
```
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
unzip awscliv2.zip
sudo ./aws/install
```

- eksctl 
```
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
```
