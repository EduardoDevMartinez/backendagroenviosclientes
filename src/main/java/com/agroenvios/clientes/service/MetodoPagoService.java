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
    private final LogsService logsService;

    public List<MetodoPago> getMetodosPago(User user) {
        logsService.saveLog("metodos_pago", "get_all", "Métodos de pago consultados", user.getUsername());
        return metodoPagoRepository.findByUserId(user.getId());
    }

    public MetodoPago getMetodoPagoById(Long metodoPagoId, User user) {
        MetodoPago metodoPago = metodoPagoRepository.findById(metodoPagoId)
                .filter(m -> m.getUser().getId().equals(user.getId()))
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));
        logsService.saveLog("metodos_pago", "get_one", "Método de pago consultado - ID: " + metodoPagoId, user.getUsername());
        return metodoPago;
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
        MetodoPago creado = metodoPagoRepository.save(metodoPago);
        logsService.saveLog("metodos_pago", "crear", "Método de pago creado - ID: " + creado.getId(), user.getUsername());
        return creado;
    }

    @Transactional
    public MetodoPago actualizar(Long metodoPagoId, MetodoPago datos, User user) {
        MetodoPago metodoPago = getMetodoPagoById(metodoPagoId, user);

        if (datos.getTipo() != null) metodoPago.setTipo(datos.getTipo());
        if (datos.getTitular() != null) metodoPago.setTitular(datos.getTitular());
        if (datos.getFechaExpiracion() != null) metodoPago.setFechaExpiracion(datos.getFechaExpiracion());

        if (datos.getEsPrincipal() != null && datos.getEsPrincipal()) {
            List<MetodoPago> existentes = metodoPagoRepository.findByUserId(user.getId());
            existentes.forEach(m -> m.setEsPrincipal(false));
            metodoPagoRepository.saveAll(existentes);
            metodoPago.setEsPrincipal(true);
        }

        logsService.saveLog("metodos_pago", "actualizar", "Método de pago actualizado - ID: " + metodoPagoId, user.getUsername());
        return metodoPagoRepository.save(metodoPago);
    }

    public void eliminar(Long metodoPagoId, User user) {
        MetodoPago metodoPago = getMetodoPagoById(metodoPagoId, user);
        metodoPagoRepository.delete(metodoPago);
        logsService.saveLog("metodos_pago", "eliminar", "Método de pago eliminado - ID: " + metodoPagoId, user.getUsername());
    }
}
