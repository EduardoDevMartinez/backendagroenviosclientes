package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.DireccionEntrega;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.DireccionEntregaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DireccionEntregaService {

    private final DireccionEntregaRepository direccionRepository;
    private final LogsService logsService;

    public List<DireccionEntrega> getDirecciones(User user) {
        logsService.saveLog("direcciones", "get_all", "Direcciones consultadas", user.getUsername());
        return direccionRepository.findByUserId(user.getId());
    }

    public DireccionEntrega getDireccionById(Long direccionId, User user) {
        DireccionEntrega direccion = direccionRepository.findById(direccionId)
                .filter(d -> d.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
        logsService.saveLog("direcciones", "get_one", "Dirección consultada - ID: " + direccionId, user.getUsername());
        return direccion;
    }

    @Transactional
    public DireccionEntrega crear(DireccionEntrega direccion, User user) {
        direccion.setUser(user);
        if (Boolean.TRUE.equals(direccion.getEsPrincipal())) {
            List<DireccionEntrega> existentes = direccionRepository.findByUserId(user.getId());
            existentes.forEach(d -> d.setEsPrincipal(false));
            direccionRepository.saveAll(existentes);
        }
        DireccionEntrega creada = direccionRepository.save(direccion);
        logsService.saveLog("direcciones", "crear", "Dirección creada - ID: " + creada.getId(), user.getUsername());
        return creada;
    }

    @Transactional
    public DireccionEntrega actualizar(Long direccionId, DireccionEntrega datos, User user) {
        DireccionEntrega direccion = getDireccionById(direccionId, user);

        if (datos.getNombre() != null) direccion.setNombre(datos.getNombre());
        if (datos.getCalle() != null) direccion.setCalle(datos.getCalle());
        if (datos.getCiudad() != null) direccion.setCiudad(datos.getCiudad());
        if (datos.getEstado() != null) direccion.setEstado(datos.getEstado());
        if (datos.getCodigoPostal() != null) direccion.setCodigoPostal(datos.getCodigoPostal());
        if (datos.getColonia() != null) direccion.setColonia(datos.getColonia());

        if (datos.getEsPrincipal() != null && datos.getEsPrincipal()) {
            List<DireccionEntrega> existentes = direccionRepository.findByUserId(user.getId());
            existentes.forEach(d -> d.setEsPrincipal(false));
            direccionRepository.saveAll(existentes);
            direccion.setEsPrincipal(true);
        }

        logsService.saveLog("direcciones", "actualizar", "Dirección actualizada - ID: " + direccionId, user.getUsername());
        return direccionRepository.save(direccion);
    }

    public void eliminar(Long direccionId, User user) {
        DireccionEntrega direccion = getDireccionById(direccionId, user);
        direccionRepository.delete(direccion);
        logsService.saveLog("direcciones", "eliminar", "Dirección eliminada - ID: " + direccionId, user.getUsername());
    }
}
