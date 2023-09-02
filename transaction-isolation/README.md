# 트랜잭션의 격리 수준(Transaction Isolation Level)

**트랜잭션의 격리수준이란?**

여러 트랜잭션이 동시에 처리될 때 특정 트랜잭션이 다른 트랜잭션에서 변경하거나 조회하는 데이터를 볼 수 있게 허용할지 말지 결정하는 것.

**************************************격리수준의 종류**************************************

아래로 갈 수록 트랜잭션간 데이터 격리(고립) 정도가 높아지며, 동시 처리 성능이 떨어지는 것이 일반적

- READ UNCOMMITTED
- READ COMMITTED
- REPEATABLE READ
- SERIALIZABLE

**일반적인 온라인 서비스 용도의 데이터베이스는 READCOMMITTED와 REPEATABLE READ 중 하나를 사용한다.**

- 오라클은 READ COMMITTED 수준을 많이 사용
- Mysql은 REPEATABLE READ를 주로 사용

## READ UNCOMMITTED

- 변경 내용이 COMMIT 이나 ROLLBACK 여부에 상관없이 다른 트랜잭션에서 보인다.
- RDBMS 표준에서 트랜잭션의 격리 수준으로 인정하지 않을 정도로 정합성에 문제가 많은 격리 수준.
- Mysql을 사용한다면 최소 READ COMMITTED 이상의 격리 수준을 사용할 것을 권장.

**테스트 시나리오**

![image](https://github.com/yssy3135/practice/assets/62733005/58cb1e9f-9883-4d9e-9c1d-99e450b9ee47)

**테스트 코드**

```java
@Test
    @DisplayName("READ_UNCOMMITTED 여러 스레드가 동시 호출시Dirty Read, Non-Repeatable Read, Phantom Read 현상이 모두 발생")
    public void shouldDirtyReadAndPhantomReadWhenIsolationLevelReadUncommitted() throws InterruptedException, ExecutionException {
        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            memberService.updateMember(savedMember.getId(), "updated");

        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            Member foundMember = memberService.findMemberBy(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        assertEquals("updated", foundMemberResult.get().getName());
    }
```

**스레드 worker-1 실행 함수**

```java
@Transactional
    public Member updateMember(Long id, String name) {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));

        member.updateName(name);
        memberRepository.flush();
        sleep(3000);

        log.info("*****update Member close*****");
        return memberRepository.save(member);
    }
```

**************스레드 worker-2 실행 함수**************

```java
@Transactional(isolation = Isolation.READ_UNCOMMITTED)
    public Member findMemberBy(Long id)  {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        log.info("*****found Member : {}*****", member);
        return member;
    }
```

실행 로그

![image](https://github.com/yssy3135/practice/assets/62733005/feb65f5f-8f62-486f-a65d-0cbb65f6d585)

1. worker-1 스레드 :  저장된 Member를 select 한다. (transaction start)
2. worker-1 스레드 : member를 update한다. (Member(name : “before”) → Member(name : “update”)
3. worker-2 스레드 : member를 select 한다.

   이때 transaction이 끝나지 않고 commit 되지 않은 member 값이 조회되는 것을 볼수 있다. (Dirty Read)


### Dirty Read

- 트랜잭션에서 처리한 작업이 완료되지 않았는데 다른 트랜잭션에서 볼 수 있는 현상
- READ UNCOMMITED 수준에서 허용된다.

## READ COMMITTED

- 오라클 DBMS에서 기본으로 사용되는 격리 수준,
- 온라인 서비스에서 가장 많이 선택되는 격리 수준
- 더티 리드 (Dirty read) 현상 발생 하지 않음.

****Dirty Read 발생 하지 않는다!****

- 위 READ UNCOMMITTED와 같은시나리오로 테스트를 진행
- 스레드 worker-2 함수의 어노테이션만 변경하였다.

```java
@Test
    @DisplayName("READ_COMMITTED 여러 스레드가 동시 호출시Dirty Read 발생 안함")
    public void shouldNotDirtyReadWhenIsolationLevelReadCommitted() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            memberService.updateMember(savedMember.getId(), "updated");
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            sleep(500);
            Member foundMember = memberService.findMemberById_read_committed(savedMember.getId());
            log.info("found member : {}", foundMember.toString());
            return foundMember;

        });

        CompletableFuture.allOf(changeName,foundMemberResult).join();

        assertEquals("before", foundMemberResult.get().getName());
    }
```

****************************스레드 worker-2****************************

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findMemberById_read_committed(Long id) {

        return memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));

    }
```

**테스트 결과**

![image](https://github.com/yssy3135/practice/assets/62733005/d3a383e8-bc4f-47c4-b67c-38a91a44a2ec)


- 위 READ UNCOMMITTED와 동일한 시나리오로 테스트하였지만 다른 트랜잭션에서 아직 커밋되지 않은 정보를 읽어오지 않는다!

### 그럼 어떻게 조회되는 것일까?

![image](https://github.com/yssy3135/practice/assets/62733005/f719f829-98e1-4a9a-8530-80ff4402705a)

- A 에서 member name 을 “update” 로 수정했는데 이때 “update”는 테이블에 즉시 기록
- 이전 값 “before”가 언두 영역에 백업된다.
- A가 커밋되기 전에 B에서 조회했을 경우 언두 영역에서 백업된 레코드를 가져와 member name : ”before” 가 반환 된다.

### READ COMMITTED의 부정합 문제 NON-REPEATALBE READ

**NON_REPEATABLE READ란?**

하나의 트랜잭션 내에서 똑같은 SELECT 쿼리를 실행했을 때는 항상 같은 결과를 가져와야 한다는 REPEATABLE READ 정합성에 어긋난 것.

**시나리오**

![image](https://github.com/yssy3135/practice/assets/62733005/53b5373b-0879-4da3-b6a7-f69b7ea8a54d)

```java
@Test
    @DisplayName("READ_COMMITTED Non-Repeatable Read 한 트랜잭션 내에서 반복 읽기를 수행하면 다른 트랜잭션의 커밋 여부에 따라 조회 결과가 달라지는 문제 발생")
    public void shouldNonRepeatableReadWhenIsolationLevelReadCommitted() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.immediateUpdateMember(savedMember.getId(), "updated");
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            Member member= null;
            member = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnResults("updated");

            return member;

        });

        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());
        assertEquals("updated", foundMemberResult.get().getName());

    }
```

```java
@Transactional
    public Member immediateUpdateMember(Long id, String name) throws InterruptedException {
        Member member = memberRepository.findById(id).orElseThrow(() -> new RuntimeException("Not found user"));
        member.updateName(name);

        log.info("*****update Member close*****");
        return memberRepository.saveAndFlush(member);
    }
```

```java
@Transactional(isolation = Isolation.READ_COMMITTED)
    public Member findUserByIdTwiceStopOnceInTheMiddleReturnResults(String name) throws InterruptedException {

        return repeatableFind(name);

    }

    private Member repeatableFind(String name) throws InterruptedException {
        Optional<Member> member = memberRepository.findMemberByName(name);

        log.info("*****첫번쨰 Member : {}*****", member);

        Thread.sleep(3000);

        Optional<Member> secondMember = memberRepository.findMemberByName(name);
        log.info("*****두번쨰 Member : {}*****", secondMember);
        return secondMember.orElse(null);
    }
```

### 결과

![image](https://github.com/yssy3135/practice/assets/62733005/76fce210-e54b-4036-b6cb-28f416738f74)

- woker-1 트랜잭션이 시작되고 첫번째 findByName(”updated”) : Optional.empty가 나온다.
- worker-2에서 이미 저장되어 있던 member를 Member(1,”updated”)로 업데이트하고 커밋한다.
- woker-1 스레드 에서 두번째 findByName(”updated”)에서는 결과로  Member(1,”updated”) 가반환된다.

하나의 트랜잭션 내에서 똑같은 SELECT 쿼리를 실행했을 때는 항상 같은 결과를 가져와야 한다는 REPEATABLE READ 정합성에 어긋난 다는 것을 확인할 수 있다!

## REPREATABLE READ

- mysql의 InnoDB 스토리지 엔진에서 기본으로 사용되는 격리 수준.
- NON-REPEATABLE READ 부정합 발생하지 않음.

### NON-REPEATABLE READ 부정합 발생하지 않는다!

**테스트**

**위 COMMIT READ와 같은 방식,시나리오로 테스트**

```java
@Test
    @DisplayName("REPEATABLE_READ NON-REPEATABLE READ 현상 발생 안함.")
    public void shouldNotNonRepeatableReadWhenIsolationLevelRepeatableRead() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.immediateUpdateMember(savedMember.getId(), "updated");
        });

        CompletableFuture<Member> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            Member member= null;
            member = memberService.findMemberRepeatable("updated");

            return member;

        });

        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());
        assertNull(foundMemberResult.get());

    }
```

**************************테스트 결과**************************

![image](https://github.com/yssy3135/practice/assets/62733005/d5f73f0c-f8c9-4961-bb8b-e9010890acf4)

- woker-1 트랜잭션이 시작되고 첫번째 findByName(”updated”) : Optional.empty가 나온다.
- worker-2에서 이미 저장되어 있던 member를 Member(1,”updated”)로 업데이트하고 커밋한다.
- woker-1 스레드 에서 두번째 findByName(”updated”)에서도 Optional.empty가 나온다!

********************어떻게 동작하는 걸까?********************

![image](https://github.com/yssy3135/practice/assets/62733005/1820eb95-3018-4542-9628-89884049ea6b)

- A트랜잭션 num 은 12이고 ,B 트랜잭션 num 은 10이다.
- 한 트랜잭션 중간에 다른 트랜잭션에서 수정이나, 추가 작업이 일어날 경우 번경전 데이터를 트랜잭션번호와 함께 백업된다.
- **모든 select 쿼리는 자신의 트랜잭션 번호보다 작은 트랜잭션 번호에서 변경한것만 보이게 된다!**

### 하지만 Phantom Read가 발생할 수 있다.

mysql 에서 대부분의 경우에서는 Phantom Read가 발생하기 어렵다.

Mysql의 GapLock과 MVCC 때문이다.

**아래는 유일하게 Mysql에서 Phantom Read가 발생하는 시나리오이다.**

### **SELECT 이후 SELECT FOR UPDATE**

****************시나리오****************

![image](https://github.com/yssy3135/practice/assets/62733005/85edb108-f7e7-4b9d-96fd-612d5a021cb2)

**테스트**

```java
@Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 조회 팬텀리드 발생")
    public void shouldPhantomReadWhenIsolationLevelRepeatableReadSelectForUpdate() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
        List<Member> members = null;
        members = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(savedMember.getId());
            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(2, foundMemberResult.get().size());
    }
```

**woker-1**

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)
    public List<Member> findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResultUsingLock(Long id) throws InterruptedException {
     
				memberRepository.findMembersByIdAfterForUpdateUsingLock(id);

        Thread.sleep(1000);

        return memberRepository.findMembersByIdAfterForUpdateUsingLock(id);
    }
-----------------------------------------------------------------------------------

@Query("select m from Member m where m.id >= :id ")
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Member> findMembersByIdAfterForUpdateUsingLock(Long id);
```

**woker-2**

```java
public Member saveMember(MemberRequest memberRequest)  {
        Member memberRequestEntity = memberRequest.toEntity();
        return memberRepository.saveAndFlush(memberRequestEntity);
    }
```

**결과**

![image](https://github.com/yssy3135/practice/assets/62733005/2c0b65ab-70e4-4fb7-a053-35762b111486)

- woker-1 트랜잭션이 시작되고 조회를 id 가 1이상인 Member를 조회한다.
- woker-2 Member Table에 새로운 member를 추가한다.  → Undo 로그에 변경전 데이터가 보관 될것
- woker-1에서 다시 id가  1 이상인 Member를 조회한다 그런데 **SELECT FOR UPDATE 를 통하**여 쓰기 조회를 하였다.
- 그 결과로 언두로그에서 조회하지 않고 테이블의 레코드에 접근해서 가져오기 때문에
- woker-2 에서 새롭게 저장한 member도 함께 조회된다.

### 왜 SELECT FOR UPDATE는 Table에 바로 접근할까?

- MVCC에서는 데이터를 먼저 테이블에 반영하고 언두 로그에 백업한다.
- 즉 SELECT FOR UPDATE로 잠금을 걸어도 이미 테이블에는 반영되고 언두 로그에는 이전 트랜잭션의 데이터가 쌓인다.
- 만약 먼저 시작된 트랜잭션이 존재하여 작업을 하면 테이블에는 반영되고, 언두 로그에는 이전 트랜잭션의 데이터가 쌓인다. 그러므로 MVCC 만으로 정확한 데이터 제공이 불가능하다.
- 언두 로그에도 잠금을 걸어야 하는데, 언두로그는 append only 형태이므로 잠글 수 없다.
- 따라서 SELECT FOR UPDATE나 LOCK IN SHARE MODE로 조회하는 경우에는 언두 영역의 데이터가 아니라 테이블의 레코드를 가져오게 된다.

### 위 Phantom Read가 일어나는 상황과 동일한 코드로 여러 상황을 비교해 보자

두번의 조회에서 Select for update와 select의 순서를 번갈아서 테스트 해 볼 것이다.

### SELECT FOR UPDATE 이후 SELECT

```java
@Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 이후 SELECT ")
    public void shouldNotPhantomReadWhenIsolationLevelRepeatableReadSelectForUpdateAfterSelect() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.selectForUpdateAfterSelect(savedMember.getId());
            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }
```

![image](https://github.com/yssy3135/practice/assets/62733005/2ebbdfa7-543c-400a-9562-d2b4824ad808)

- worker - 1 : select for update 조회
- worker - 2 : insert Member 하지만 commit 되지 않았다!
- worker - 1 : 다시 select
- worker - 1 : commit
- worker - 2 commit
- 테스트는 성공

**왜 팬텀 리드가 일어나지 않을까?**

mysql에서는 Gap Lock 때문에 팬텀 리드가 일어나지 않는다.

먼저 실행된 worker -1 스레드에서 id 1이상의 데이터를 Select For Update 조회했을때

id가 1 이상인 데이터에 대해 GapLock을 걸기 때문에 worker - 2 스레드의 트랜잭션은 worker-1 스레드의 트랜잭션이 종료될 때 가지 대기하게 된다.

### SELECT FOR UPDATE 이후 SELECT FOR UPDATE

```java
@Test
    @DisplayName("REPEATABLE_READ SELECT FOR UPDATE 이후 SELECT_FOR_UPDATE 조회 팬텀리드X")
    public void shouldNotPhantomReadWhenIsolationLevelRepeatableReadSelectForUpdateAfterSelectForUpdated() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.selectForUpdateAfterSelectForUpdate(savedMember.getId());

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }
```

![image](https://github.com/yssy3135/practice/assets/62733005/a1be02f8-a631-4410-aa1a-0268de42e5c6)


- worker - 1 : select for update 조회
- worker - 2 : insert Member 하지만 commit 되지 않았다!
- worker - 1 : 다시 select for update
- worker - 1 : commit
- worker - 2 commit
- 테스트는 성공

위와 동일하기 gap lock에 의해 팬텀리드는 발생하지 않는다.

### SELECT 이후 SELECT

```java
@Test
    @DisplayName("REPEATABLE_READ 팬텀리드 발생 안함. Select 이후 Select ")
    public void shouldNotPhantomReadWhenIsolationLevelRepeatableReadSelectAfterSelect() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            // 처음 조회후 3초간 sleep 후 다시 조회
            members = memberService.findUserByIdTwiceStopOnceInTheMiddleReturnLastSearchResult(savedMember.getId());

            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult).join();

        log.info("updatedMembers {}", changeName.get());
        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
    }
```

![image](https://github.com/yssy3135/practice/assets/62733005/097bf537-caac-4acb-8f74-794603859708)

- worker - 1 : select  조회
- worker - 2 : insert Member 그리고 commit!
- worker - 1 : 다시 select
- worker - 1 : commit
- 테스트 성공

**이번에는 Gap lock이 발생하지 않았다**

**하지만 커밋된 데이터가 조회되지 않았다. 어떻게?**

mysql의 MVCC에 의해 조회 되지 않는다.

## SERIALIZABLE

- SERIALIZABLE은 가장 엄격한 격리수준이다.
- 이름 그대로 트랜잭션을 순차적으로 진행시킨다.
- 여러 트랜잭션이 동일 레코드에 동시 접근 할 수 없기 때문에 어떠한 데이터 부정합 문제도 발생하지 않는다.
- 그만큼 동시 처리 성능도 다른 트랜잭션 격리 수준보다 떨어진다.
- 하지만 읽기 작업도 공유 잠금(읽기 잠금) 을 획득 해야만 하며, 동시에 다른 트랜잭션은 레코드를 변경하지 못하게 된다.
- Phantom Read문제 발생하지 않는다.
- InnoDB 스토리지 엔진에서는 갭락과 넥스트 키 락 덕분에 Repeatable Read 격리 수준에서도 이미 Phantom Read가 발생하지 않기 때문에 SERIALIZABLE을 사용할 필요성은 없다.

**위 REPEATABLE READ에서 Phantom Read가 발생했던 시나리오로 테스트를 진행해보았다.**

![image](https://github.com/yssy3135/practice/assets/62733005/967638b1-dc1d-45a5-b378-0900696bc910)

```java
@Test
    @DisplayName("serializable 아무런 문제 발생하지 않음")
    public void shouldNoProblemWhenIsolationLevelSerializable() throws InterruptedException, ExecutionException {

        Member savedMember = memberService.saveMember(MemberRequest.builder().name("before").build());

        CompletableFuture<List<Member>> foundMemberResult = CompletableFuture.supplyAsync(() -> {
            List<Member> members = null;
            members = memberService.selectForUpdateAfterSelectForUpdateSerializable(savedMember.getId());
            return members;
        });

        CompletableFuture<Void> changeName = CompletableFuture.runAsync(() -> {
            sleep(500);
            memberService.saveMember(MemberRequest.builder().name("newMem").build());
        });
        CompletableFuture.allOf(changeName,foundMemberResult);

        log.info("members {}", foundMemberResult.get());

        assertEquals(1, foundMemberResult.get().size());
        assertEquals("before", foundMemberResult.get().get(0).getName());
    }
```

![image](https://github.com/yssy3135/practice/assets/62733005/45a9f090-9739-47db-9c94-fb578ab9ca07)

- worker - 1 : select 로 조회한다.
- worker -2 : 새로운 Member 를 insert
- woker - 2 : select for update 로 조회

REPEATABLE READ에서는 select로 조회 했을 경우에는 Gap Lock이 걸리지 않기 때문에 위 경우에서 팬텀리드가 발생하였다.

하지만 SERIALIZABLE에서는 Select로 조회했을 경우에도 공유잠금 (읽기 잠금) 을 획을해야 하기 때문에

한 트랜잭션에서 넥스트 키 락이 걸린 레코드를 다른 트랜잭션에서는 절대 추가/수정/삭제할 수 없다.

때문에 팬텀 리드가 발생하지 않는다!