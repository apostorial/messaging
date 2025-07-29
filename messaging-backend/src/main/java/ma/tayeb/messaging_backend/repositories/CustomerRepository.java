package ma.tayeb.messaging_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Customer;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Customer findByProspectId(String prospectId);
    Customer findByClientId(String clientId);
}
