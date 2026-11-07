package com.ticketflow.customerservice.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ticketflow.customerservice.dto.CustomerRequest;
import com.ticketflow.customerservice.entities.Customer;
import com.ticketflow.customerservice.entities.CustomerStatus;
import com.ticketflow.customerservice.exceptions.DuplicateEmailException;
import com.ticketflow.customerservice.exceptions.ResourceNotFoundException;
import com.ticketflow.customerservice.repositories.CustomerRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:test;MODE=PostgreSQL",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class CustomerServiceTest {

    @Autowired
    private CustomerService service;

    @Autowired
    private CustomerRepo repo;

    @Test
    void shouldCreateCustomerAndNormalizeEmailToLowerCase() {
        CustomerRequest req = new CustomerRequest("Alice", "Dupont", "Alice@Example.COM", "+33612345678");
        Customer created = service.create(req, "kc-123");
        assertThat(created.getId()).isNotNull();
        assertThat(created.getEmail()).isEqualTo("alice@example.com");
        assertThat(created.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
        assertThat(created.getKeycloakId()).isEqualTo("kc-123");
    }

    @Test
    void shouldRejectDuplicateEmail() {
        service.create(new CustomerRequest("Bob", "Martin", "bob@test.io", null), "kc-1");
        assertThatThrownBy(() ->
                service.create(new CustomerRequest("Bob2", "Martin", "BOB@TEST.IO", null), "kc-2"))
                .isInstanceOf(DuplicateEmailException.class);
    }

    @Test
    void shouldThrowWhenCustomerNotFound() {
        assertThatThrownBy(() -> service.get(99999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Customer 99999 not found");
    }

    @Test
    void shouldSuspendAndReactivateCustomer() {
        Customer c = service.create(new CustomerRequest("Carol", "Leroy", "carol@test.io", null), "kc-3");
        service.suspend(c.getId());
        assertThat(service.get(c.getId()).getStatus()).isEqualTo(CustomerStatus.SUSPENDED);
        service.reactivate(c.getId());
        assertThat(service.get(c.getId()).getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void shouldCloseCustomer() {
        Customer c = service.create(new CustomerRequest("Dave", "Brown", "dave@test.io", null), "kc-4");
        service.close(c.getId());
        assertThat(service.get(c.getId()).getStatus()).isEqualTo(CustomerStatus.CLOSED);
    }

    @Test
    void shouldUpdateCustomerWithoutEmailConflict() {
        Customer c = service.create(new CustomerRequest("Eve", "Stone", "eve@test.io", null), "kc-5");
        Customer updated = service.update(c.getId(),
                new CustomerRequest("Eve2", "Stone2", "eve@test.io", "+33699999999"));
        assertThat(updated.getFirstName()).isEqualTo("Eve2");
        assertThat(updated.getPhone()).isEqualTo("+33699999999");
    }
}