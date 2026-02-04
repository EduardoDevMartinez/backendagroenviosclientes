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

    public List<DireccionEntrega> getDirecciones(Long userId) {
        return direccionRepository.findByUserId(userId);
    }

    public DireccionEntrega getDireccionById(Long direccionId, Long userId) {
        return direccionRepository.findById(direccionId)
                .filter(d -> d.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Dirección no encontrada"));
    }

    @Transactional
    public DireccionEntrega crear(DireccionEntrega direccion, User user) {
        direccion.setUser(user);
        if (Boolean.TRUE.equals(direccion.getEsPrincipal())) {
            List<DireccionEntrega> existentes = direccionRepository.findByUserId(user.getId());
            existentes.forEach(d -> d.setEsPrincipal(false));
            direccionRepository.saveAll(existentes);
        }
        return direccionRepository.save(direccion);
    }

    @Transactional
    public DireccionEntrega actualizar(Long direccionId, DireccionEntrega datos, Long userId) {
        DireccionEntrega direccion = getDireccionById(direccionId, userId);

        if (datos.getNombre() != null) direccion.setNombre(datos.getNombre());
        if (datos.getCalle() != null) direccion.setCalle(datos.getCalle());
        if (datos.getCiudad() != null) direccion.setCiudad(datos.getCiudad());
        if (datos.getEstado() != null) direccion.setEstado(datos.getEstado());
        if (datos.getCodigoPostal() != null) direccion.setCodigoPostal(datos.getCodigoPostal());
        if (datos.getColonia() != null) direccion.setColonia(datos.getColonia());

        if (datos.getEsPrincipal() != null && datos.getEsPrincipal()) {
            List<DireccionEntrega> existentes = direccionRepository.findByUserId(userId);
            existentes.forEach(d -> d.setEsPrincipal(false));
            direccionRepository.saveAll(existentes);
            direccion.setEsPrincipal(true);
        }

        return direccionRepository.save(direccion);
    }

    public void eliminar(Long direccionId, Long userId) {
        DireccionEntrega direccion = getDireccionById(direccionId, userId);
        direccionRepository.delete(direccion);
    }
}
