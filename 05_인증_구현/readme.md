# 05. 인증 구현

> ### 서론)

- AuthenticationManager → HTTP 필터 계층에서 요청 수신 및 책임 위임
- AuthenticationProvider 계층 → 인증 논리 담당

<br>

## I. AuthenticationProvider의 이해

- 프레임워크의 목적 → 어떠한 `시나리오`가 필요하더라도 `구현`할 수 있게 해주는 것
- Spring Security에서는 `AuthenticationProvider 계약`으로 모든 `맞춤형 인증 논리` 정의 가능

> ### i) 인증 프로세스 중 요청 나타내기

**_Authentication_**

- 맞춤형 AuthenticationProvider를 구현하려면, `인증 이벤트` 자체를 먼저 이해해야 함
- Authentication은 인증 프로세스의 필수 interface (`인증 요청 이벤트`)
- 애플리케이션에 접근을 요청한 엔티티의 세부 정보를 담음

```java
public interface Authentication extends Principal, Serializable {

    Collection<? extends GrantedAuthority> getAuthorities();
    Object getCredentials();
    Object getDetails();
    Object getPrincipal();
    boolean insAuthenticated();
    void setAuthenticated(boolean isAuthenticated) throws IllegalArgumentException;
}
```

- Authentication 계약은 주체뿐 아니라 인증 프로세스 완료여부, 권한 컬렉션 등의 정보를 추가로 가짐
- Principal 인터페이스의 `getName()` → 인증하려는 사용자의 이름 반환
  - Principal : 애플리케이션에 접근을 요청하는 사용자
  - Java Security API → Authentication에서 구현하면서 유연성 증가
- `getCredentials()` → 인증 프로세스에 이용된 암호나 비밀 반환
- `getAuthorities()` → 인증된 요청에 허가된 권한의 컬렉션 반환
- `getDetails()` → 요청에 대한 추가 세부정보 반환
- `isAuthenticated()` → 인증 프로세스가 끝났으면 true, 진행중이면 false

<br>

> ### ii) 맞춤형 인증 논리 구현

&nbsp; 인증 논리를 처리하는 AuthenticationProvider는 시스템의 `사용자를 찾는 책임`을 `UserDetailsService`에 위임하고, `PasswordEncoder`로 인증 프로세스에서 `암호를 관리`한다.

```java
public interface AuthenticationProvider {
    Authentication authenticate(Authentication authentication) throws AuthenticationException;
    boolean supports(Class<?> authentication);
}
```

- `authenticate()` → 인증 논리 정의
  - 인증 실패 시 `AuthenticationException` 발생
  - 현재 AuthenticationProvider 구현에서 지원되지 않는 인증 객체를 받으면 null 리턴
  - 완전히 인증된 객체를 나타내는 Authentication 인스턴스를 반환해야 함
    - isAuthenticated() → true 리턴
    - 인증된 엔티티의 모든 필수 세부 정보 포함
    - 민감한 데이터는 제거
- `supports()`
  - 현재 AuthenticationProvider가 Authentication 객체로 제공된 형식을 지원하면 true 리턴
  - 이 메서드에서 true를 반환해도, `authenticate()` 메서드가 null 반환으로 요청 거부 가능
  - 인증 유현뿐 아니라 `요청의 세부 정보`를 기준으로 인증 요청을 거부하는 AuthenticationProvider를 구현할 수 있도록 설계됨

<br>

> ### iii) 맞춤형 인증 논리 적용

```java
@RequiredArgsConstructor
@Component
public class CustomAuthenticationProvider implements AuthenticationProvider {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String password = authentication.getCredentials().toString();

        UserDetails u = userDetailsService.loadUserByUsername(username);

        if(passwordEncoder.matches(password, u.getPassword())) {
            return new UsernamePasswordAuthenticationToken(username, password, u.getAuthorities());
        }
        throw new BadCredentialsException("Something went wrong!");
    }

    /**
     * UsernamePasswordAuthenticationToken: Authentication 인터페이스의 한 구현이며, 사용자 이름과 암호를 이용하는 표준 인증 요청을 나타냄
     * 인증 필터 수준에서 아무것도 맞춤 구성하지 않으면 UsernamePasswordAuthenticationToken 클래스가 형식 정의
     */
    @Override
    public boolean supports(Class<?> authentication) {
        return authentication.equals(UsernamePasswordAuthenticationToken.class);
    }
}
```

**_AuthenticationProvider 설정_**

- Spring Security 5버전
  - void configure(AuthenticationManagerBuilder auth) 메서드 재구현
  - auth.authenticationProvider(authenticationProvider)
- Spring Security 6버전 이상
  - AuthenticationManager authenticationManager(AuthenticationManagerBuilder auth) 메서드 구현
  - return auth.authenticationProvider(authenticationProvider).build();

<br>

## II. SecurityContext 이용

```java
public interface SecurityContext extends Serializable {
    Authentication getAuthentication();
    void setAuthentication(Aythentication authentication);
}
```

- 주 책임 → Authentication 객체 저장
- SecurityContextHolder
  - SecurityContext를 관리하는 객체
  - SecurityContext를 관리하는 세 가지 전략 제공

**_MODE_THREADLOCA_**

- 각 스레드가 보안 컨텍스트에 각자의 세부 정보를 저장할 수 있게 해줌
- 스레드 방식의 웹 애플리케이션에서는 각 요청이 개별 스레드를 가짐

**_MODE_INHERITABLETHREADLOCAL_**

- MODE_THREADLOCAL과 비슷
- 비동기 메서드의 경우 보안 컨텍스트를 `다음 스레드로 복사`하도록 Spring Security에 지시
- `@Async` 메서드를 실행하는 새 스레드가 보안 컨텍스트를 상속하게 할 수 있음

**_MODE_GLOBAL_**

- 애플리케이션의 모든 스레드가 같은 보안 컨텍스트 인스턴스를 보게 함

> ### i) 보안 컨텍스트를 위한 보유 전략 이용

```java
@GetMapping("/hello")
public String hello(Authentication a) {
    return a.getName();
}
```

→ 올바른 사용자로 엔드포인트르 호출 시 정상 작동

<br>

> ### ii) 비동기 호출을 위한 보유 전략 이용

- 요청당 여러 스레드가 사용될 때 상황이 복잡해짐
- 엔드포인트가 `비동기`가 되면 메서드를 실행하는 스레드와 요청을 수행하는 스레드가 다름

<br>

```java
@GetMapping("/bye")
@Async
public void goodbye() {
    SecurityContext context = SecurityContextHolder.getContext();
    String username = context.getAuthentication().getName();
}
```

- username을 받아오는 Line에서 `NullPointerException` 발생
- 보안 컨텍스트를 상속하지 않는 다른 스레드에서 실행되기 때문 (`Authentication이 null 상태`)
- MODE_INHERITABLETHREADLOCAL 전략으로 해결 가능
- But. 프레임워크가 자체적으로 스레드를 만들 때만 작동

```java
@Configuration
@EnableAsync
public class ProjectConfig {

    @Bean
    public InitializaingBean initializingBean() {
        return () -> SecurityContextHolder.setStrategyName(
            SecurityContextHolder.MODE_INHERITABLETHREADLOCAL);
    }

}
```

<br>

> ### iii) 독립형 애플리케이션을 위한 보유 전략 이용

```java
@Bean
public InitializaingBean initializingBean() {
    return () -> SecurityContextHolder.setStrategyName(
        SecurityContextHolder.MODE_GLOBAL);
}
```

- 모든 스레드에 공유되는 전략
- 독릭형 애플리케이션에서 유용히 사용
- 애플리케이션의 모든 스레드가 SecurityContext 접근 가능 → 직접 동시 접근을 해결해줘야 함

<br>

## III. HTTP Basic 인증과 양식 기반 로그인 인증 이해하기

> ### i) HTTP Basic 이용 및 구성

**_Spring Security Config_**

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(c -> {
                    c.realmName("OTHER");
                    c.authenticationEntryPoint(new CustomEntryPoint());
                })
                .authorizeHttpRequests().anyRequest().authenticated()
                .and()
                .build();
    }
}
```

- HTTP Basic 인증은 간단하지만, 모든 실제 시나리오에 적합하지 않음
- 영역(realm)
  - 특정 인증 방식을 이용하는 보호 공간
- AuthenticationEntryPoint
  - 인증이 실패했을 때의 응답 맞춤 구성
  - commence() 메서드로 구성

<br>

**_AuthenticationEntryPoint 커스텀_**

```java
public class CustomEntryPoint implements AuthenticationEntryPoint {


    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.addHeader("message", "Luke, I am your father!");
        response.sendError(HttpStatus.UNAUTHORIZED.value());
    }
}
```

<br>

**_엔드포인트 호출 시 결과_**

```shell
# request
curl -v http://localhost:8080/hello

# response
...
< HTTP/1.1 401
< Set-Cookie: JSESSIONID=621C8C4FFD46426BE74429B8222B7783; Path=/; HttpOnly
< message: Luke, I am your father!
...
```

<br>

> ### ii) 양식 기반 로그인으로 인증 구현

**_Spring Security Config_**

```java
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {
    private final AuthenticationSuccessHandler authenticationSuccessHandler;
    private final AuthenticationFailureHandler authenticationFailureHandler;
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .formLogin()
                    .defaultSuccessUrl("/home", true)
                    .successHandler(authenticationSuccessHandler)
                    .failureHandler(authenticationFailureHandler)
                .and()
                .authorizeHttpRequests()
                    .anyRequest().authenticated()
                .and()
                .httpBasic()
                .and()
                .build();
    }
}
```

- 로그인하지 않고 아무 경로에 접근하려고 하면 자동으로 로그인 페이지로 `리다이렉션`
- formLogin()
  - FormLoginConfigurer<HttpSecurity> 형식의 객체 반환

<br>

**_AuthenticationSuccessHandler 커스텀_**

```java
@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        var authorities = authentication.getAuthorities();
        var auth = authorities.stream()
                .filter(a -> a.getAuthority().equals("read"))
                .findFirst();

        if(auth.isPresent()) {
            response.sendRedirect("/home");
        } else {
            response.sendRedirect("/error");
        }
    }
}
```

<br>

**_AuthenticationFailureHandler 커스텀_**

```java
@Component
public class CustomAuthenticationFailureHandler implements AuthenticationFailureHandler {
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        response.setHeader("failed", LocalDateTime.now().toString());
    }
}
```
