# 문제점

- 요청을 받으면 Controller단에서 validation을 한번 해줘야 하는 경우가 있다.
- validation 시에 Pattern annotation을 활용해 정규식으로 검사하거나
- Size, isEmpty, isNull 등등 annotation을 활용하여 taget을 정해 검사를 진행한다.

**이때 한 마라미터만 검사하는 것이 아닌 여러파라미터를 함께 조건으로 걸어 여러 상황에 따라 validation을 달리 가져가고 싶다.!**

# 분석

### 1. Contstraint 어노테이션

- ConstraintValidator를 상속받은 클래스를 전달해줄 수 있다.
- 여러개의 ConstraintValidator상속 클래스를 전달 할 수 있다.
- 
![1.png](image%2F1.png)

### 2. ConstraintValidator

- valid 조건을 구현해주어야 하는 Validator이다
- 특정 Annotation을 통해 Validator를 적용시킬 수 있고, 적용할 Class target을 설정할 수 있다.
- initialize 메소드를 통해 validator를 초기화
- isValid메소드를 통해 검증
- 
![2.png](image%2F2.png)

# 구현.

user가 회원가입하는 상황을 예를 들어볼 것이다.

userType 이 manager일 경우에만 name과 phone을 필수로 받을것이다

## 1. Annotation 생성

검증을 위한 어노테이션을 만들어준다.

- @Target(FIELD): 필드에 설정할 것 이므로 FIELD를 선택해준다.
- @Retention(RUNTIME): 해당 어노테이션이 유지되는 시간으로써 런타임까지 유효함을 설정
- @Constraint(validatedBy =UserValidator.class): UserValidator를 통해 유효성 검사를 진행
- @Documented: JavaDoc 생성시 Annotation에 대한 정보도 함께 생성

```java
@Documented
@Constraint(validatedBy = UserValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserRegisterConstraint {

    String message() default "preSchool name is not validate";

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };

}
```

## 2. Validator 구현

```java
public class UserValidator implements ConstraintValidator<UserRegistrationConstraint, UserRequest> {

    @Override
    public boolean isValid(UserRequest userRequest, ConstraintValidatorContext context) {

        String userType = userRequest.getUserType();
        String name = userRequest.getName();
        String phone = userRequest.getPhone();

        if(!userType.equals("manager")){
            return true;
        }

        return StringUtils.hasText(name) && StringUtils.hasText(phone);
    }

}
```

- userType이 manager이면 name과 phone은 null이거나 empty여도 상관없기 때문에 true로 return한다.
- userType이 manager라면 name과 phone의 값을 검증한 값을 return 해준다.
- 여기서 globalExceptionHandler가 존재한다면  exception을 바로 던져도 무관할 것 같다.
- initialize 메소드를 통해 validator를 초기화할 때 다른 작업을 할 수도 있지만 필요하지 않아 override하지 않았다
# 검증

```java
@ExtendWith(SpringExtension.class)
public class UserControllerTests {

    private MockMvc mockMvc;

    @InjectMocks
    UserController userController;

    private Gson gson;

    @BeforeEach
    public void before() {
        MockitoAnnotations.initMocks(this);
        createStandAloneMvc();

    }

    private void createStandAloneMvc(){

        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .build();

        gson = new GsonBuilder().create();

    }

    @Test
    public void userRegistrationTest() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                                .userType("manager")
                                .username("ysys3131")
                                .password("0000")
                                .name("yoonsoo")
                                .phone("01012340000")
                        .build()))
        ).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("userType이 manager일때는 name이 null이면 실패")
    public void userRegistrationFailWhenUserTypeManagerAndNameIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("manager")
                        .username("ysys3131")
                        .password("0000")
                        .phone("01012340000")
                        .build()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("userType이 manager일때는 phone이 null이면 실패")
    public void userRegistrationFailWhenUserTypeManagerAndPhoneIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("manager")
                        .username("ysys3131")
                        .password("0000")
                        .name("yoonsoo")
                        .build()))
        ).andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("userType이 manager가 아닐때 phone은 null이어도 성공")
    public void userRegistrationSuccessWhenUserTypeGeneralAndPhoneIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("general")
                        .username("ysys3131")
                        .password("0000")
                        .name("yoonsoo")
                        .build()))
        ).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("userType이 manager가 아닐때 name은 null이어도 성공")
    public void userRegistrationSuccessWhenUserTypeGeneralAndNameIsNull() throws Exception {

        mockMvc.perform(post("/user")
                .contentType(MediaType.APPLICATION_JSON)
                .content(gson.toJson(UserRequest.builder()
                        .userType("general")
                        .username("ysys3131")
                        .password("0000")
                        .phone("01012340000")
                        .build()))
        ).andExpect(status().isCreated());
    }

}
```

위아 같이 테스트 코드를 작성하고 모두 성공하는 모습을 확인할 수 있다.

- 검증시에 다른 방면으로도 유용하게 사용할 수 있을 것 같다.
- 다른 로직과 섞이지 않고 검증만 해주고 다른 곳에서 신경쓰지 않아도 되니 효과적이라고 생각된다.
- 관심사가 잘 분리되었다고 생각된다.