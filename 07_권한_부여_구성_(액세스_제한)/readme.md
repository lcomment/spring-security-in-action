# 07. 권한 부여 구성: 액세스 제한

> ### 서론

- Authorization (권한 부여)
  - 식별된 클라이언트가 요청된 리소스에 액세스할 권한이 있는지 시스템이 결정하는 프로세스
- 스프링 시큐리티의 동작원리
  - `인증 흐름`을 완료한 후 요청을 `권한 부여 필터`에 위임
  - 필터 → 구성된 `권한 부여 규칙`에 따라 요청 허용 및 거부

<br>

## I. 권한과 역할에 따라 접근 제한

> 사용자는 가진 권한에 따라 특정 작업만 실행한다!

권한 : 사용자가 시스템 리소스로 수행할 수 있는 작업

**_GrantedAuthority 계약_**

```java
public interface GrantedAuthority extends Serializable {
    String getAuthority();
}
```

**_UserDetails_** 의 `getAuthorities()`

```java
public interface UserDetails extends Serializable {
    Collection<? extends GrantedAuthority> getAuthorities();

    . . .
}
```

<br>

## II. 사용자 권한을 기준으로 모든 엔드포인트에 접근 제한

- `hasAuthority()`
  - 애플리케이션이 제한을 구성하는 `하나의 권한`만 매개변수로 받음
- `hasAnyAuthority()`
  - 애플리케이션이 제한을 구성하는 `권한을 하나 이상` 받을 수 있음
- `access()`
  - SpEl을 기반으로 권한 부여 규칙 구축
  - 접근을 허가하는 조건을 복잡한 식으로 작성해야 하는 시나리오에서 유연한 장점을 보임

```java
@Configuration
public class ProjectConfig {
    @Bean
    public UserDetailsService userDetailsService() {
        var manager = new InMemoryUserDetailsManager();

        var user1 = User.withUsername("john")
            .password("12345")
            .authorities("READ")
            .build();

        var user2 = User.withUsername("jane")
            .password("12345")
            .authorities("WRITE")
            .build();

        manager.createUser(user1);
        manager.createUser(user2);

        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
```

> ### i) hasAuthority() 사용

```java
@Configuration
public class ProjectConfig {
    . . .

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests((auth) ->
                        auth
                                .anyRequest()
                                .hasAuthority("WRITE"))
                .build();
    }
}
```

<br>

> ### ii) hasAnyAuthority() 사용

```java
@Configuration
public class ProjectConfig {
    . . .

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests((auth) ->
                        auth
                                .anyRequest()
                                .hasAnyAuthority("WRITE", "READ"))
                .build();
    }
}
```

<br>

> ### iii) access() 사용

```java
@Configuration
public class ProjectConfig {
    . . .

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests((auth) ->
                        auth
                                .anyRequest()
                                .access(new WebExpressionAuthorizationManager("hasAuthority('read') and !hasAuthority('delete')")))
                .build();
    }
}
```

- Spring Boot 3.0 이상에서는 `WebExpressionAuthorizationManager` 사용

<br>

## III. 사용자 역할을 기준으로 모든 엔드포인트에 대한 접근을 제한

> 역할 : 사용자에게 작업 그룹에 속한 이용 권리 제공

- `hasRole()`
  - 애플리케이션이 요청을 승인할 `하나의 역할`을 매개변수로 받음
- `hasAnyRole()`
  - 애플리케이션이 요청을 승인할 `여러 역할`을 매개변수로 받음
- `access()`
  - SpEl을 기반으로 역할 부여 규칙 구축

```java
@Configuration
public class ProjectConfig {
    @Bean
    public UserDetailsService userDetailsService() {
        var manager = new InMemoryUserDetailsManager();

        var user1 = User.withUsername("john")
            .password("12345")
         // .authorities("ROLE_ADMIN")
            .roles("ADMIN")
            .build();

        var user2 = User.withUsername("jane")
            .password("12345")
         // .authorities("ROLE_ADMIN")
            .roles("MANAGER")
            .build();

        manager.createUser(user1);
        manager.createUser(user2);

        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return NoOpPasswordEncoder.getInstance();
    }
}
```

> ### i) hasRole() 사용

```java
@Configuration
public class ProjectConfig {
    . . .

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests((auth) ->
                        auth
                                .anyRequest()
                                .hasRole("ADMIN"))
                .build();
    }
}
```

<br>

> ### ii) hasAnyRole() 사용

```java
@Configuration
public class ProjectConfig {
    . . .

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .httpBasic(Customizer.withDefaults())
                .authorizeHttpRequests((auth) ->
                        auth
                                .anyRequest()
                                .hasAnyRole("ADMIN", "MANAGER"))
                .build();
    }
}
```

<br>

## IV. 모든 엔드포인트에 대한 접근 제한

→ `denyAll()` 메서드
