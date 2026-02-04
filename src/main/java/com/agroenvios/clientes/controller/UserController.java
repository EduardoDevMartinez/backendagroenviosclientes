package com.agroenvios.clientes.controller;

import com.agroenvios.clientes.dto.user.ResponseUser;
import com.agroenvios.clientes.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ResponseUser> getCurrentUserData() {
        return ResponseEntity.ok(userService.getCurrentUser());
    }

    @PutMapping("/update")
    public ResponseEntity<ResponseUser> updateUser(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String paterno,
            @RequestParam(required = false) String materno,
            @RequestParam(required = false) String telefono,
            @RequestParam(name = "file", required = false) MultipartFile file
    ) {
        return ResponseEntity.ok(userService.updateUser(nombre, paterno, materno, telefono, file));
    }

    @PostMapping("/phone/request-code")
    public ResponseEntity<String> requestPhoneCode() {
        return userService.requestPhoneCode();
    }

    @PostMapping("/phone/verify")
    public ResponseEntity<String> verifyPhone(@RequestParam String code) {
        return userService.verifyPhone(code);
    }
}
