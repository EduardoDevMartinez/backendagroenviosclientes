package com.agroenvios.clientes.service;

import com.agroenvios.clientes.model.Customer;
import com.agroenvios.clientes.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomerService {
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;

    public Customer createCustomer(String email, String password, String nombre) {
        Customer customer = Customer.builder()
                .email(email)
                .password(passwordEncoder.encode(password))
                .nombre(nombre)
                .emailVerified(false)
                .build();
        return customerRepository.save(customer);
    }

    public Optional<Customer> findByEmail(String email) {
        return customerRepository.findByEmail(email);
    }

    public Customer getCustomerById(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
    }

    public Customer updateCustomer(Long id, Customer customerData) {
        Customer customer = getCustomerById(id);
        if (customerData.getNombre() != null) customer.setNombre(customerData.getNombre());
        if (customerData.getPaterno() != null) customer.setPaterno(customerData.getPaterno());
        if (customerData.getMaterno() != null) customer.setMaterno(customerData.getMaterno());
        if (customerData.getTelefono() != null) customer.setTelefono(customerData.getTelefono());
        if (customerData.getDireccion() != null) customer.setDireccion(customerData.getDireccion());
        if (customerData.getCiudad() != null) customer.setCiudad(customerData.getCiudad());
        return customerRepository.save(customer);
    }

    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }
}
