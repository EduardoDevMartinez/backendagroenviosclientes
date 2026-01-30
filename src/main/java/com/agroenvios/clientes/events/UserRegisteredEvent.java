package com.agroenvios.clientes.events;

import com.agroenvios.clientes.model.User;
import org.springframework.context.ApplicationEvent;

public class UserRegisteredEvent extends ApplicationEvent {

    private final User user;

    public UserRegisteredEvent(User user) {
        super(user);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}
