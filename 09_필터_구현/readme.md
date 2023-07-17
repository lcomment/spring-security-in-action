# 09. 필터 구현

> ### 서론

- Spring Security의 HTTP 필터 → 요청에 적용해야 하는 각 책임 관리 및 책임의 체인 형성
- 필터 → 요청 수신 및 논리 실행, 다음 필터에 요청 위임
- Spring Security는 필터체인을 원하는 방식으로 모델링할 수 있는 유연성 제공

## I. 스프링 시큐리티 아키테처의 필터 구현

- javax.servlet 패키지의 `Filte` 인터페이스를 구현
  - `doFilter()` 메서드 재정의
  - `ServletRequest`: HTTP 요청, 요청에 대한 세부 정보를 얻음
  - `ServletResponse`: HTTP 응답, 필터체인에서 응답 변경
  - `FilterChain`: 필터 체인, 체인의 다음 필터로 요청 전달
- Spring Security의 FilterChain
  - `BasicAuthenticationFilter`: HTTP Basic 인증 처리
  - `CsrfFilter`: CSRF(사이트 간 요청 위조) 처리
  - `CorsFilter`: CORS(교차 출처 리소스 공유) 권한 부여 규칙 처리

```java
// httpBasic
http.httpBasic()

// csrf → csrfTokenRepository를 통해 XSRF-TOKEN 쿠키에 CSRF 토큰 유지
.csrf()
.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())

// cors
@Bean
CorsConfigurationSource corsConfigurationSource() {
	CorsConfiguration configuration = new CorsConfiguration();
	configuration.setAllowedOrigins(Arrays.asList("https://example.com"));
	configuration.setAllowedMethods(Arrays.asList("GET","POST"));
	UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
	source.registerCorsConfiguration("/**", configuration);
	return source;
}
```

- 각 필터에는 순서 번호가 있음 → 인덱스
- 같은 위치에 필터 두 개 이상 추가 가능
- 여러 필터가 같은 위치에 있으면 필터 호출 순서가 정해지지 않음

<br>

## II. 체인에서 기죤 필터 앞에 필터 추가

> ### i) 필터 생성

```java
public class RequestValidationFilter implements Filter {
    @Override
    public void doFilter(ServletRequest servletRequestm ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String requestId = httpRequest.getHeader("Request-Id");

        if(requestId == null || requestId.isBlank()) {
            httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
```

> ### ii) addFilterBefore()로 인증 전에 필터 실행시키기

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RequestValidationFilter requestValidationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .addFilterBefore(requestValidationFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll();)
                .build();
    }
}
```

<br>

## III. 체인에서 기존 필터 뒤에 필터 추가

> ### i) 로깅 필터 생성

```java
public class AuthenticationLoggingFilter implements Filter {
    private final Logger logger = Logger.getLogger(AuthenticationLoggingFilter.class.getName());

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestId = httpRequest.getHeader("Request-Id");

        logger.info("Successfully authenticated request with id: " + requestId);

        filterChain.doFilter(request, response);
    }
}
```

> ### ii) addFilterAfter()로 인증 후에 필터 실행시키기

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final RequestValidationFilter requestValidationFilter;
    private final AuthenticationLoggingFilter authenticationLoggingFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .addFilterBefore(requestValidationFilter, BasicAuthenticationFilter.class)
                .addFilterAfter(nauthenticationLoggingFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll();)
                .build();
    }
}
```

<br>

## IV. 필터 체인의 다른 필터 위치에 필터 추가

- 기존 필터가 수행하는 책임에 대해 다른 구현을 제공할 때 적합
  - 인증을 위한 정적 헤더 값에 기반을 둔 식별
  - 대칭 키를 이용해 인증 요청 서명
  - 인증 프로세스에 OTP 이용
- 필터 실행 순서 보장 X

> ### i) 인증을 위한 정적 헤더 값에 기반을 둔 식별

```java
public class StaticKeyAuthenticationFilter implements Filter {
    @Value("${authorization.key}")
    private String authorizationKey;

    @Override
    public void doFilter(ServletRequest servletRequestm ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String authorization = httpRequest.getHeader("Authorization");

        if(authorizationKey.equals(authorization)) {
            filterChain.doFilter(request, response);
        } else {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
```

> ### ii) addFilterAt() 메서드를 이용해 다른 필터 위치에 추가

```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final StaticKeyAuthenticationFilter staticKeyAuthenticationFilter;

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .addFilterAt(staticKeyAuthenticationFilter, BasicAuthenticationFilter.class)
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll();)
                .build();
    }
}
```

<br>

## V. 스프링 시큐리티가 제공하는 필터 구현

> ### OncePerRequestFilter

- Spring Security에서 기본적으로 제공하는 Filter 인터페이스를 구현하는 클래스
- GenericFilterBean을 확장하는 더 유용한 클래스
  - GenericFilterBean을 확장하면 필요할 때 web.xml 설명자 파일에 정의하여 초기화 매개변수 사용 가능
- 요청당 한번만 실행
  - 프레임워크는 필터체인에 추가한 필터를 요청당 한번만 실행하도록 보장하진 않음
- 사용 시나리오: 로깅 기능 등

```java
public class AuthenticationLoggingFilter extends OncePerRequestFilter {
    private final Logger logger = Logger.getLogger(AuthenticationLoggingFilter.class.getName());

    @Override
    public void doFilterInterrnal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;

        String requestId = request.getHeader("Request-Id");

        logger.info("Successfully authenticated request with id: " + requestId);

        filterChain.doFilter(request, response);
    }
}
```

- HTTP 요청만 지원하지만, 사실 항상 이것만 이용한다.
  - 직접 HttpServletRequest, HttpServletResponse로 형변환
- 필터가 적용될지 결정하는 논리를 구현할 수 있다.
  - 추가한 필터가 특정 요청에는 적용되지 않게 결정 가능
  - `shouldNotFilterr(HttpServletRequest)` 재정의
- OncePerRequestFilter가 기본저으로 비동기 요청이나 오류 발송 요청에는 적용되지 않는다.
  - 이 동작에 대해 변경하기 위해선 `shouldNotFilterAsyncDispatch()`, `shouldNotFilterErrorDispatch()` 메서드 재정의
