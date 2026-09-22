package com.agroenvios.clientes.primary.repository;

import com.agroenvios.clientes.primary.model.Pedido;
import com.agroenvios.clientes.primary.model.PedidoItem;
import com.agroenvios.clientes.primary.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Compila con Hibernate cada {@code @Query} de PedidoRepository contra el modelo real, sin
 * necesitar base de datos. Es lo mismo que Spring Data hace al arrancar: una consulta inválida
 * (campo mal escrito, sintaxis) tumbaría el despliegue entero, y los demás tests usan mocks del
 * repositorio así que no lo detectarían.
 */
class PedidoRepositoryQueriesTest {

    private static StandardServiceRegistry registro;
    private static SessionFactory fabrica;

    @BeforeAll
    static void abrirHibernateSinBaseDeDatos() {
        registro = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.MySQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting("hibernate.hbm2ddl.auto", "none")
                .build();
        fabrica = new MetadataSources(registro)
                .addAnnotatedClass(Pedido.class)
                .addAnnotatedClass(PedidoItem.class)
                .addAnnotatedClass(User.class)
                .buildMetadata()
                .buildSessionFactory();
    }

    @AfterAll
    static void cerrar() {
        if (fabrica != null) fabrica.close();
        if (registro != null) StandardServiceRegistryBuilder.destroy(registro);
    }

    private static List<Method> consultas() {
        return Arrays.stream(PedidoRepository.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(Query.class))
                .toList();
    }

    @Test
    @DisplayName("todas las @Query de PedidoRepository compilan contra el modelo")
    void todasLasConsultasCompilan() {
        List<Method> consultas = consultas();
        // Si esto llega a 0 el test estaría pasando en vacío
        assertTrue(consultas.size() >= 5, "se esperaban al menos las 5 consultas @Query, hay " + consultas.size());

        try (Session sesion = fabrica.openSession()) {
            for (Method metodo : consultas) {
                String hql = metodo.getAnnotation(Query.class).value();
                try {
                    sesion.createQuery(hql);
                } catch (Exception e) {
                    throw new AssertionError("La consulta de " + metodo.getName() + " no compila: " + e.getMessage(), e);
                }
            }
        }
    }

    @Test
    @DisplayName("la consulta de pendientes de replicar y los tres UPDATE están entre las verificadas")
    void incluyeLasConsultasDelReintento() {
        List<String> nombres = consultas().stream().map(Method::getName).toList();
        assertFalse(nombres.isEmpty());
        assertTrue(nombres.containsAll(List.of(
                "findPendientesDeReplicar", "marcarReplicado", "registrarFalloReplicacion", "descartarReplicacion")),
                "faltan consultas del reintento en la verificación: " + nombres);
    }
}
