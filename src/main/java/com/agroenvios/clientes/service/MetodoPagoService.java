package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.MetodoPago;
import com.agroenvios.clientes.model.User;
import com.agroenvios.clientes.repository.MetodoPagoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetodoPagoService {

    private final MetodoPagoRepository metodoPagoRepository;

    public List<MetodoPago> getMetodosPago(Long userId) {
        return metodoPagoRepository.findByUserId(userId);
    }

    public MetodoPago getMetodoPagoById(Long metodoPagoId, Long userId) {
        return metodoPagoRepository.findById(metodoPagoId)
                .filter(m -> m.getUser().getId().equals(userId))
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));
    }

    @Transactional
    public MetodoPago crear(MetodoPago metodoPago, User user) {
        metodoPago.setUser(user);
        // Solo guardar los últimos 4 dígitos por seguridad
        if (metodoPago.getUltimosDigitos() != null && metodoPago.getUltimosDigitos().length() > 4) {
            String digitos = metodoPago.getUltimosDigitos();
            metodoPago.setUltimosDigitos(digitos.substring(digitos.length() - 4));
        }

        if (Boolean.TRUE.equals(metodoPago.getEsPrincipal())) {
            List<MetodoPago> existentes = metodoPagoRepository.findByUserId(user.getId());
            existentes.forEach(m -> m.setEsPrincipal(false));
            metodoPagoRepository.saveAll(existentes);
        }
        return metodoPagoRepository.save(metodoPago);
    }

    @Transactional
    public MetodoPago actualizar(Long metodoPagoId, MetodoPago datos, Long userId) {
        MetodoPago metodoPago = getMetodoPagoById(metodoPagoId, userId);

        if (datos.getTipo() != null) metodoPago.setTipo(datos.getTipo());
        if (datos.getTitular() != null) metodoPago.setTitular(datos.getTitular());
        if (datos.getFechaExpiracion() != null) metodoPago.setFechaExpiracion(datos.getFechaExpiracion());

        if (datos.getEsPrincipal() != null && datos.getEsPrincipal()) {
            List<MetodoPago> existentes = metodoPagoRepository.findByUserId(userId);
            existentes.forEach(m -> m.setEsPrincipal(false));
            metodoPagoRepository.saveAll(existentes);
            metodoPago.setEsPrincipal(true);
        }

        return metodoPagoRepository.save(metodoPago);
    }

    public void eliminar(Long metodoPagoId, Long userId) {
        MetodoPago metodoPago = getMetodoPagoById(metodoPagoId, userId);
        metodoPagoRepository.delete(metodoPago);
    }
}
