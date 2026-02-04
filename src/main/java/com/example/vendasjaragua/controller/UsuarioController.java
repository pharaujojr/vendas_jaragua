package com.example.vendasjaragua.controller;

import com.example.vendasjaragua.model.Usuario;
import com.example.vendasjaragua.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UsuarioController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> getAllUsuarios() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        // Remove a senha para não expor no JSON
        usuarios.forEach(u -> u.setPassword("***"));
        return ResponseEntity.ok(usuarios);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createUsuario(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            String role = request.get("role");

            if (username == null || username.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Username é obrigatório"));
            }
            if (password == null || password.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password é obrigatório"));
            }
            if (role == null || role.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Role é obrigatório"));
            }

            if (usuarioRepository.existsById(username)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "Usuário já existe"));
            }

            Usuario usuario = new Usuario();
            usuario.setUsername(username.trim());
            usuario.setPassword(passwordEncoder.encode(password));
            usuario.setRole(role.trim().toUpperCase());

            Usuario saved = usuarioRepository.save(usuario);
            saved.setPassword("***"); // Não retornar senha
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao criar usuário: " + e.getMessage()));
        }
    }

    @PutMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUsuario(@PathVariable String username, @RequestBody Map<String, String> request) {
        try {
            Usuario usuario = usuarioRepository.findById(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

            String newPassword = request.get("password");
            String newRole = request.get("role");

            if (newPassword != null && !newPassword.trim().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(newPassword));
            }
            if (newRole != null && !newRole.trim().isEmpty()) {
                usuario.setRole(newRole.trim().toUpperCase());
            }

            Usuario updated = usuarioRepository.save(usuario);
            updated.setPassword("***");
            return ResponseEntity.ok(updated);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao atualizar usuário: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUsuario(@PathVariable String username) {
        try {
            if (!usuarioRepository.existsById(username)) {
                return ResponseEntity.notFound().build();
            }
            usuarioRepository.deleteById(username);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Erro ao deletar usuário: " + e.getMessage()));
        }
    }
}
