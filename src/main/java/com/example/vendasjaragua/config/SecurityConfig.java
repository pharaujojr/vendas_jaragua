package com.example.vendasjaragua.config;

import com.example.vendasjaragua.model.Usuario;
import com.example.vendasjaragua.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/login") // Apenas ignora CSRF no endpoint de login
            )
            .cors(cors -> cors.configurationSource(request -> {
                CorsConfiguration config = new CorsConfiguration();
                config.setAllowCredentials(true);
                config.addAllowedOriginPattern("*"); 
                config.addAllowedHeader("*");
                config.addAllowedMethod("*");
                return config;
            }))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login.html", "/login", "/css/**", "/js/**", "/images/**", "/contorno_pequeno.svg", "/solturi.svg").permitAll()
                .requestMatchers("/usuarios.html", "/api/usuarios/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/", true)
                .failureUrl("/login.html?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/login.html")
                .permitAll()
            )
            .rememberMe(remember -> remember
                .key("AppVendasJaraguaUniqueKey123")
                .tokenValiditySeconds(86400 * 30)
            )
            .sessionManagement(session -> session
                .maximumSessions(100)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService(UsuarioRepository usuarioRepository) {
        return username -> {
            Usuario usuario = usuarioRepository.findById(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

            return User.builder()
                .username(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRole())
                .build();
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // Initialize or Update default users
    @Bean
    public CommandLineRunner initUser(UsuarioRepository repository, PasswordEncoder encoder) {
        return args -> {
            // Usuário solturi
            Usuario user = new Usuario("solturi", encoder.encode("Solturi2025."), "ADMIN");
            repository.save(user);
            System.out.println("Usuário 'solturi' atualizado/criado com sucesso.");
            
            // Usuário admin
            Usuario admin = new Usuario("admin", encoder.encode("@Solturi#2025@"), "ADMIN");
            repository.save(admin);
            System.out.println("Usuário 'admin' atualizado/criado com sucesso.");
        };
    }
}
