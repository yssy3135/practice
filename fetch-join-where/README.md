# fetch join이 on절을 지원하지 않는 이유

### JPA 표준 스펙에는 fetch join 대상에 별칭이 없다. 하지만 하이버네이트는 허용한다.

→ fetch join 대상에 별칭을 사용은 할 수 있지만 문제가 발생할 수 있어 주의해서 사용해야 한다.

hibernate 참조

> HQL(hibernate Query Language) 에서  Fetch Join에 별칭(Alias)가 필요한 유일한 이유는 추가 컬렉션(N방향) 을 재귀적으로 조인 하여 가져오는 경우밖에 없다.
>

## Fetch Join의 한계

### Fetch Join의 대상은 별칭을 줄 수 없다.

- select, where 서브쿼리 등 에서 fetch join대상을 사용할 수 없다.
- 하이버네이트 자체에서 별칭을 지원 하지만 연관된 데이터 수에 대한 무결성이 깨질 수도 있다.
- 2개 이상의 xToMany 관계에서는 모든 연관 엔티티에 Fetch Join을 적용할 수 없다.
    - default_batch_fetch_size 를 사용하여 in 절로 한번에 호출하도록 할 수 있다.
    - List가 아닌 set 을 사용하면 가능하다. (Hibernate 내부 자료구조를 Bag이라는 타입을 사용하기 때문이다)

## Fetch Join에서 Where절로 사용하게 된다면?

Fetch join에서 on 절은 지원하지 않지만 where 절은 사용할 수 있다.

하지만 where절을 잘못사용할 경우 문제가 일어날 수 있기 때문에 조심해야 한다.

**하이버네이트에서는 Alias를 지원하지만 데이터 무결성이 깨질 수 있다.**

Team 과 Member가 oneToMany로 매핑되어 있다고 가정해볼때

Team1 - Member1

Team1 - Member2

Team1 - Member3

이렇게 연관관계가 매핑되어 있다고 해보자

Fetch join에 ON 절을 사용할 경우 에러가 발생할 것이다.

```jsx
SELECT * FROM Team t JOIN fetch t.member m ON m.id != 1  WHERE t.id = 1
```

이렇게 조회를 한다고 해보자

```jsx
SELECT * FROM Team t JOIN fetch t.member m WHERE t.id = 1 and m.id != 1
```

결과가 어떻게 될까?

Team1 - Member2

Team1 - Member3

위 결과처럼 Team1에 매핑된 Member중 id 가 1일 Member를 제외하고 조회될 것이다

**예상된 결과이지만 여기서 문제가 일어날 수 있다!**

1. DB와 무결성이 깨진다
    1.  DB 상 Team1과 Member1,Member2,Member3이 연관이 되어 있는 상태이지만
        Member1이 제외 되었기 때문에 DB와 데이터가 일치하지 않는 상황이 발생한다.
2. 예상 불가능한 쿼리가 발생할 수 있다.

## 테스트.

Entity 정의

Team

```java
@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    String name;

    @OneToMany
    @JoinColumn(name = "memberId")
    List<Member> members = new ArrayList<>();

    public void setMembers(List<Member> members) {
        this.members = members;
    }

    public void replaceMembers(List<Member> members) {
        this.members.clear();
        this.members.addAll(members);
    }
    public void addMember(Member member) {
        this.members.add(member);
    }

    public void changeName(String name) {
        this.name = name;
    }
}
```

Member

```java
@Entity
@Builder
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long id;

    private String name;

    public String updateName(String name) {
        return this.name = name;
    }

}
```

Test Data Setting

```java
@BeforeEach
    void before() {
        Member member1 = Member.builder().name("철수").build();
        Member member2 = Member.builder().name("영희").build();
        Member member3 = Member.builder().name("짱구").build();
        Member member4 = Member.builder().name("맹구").build();

        List<Member> members = List.of(member1, member2, member3, member4);
        memberRepository.saveAll(members);
        Team team = Team.builder().name("떡잎마을").build();
        team.setMembers(members);

        teamRepository.saveAndFlush(team);

        entityManager.clear();
        System.out.println("================================세팅================================================");
    }
```

### 1. Fetch join where 절을 사용해보자

```jsx
Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

------------------------------------------------------------------------------------------
@Query("SELECT t " +
            "FROM Team t JOIN FETCH t.members m " +
            "WHERE t.name = :teamName AND m.name != :memberName")
    Optional<Team> findByTeamExceptMemberName(String teamName, String memberName);
```

**결과가 어떻게 나올까?**

떡잎마을 - 철수

떡잎마을 - 짱구

떡잎마을 - 맹구

Where 절을 이용해서 영희를 filtering 했기 때문에 영희를 제외한 Member가 조회된다.

**이후 team - 떡잎마을 저장해보면 어떤 결과가 나올까?**

예상되는 결과는

- DB에는 member - 영희가 연관되어 있었으니 member - 영희가 삭제되어야 할까?

```java
@Test
    @Transactional
    @DisplayName("Team을 변경하지 않고 그대로")
    public void saveTeam() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        assertThat(team.getMembers()).size().isEqualTo(3);

        Team saveTeam = teamRepository.saveAndFlush(team);
        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));
        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }
```

테스트 결과

**어떤 쿼리도 수행되지 않는다… 왜…?**

- 조회시에 결과(snapShot)이 저장되어 save할때 해당 entity와 달라진 부분을 dirty checking을 진행하게 되는데 변화가 없었기 때문이다!

```
2024-02-03T17:43:07.373+09:00 DEBUG 97377 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName 
        AND m.name != :memberName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=? 
            and m1_0.name<>?
2024-02-03T17:43:07.408+09:00 DEBUG 97377 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=?
```

**그럼 member - 영희을(DB 상 이미 team에 속해있는) 추가해서 save를 하면 update쿼리가 수행될까?**

DB상에서는 이미 연관관계가 맺어져 있다

일단 snapShot과 비교했을때 달라졌다.

그렇기 때문에 DB상에는 이미 연관관계가 맺어져 있지만 추가된 데이터로 판단하고 다시 연관관계를 위한 Column이 update된다.

```java
@Test
    @Transactional
    @DisplayName("조회에서 제외된 Member를 다시 추가하고 저장했을 경우")
    public void changeTeamAndAddMember() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);

        Member member = memberRepository.findMemberByName("영희").orElseThrow(() -> new RuntimeException("not found"));
        team.addMember(member);

        Team saveTeam = teamRepository.saveAndFlush(team);
        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));
        assertThat(findTeam.getName()).isEqualTo("떡잎마을");
        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }
```

**테스트 결과**

**의미없는 쿼리가 발생하고 예측할 수 없는 쿼리가 발생한다.**

```
2024-02-03T17:39:14.106+09:00 DEBUG 94846 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName 
        AND m.name != :memberName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=? 
            and m1_0.name<>?
com.example.fetchjoin.domain.Team@94ba90a
2024-02-03T17:39:14.139+09:00 DEBUG 94846 --- [    Test worker] org.hibernate.SQL                        : 
    /* select
        m 
    from
        Member m 
    where
        m.name = :name */ select
            m1_0.id,
            m1_0.name 
        from
            member m1_0 
        where
            m1_0.name=?
2024-02-03T17:39:14.145+09:00 DEBUG 94846 --- [    Test worker] org.hibernate.SQL                        : 
    update
        member 
    set
        member_id=? 
    where
        id=?
2024-02-03T17:39:14.147+09:00 DEBUG 94846 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=?
```

**조회된 Team에서 member를 하나 삭제하고 저장했을경우 DB상에 연관되어져있던 member는 어떻게될까?**

```java
@Test
    @Transactional
    @DisplayName("Member를 한명 제거 하고 저장했을 경우")
    public void removeMember() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);
        team.getMembers().remove(team.getMembers().stream().filter(member -> member.getName().equals("철수")).findAny().orElseThrow(() -> new RuntimeException("not found")));

        Team saveTeam = teamRepository.saveAndFlush(team);

        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));

        assertThat(findTeam.getMembers().size()).isEqualTo(3);
    }
```

**테스트 결과**

변경된 Member - 철수 만 update 쿼리가 발생한다.

```
2024-02-03T17:46:27.040+09:00 DEBUG 99642 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName 
        AND m.name != :memberName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=? 
            and m1_0.name<>?
com.example.fetchjoin.domain.Team@5ce34685
2024-02-03T17:46:27.077+09:00 DEBUG 99642 --- [    Test worker] org.hibernate.SQL                        : 
    update
        member 
    set
        member_id=null 
    where
        member_id=? 
        and id=?
2024-02-03T17:46:27.081+09:00 DEBUG 99642 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=?
```

**여기서 주의!!**

members를 변경할때 List를 새로운 리스트를 전달하면 one-shot Delete를 진행하고

전달된 members에 대한 연관관계를 재연결한다.

그렇기 때문에 원래 연결되어 있었지만 조회되지 않는 Entity( member - 영희) 는 Team - 떡잎마을에서 빠지게 된다.

```java
@Test
    @Transactional
    @DisplayName("Member를 2명만 새로운 리스트를 통해 전달받아 제거")
    public void removeMemberWhenNewList() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        System.out.println(team);
        assertThat(team.getMembers()).size().isEqualTo(3);
        team.setMembers(team.getMembers().subList(0,2));

        Team saveTeam = teamRepository.saveAndFlush(team);

        entityManager.clear();

        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을").orElseThrow(() -> new RuntimeException("not found"));

        assertThat(findTeam.getMembers().size()).isEqualTo(2);
    }
```

**결과**

그렇기 때문에 원래 연결되어 있었지만 조회되지 않는 Entity( member - 영희) 는 Team - 떡잎마을에서 빠져

2명만 연관관계가 재연결된다.

```
2024-02-03T17:51:42.203+09:00 DEBUG 3750 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName 
        AND m.name != :memberName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=? 
            and m1_0.name<>?
com.example.fetchjoin.domain.Team@7b80ac30
2024-02-03T17:51:42.238+09:00 DEBUG 3750 --- [    Test worker] org.hibernate.SQL                        : 
    /* one-shot delete for com.example.fetchjoin.domain.Team.members */update member 
    set
        member_id=null 
    where
        member_id=?
2024-02-03T17:51:42.240+09:00 DEBUG 3750 --- [    Test worker] org.hibernate.SQL                        : 
    update
        member 
    set
        member_id=? 
    where
        id=?
2024-02-03T17:51:42.240+09:00 DEBUG 3750 --- [    Test worker] org.hibernate.SQL                        : 
    update
        member 
    set
        member_id=? 
    where
        id=?
2024-02-03T17:51:42.243+09:00 DEBUG 3750 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=?
```

**DB상 원래 존재하는 Member - 영희를 Team - 떡잎마을에 다시 추가한다면?**

```java
@Test
    @Transactional
    @DisplayName("DB상 원래 존재하는 Member - 영희를 Team - 떡잎마을에 다시 추가한다면?")
    public void addMember() {
        Team team = teamRepository.findByTeamExceptMemberName("떡잎마을", "영희")
                .orElseThrow(() -> new RuntimeException("not found"));

        assertThat(team.getMembers()).size().isEqualTo(3);
        Member member = memberRepository.findMemberByName("영희").orElseThrow(() -> new RuntimeException("not found"));
        team.getMembers().add(member);
        Team saveTeam = teamRepository.saveAndFlush(team);

        entityManager.clear();
        Team findTeam = teamRepository.findTeamByTeamName("떡잎마을")
                .orElseThrow(() -> new RuntimeException("not found"));

        assertThat(findTeam.getMembers().size()).isEqualTo(4);
    }
```

**테스트 결과**

DB상에서는 연관관계가 이미 연결되어있지만 인지하지 못하고 update 쿼리가 나간다.

```
2024-02-03T17:57:28.438+09:00 DEBUG 7874 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName 
        AND m.name != :memberName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=? 
            and m1_0.name<>?
2024-02-03T17:57:28.472+09:00 DEBUG 7874 --- [    Test worker] org.hibernate.SQL                        : 
    /* select
        m 
    from
        Member m 
    where
        m.name = :name */ select
            m1_0.id,
            m1_0.name 
        from
            member m1_0 
        where
            m1_0.name=?
2024-02-03T17:57:28.477+09:00 DEBUG 7874 --- [    Test worker] org.hibernate.SQL                        : 
    update
        member 
    set
        member_id=? 
    where
        id=?
2024-02-03T17:57:28.480+09:00 DEBUG 7874 --- [    Test worker] org.hibernate.SQL                        : 
    /* SELECT
        t 
    FROM
        Team t 
    JOIN
        
    FETCH
        t.members m 
    WHERE
        t.name = :teamName */ select
            t1_0.id,
            m1_0.member_id,
            m1_0.id,
            m1_0.name,
            t1_0.name 
        from
            team t1_0 
        join
            member m1_0 
                on t1_0.id=m1_0.member_id 
        where
            t1_0.name=?
```

### 무조건 사용하면 안될까? 언제 활용할 수 있을까?

- **일관성 문제가 없으면 사용해도 문제없다!**
    - 그렇다면 일관성을 해치는 경우는 어떤 경우가 있을까?
        - fetch join 대상을 where로 필터링 해서 사용하는 경우
            - Select m from Member m join fetch m.team t where t.name=:teamName
        - left join 사용할때 주의해야 한다.
            - Select m from Member m left join fetch m.team t where t.name=:teamName

- **일관성이 깨져도 엔티티를 변경하지 않고 딱! 조회용으로만 주의해서 사용하면 크게 문제는 없다.**
    - 조회 용도로만 사용해야한다 → readOnly를 사용해서 명시적으로 표현하면 좋을것 같다.
- fetch join의 결과는 연관된 모든 엔티티가 있을것이라 가정하고 사용해야 한다.