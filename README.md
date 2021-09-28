# 2019KAKAOBLINDRECRUITMENT_2nd_Java_Elevator_ver.02 (SCAN)
2019 카카오 블라인드 공채 2차 오프라인 코딩 테스트 문제

## 1. 문제 설명

문제와 Elevator 서버 코드: https://github.com/kakao-recruit/2019-blind-2nd-elevator

문제와 해설: https://tech.kakao.com/2018/10/23/kakao-blind-recruitment-round-2/

풀이 ver.01 (FIFO) : https://github.com/ckddn9496/2019KAKAOBLINDRECRUITMENT_2nd_Java_Elevator_ver.01

## 2. 코드 구성
> 코드 리팩토링이 되지 않아 구조가 조금 복잡합니다.

## ver .02 추가사항

### ElevatorController

Elevator와 Call들의 상태 관리, 명령어 생성을 위한 클래스

각 엘리베이터의 진행방향에 대한 정보를 알기 위해 ``` private int[] elevDirection;```를 이용하였으며, 진행방향이 같은 Call들을 passenger목록에 추가하였다.

```java
while (true) {
  boolean is_finished = onCalls()
  if (is_finished)
    break;
  
  generateCommands();

  action(commands);
}

```


## ver .02 변경사항

### Main

시작을 위한 메인 클래스. ElevatorController 객체를 composite 하고있다. 

start API를 이용해 Token을 얻은후 controller를 이용하여 명령을 전달한다.

### GlobalData

problem_id에 따른 빌딩의 높이값도 추가되었다.

## ver .01

### TokenManager

토큰 관리를 위한 클래스

Connection의 start를 이용하여 token을 받아오고 이후 token이 필요한 곳에서 TokenManager인스턴스를 가져와 이용한다.

``` java
String token = TokenManager.getInstance().getToken()
```

### Connection

REST API이용을 위한 클래스

start, onCalls, action를 제공하며 Connection이 필요한 곳에서 Connection 인스턴스를 가져와 이용한다.
``` java
Conection connection = Connection.getInstance();

connection.start();

connection.oncalls();

connection.action();
```

### JSONParser

REST API를 통해 가져온 JSON Format 데이터를 처리할 수 있는 Data형태로 파싱하는 클래스.

주로 onCalls를 통해 가져온 JSONObject를 파싱하여 Elevator와 Call을 생성한다.

### GlobalData

REST API이용에 필요한 URL이 정의되어 있는 클래스

## Resource
### Elevator

Elevator 클래스

### Call

Call 클래스

### Command

Command 클래스

## 3. 결과

> Start Params
```java
String userKey = "tester";
		int problemId = 0; 
    /* 0: 어피치 맨션
    *  1: 제이지 빌딩
    *  2: 라이언 타워
    */
		int numOfElevators = 4;
```
 **1. 어피치 맨션 Total Timestamp: 28**

    AveWait: 12.333333, AveTravel: 7.000000, AveTotal: 19.333333, LastTs: 28, Status: OK

  **2. 제이지 빌딩 Total Timestamp 631**

    AveWait: 63.835000, AveTravel: 26.625000, AveTotal: 90.460000, LastTs: 631, Status: OK
    
 **3. 라이언 타워 Total Timestamp 1892**

    AveWait: 29.250000, AveTravel: 18.534000, AveTotal: 47.784000, LastTs: 1892, Status: OK

    
## 4. 문제점

FIFO보다 평균 응답시간이 라이언 타워에서 약 23배 빨라졌다. 이 배율은 passenger에 양에 비례하며 꽤 좋은 응답을 보여준다고 생각한다. 하지만 라이언 타워의 조건중 1층과 13층만을 왔다갔다하는 승객이 많다는 조건으로 인하여 13층보다 더 높은 층에서 기다리는 승객이 없을 경우에도 최상층까지 왕복하는 엘리베이터의 행동은 비 효율적일 수 있다. 이는 callList에서 최상층과 최하층을 실시간으로 알아내 minHeight와 maxHeight를 대처하여 해결할 수 있으며 이러한 구현방식이 운영체제 디스크 알고리즘에서 SCAN과 LOOK의 차이라 생각한다.
