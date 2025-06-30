package ma.tayeb.messaging_backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Customer;

@Repository
public interface CustomerRepository extends MongoRepository<Customer, String> {
    Customer findByProspectId(String prospectId);
    Customer findByClientId(String clientId);
}
