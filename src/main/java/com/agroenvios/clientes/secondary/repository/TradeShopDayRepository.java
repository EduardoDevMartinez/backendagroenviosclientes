package com.agroenvios.clientes.secondary.repository;

import com.agroenvios.clientes.secondary.model.TradeShopDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TradeShopDayRepository extends JpaRepository<TradeShopDay, Integer> {

    /** ¿El horario tiene marcado ese día (LUNES...) como de descanso? */
    boolean existsByScheduleIdAndDayAndIsOpenFalse(Integer scheduleId, String day);
}
