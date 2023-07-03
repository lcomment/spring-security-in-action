# 08. 권한 부여 구성: 제한 적용

> ### 서론

→ 특정한 요청 그룹에만 권한 부여 제약 조건 적용

- `MVC 선택기` : 경로에 MVC 식을 이용해 엔드포인트 선택
- `Ant 선택기` : 경로에 Ant 식을 이용해 엔드포인트 선택
- `정규식 선택기` : 경로에 정규식을 이용해 엔드포인트 선택

&nbsp; Spring Security 6.0 이상부터는 위의 세 선택기가 `requestMatchers()` 또는 `securityMatchers()`로 통일

## I. 선택기 메서드로 엔드포인트 선택

```java
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic()
                .authorizeHttpRequests((auth) ->
                        auth.requestMatchers("/endPoint1").hasRole("ADMIN")
                            .requestMatchers("/endPoint2").hasRole("MANAGER")
                            .anyRequest().permitAll()
                )
                .build();
    }
}
```

- ADMIN 역할의 사용자만 `/endPoint1` 호출 가능
- MANAGER 역할의 사용자만 `/endPoint2` 호출 가능
- 이외의 엔드포인트는 누구나 호출 가능

<br>

## II. MVC 선택기로 권한 부여할 요청 선택

> ### i) requestMatchers(String... patterns)

→ 경로만을 기준으로 권한 부여 제한 적용 (모든 HTTP 방식 제한)

```java
public C requestMatchers(String... patterns) {
	return requestMatchers(null, patterns);
}
```

> ### ii) requestMatchers(HttpMethod method, String... patterns)

→ 제한을 적용할 HTTP 방식과 경로를 모두 지정

```java
public C requestMatchers(HttpMethod method, String... patterns) {
	List<RequestMatcher> matchers = new ArrayList<>();

    if (mvcPresent) {
		matchers.addAll(createMvcMatchers(method, patterns));
	} else {
		matchers.addAll(RequestMatchers.antMatchers(method, patterns));
	}

    return requestMatchers(matchers.toArray(new RequestMatcher[0]));
}
```

> ### iii) 예제

```java
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic()
                .authorizeHttpRequests((auth) ->
                        auth.requestMatchers(HttpMethod.GET, "/get").authenticated()
                            .requestMatchers(HttpMethod.POST, "/post").authenticated()
                            .requestMatchers(HttpMethod.PUT, "/put").authenticated()
                            .requestMatchers(HttpMethod.GET, "/delete").authenticated()
                            .anyRequest().denyAll()
                )
                .build();
    }
}
```

> ### iv) 경로식

- `*` : 한 경로 이름만 대체
- `**` : 여러 경로 이름 대체

| 식               | 설명                                           |
| ---------------- | ---------------------------------------------- |
| /a               | /a 경로만                                      |
| /a/\*            | /a/b, /a/c 등의 경로만                         |
| /a/\*\*          | /a, /a/b, /a/b/c 등의 경로                     |
| /a/{param}       | 주어진 경로 매개 변수를 포함한 /a 경로 적용    |
| /a/{param:regex} | 매개변수 값과 주어진 정규식이 일치할 때만 적용 |

<br>

## III. 앤트 선택기로 권한을 부여할 요청 선택

&nbsp; mvcMatchers는 `/a`로 경로를 지정했을 때 `/a`만을 허용한다. 하지만 antMatchers는 `/a`와 `/a/` 모두 허용한다. Spring Boot 3.x부터 적용되는 Spring Security 6.0 이상의 버전에서 사용되는 requestMatchers의 경우, mvcMatchers와 같이 `/a`만을 허용한다.

> ### i) requestMatchers(MttpMethod method)

→ 경로 관계없이 특정 HTTP 방식 지정

```java
public C requestMatchers(HttpMethod method) {
	return requestMatchers(method, "/**");
}
```

<br>

## IV. 정규식 선택기로 권한을 부여할 요청 선택

```java
@Configuration
public class SecurityConfig {
    @Bean
    SecurityFilterChain oauth2SecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic()
                .authorizeHttpRequests((auth) ->
                        auth..requestMatchers(new RegexRequestMatcher("/\\d+", null)).authenticated()
                )
                .build();
    }
}
```

→ 위과 같이 RegexRequestMatcher 객체를 생성해 정규식 적용
