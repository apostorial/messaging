package ma.tayeb.messaging_backend.repositories;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ma.tayeb.messaging_backend.entities.Agent;

@Repository
public interface AgentRepository extends JpaRepository<Agent, UUID> {
} 