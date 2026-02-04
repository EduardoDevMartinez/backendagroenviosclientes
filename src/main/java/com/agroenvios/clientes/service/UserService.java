package com.agroenvios.clientes.service;

import com.agroenvios.clientes.dto.user.ResponseUser;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PhoneVerificationService phoneVerificationService;
    private final MinioService minioService;

    private User getCurrentUserEntity() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private ResponseUser toResponse(User user) {
        return ResponseUser.builder()
                .id(user.getId())
                .username(user.getUsername())
                .nombre(user.getNombre())
                .paterno(user.getPaterno())
                .materno(user.getMaterno())
                .correo(user.getCorreo())
                .telefono(user.getTelefono())
                .foto(user.getFoto())
                .isEmailVerified(user.getIsEmailVerified())
                .isTelefonoVerified(user.getIsTelefonoVerified())
                .isActive(user.getIsActive())
                .build();
    }

    public ResponseUser getCurrentUser() {
        return toResponse(getCurrentUserEntity());
    }

    public ResponseUser updateUser(String nombre, String paterno, String materno, String telefono, MultipartFile file) {
        User user = getCurrentUserEntity();

        if (nombre != null) user.setNombre(nombre);
        if (paterno != null) user.setPaterno(paterno);
        if (materno != null) user.setMaterno(materno);
        if (telefono != null) user.setTelefono(telefono);
        if (file != null && !file.isEmpty()) {
            String url = minioService.upload(file, "usuario/foto", user.getUsername());
            user.setFoto(url);
        }

        return toResponse(userRepository.save(user));
    }

    public ResponseEntity<String> requestPhoneCode() {
        return phoneVerificationService.requestCode(getCurrentUserEntity());
    }

    public ResponseEntity<String> verifyPhone(String code) {
        return phoneVerificationService.verifyPhone(getCurrentUserEntity(), code);
    }
}
