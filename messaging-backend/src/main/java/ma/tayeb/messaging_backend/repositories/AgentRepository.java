package ma.tayeb.messaging_backend.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Agent;

@Repository
public interface AgentRepository extends MongoRepository<Agent, String> {
    Agent findByEmail(String email);
}
