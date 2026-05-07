package com.agroenvios.clientes.primary.controller;

import com.agroenvios.clientes.primary.dto.envio.CotizacionEnvio;
import com.agroenvios.clientes.primary.service.EnvioService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/envio")
@RequiredArgsConstructor
public class EnvioController {

    private final EnvioService envioService;

    @GetMapping("/cotizar")
    public CotizacionEnvio cotizar(@RequestParam Long direccionId) {
        return envioService.cotizar(direccionId);
    }
}
