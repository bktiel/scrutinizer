package com.example.medevac.config;

import com.example.medevac.services.NineLineUserDetailsService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig
{
    @Bean
    public SecurityFilterChain securityFilterChain (HttpSecurity http) throws Exception
    {

        return http
            .csrf (AbstractHttpConfigurer::disable)
            .cors (cors -> cors.configurationSource (corsConfigurationSource ()))
            .authorizeHttpRequests ((request) -> request
                .requestMatchers (HttpMethod.GET, "/api/v1/validateSession").permitAll ()
                .requestMatchers (HttpMethod.POST, "/api/v1/medevac").permitAll ()
                .anyRequest ().authenticated ()
            )
            .formLogin ((formLogin) -> formLogin
                .loginProcessingUrl ("/api/v1/loginUser")
                .failureHandler (new SimpleUrlAuthenticationFailureHandler ())
                .successHandler (this::handleLoginSuccess)
            )
            .logout ((logout) ->
            {
                logout.logoutUrl ("/api/v1/logoutUser");
                logout.deleteCookies ("JSESSIONID");
                logout.deleteCookies ("userDetail");
                logout.logoutSuccessHandler ((new HttpStatusReturningLogoutSuccessHandler (HttpStatus.OK)));
            })
            .authenticationProvider (authenticationProvider ())
            .build ();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource ()
    {
        CorsConfiguration configuration = new CorsConfiguration ();
        configuration.setAllowedOrigins (List.of ("http://localhost:3000"));
        configuration.setAllowedMethods (List.of ("GET", "POST", "OPTIONS", "DELETE", "PATCH"));

        var source = new UrlBasedCorsConfigurationSource ();

        source.registerCorsConfiguration ("/**", configuration);

        return source;
    }

    protected void handleLoginSuccess (HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws JsonProcessingException
    {
        Base64.Encoder encoder = Base64.getEncoder ();
        ObjectMapper objectMapper = new ObjectMapper ();
        var newCookie = new Cookie ("userDetail",
            encoder.encodeToString (
                objectMapper.writeValueAsString (
                    authentication.getAuthorities ()
                ).getBytes (StandardCharsets.UTF_8)
            ));
        newCookie.setPath ("/");
        response.setStatus (200);
        response.addCookie (newCookie);
    }

    @Bean
    UserDetailsService userDetailsService ()
    {
        return new NineLineUserDetailsService ();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder ()
    {
        return new BCryptPasswordEncoder ();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider ()
    {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider ();
        authProvider.setUserDetailsService (userDetailsService ());
        authProvider.setPasswordEncoder (passwordEncoder ());

        return authProvider;
    }
}
