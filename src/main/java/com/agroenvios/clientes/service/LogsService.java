package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.LogsModel;
import com.agroenvios.clientes.repository.LogsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LogsService {

    private final LogsRepository logsRepository;

    public void saveLog(String modulo, String metodo, String mensaje, String usuario) {
        logsRepository.save(LogsModel.builder()
                .modulo(modulo)
                .metodo(metodo)
                .mensaje(mensaje)
                .usuario(usuario)
                .build());
    }
}
