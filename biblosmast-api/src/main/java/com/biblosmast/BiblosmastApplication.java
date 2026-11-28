package com.biblosmast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// Se excluye el usuario de prueba que Spring Security crea por defecto: el login lo hace AuthService.
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class BiblosmastApplication {
    public static void main(String[] args) {
        SpringApplication.run(BiblosmastApplication.class, args);
    }
}
