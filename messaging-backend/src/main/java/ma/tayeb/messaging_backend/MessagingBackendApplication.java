package ma.tayeb.messaging_backend;

import java.util.UUID;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;

import ma.tayeb.messaging_backend.entities.Agent;
import ma.tayeb.messaging_backend.entities.Customer;
import ma.tayeb.messaging_backend.entities.Conversation;
import ma.tayeb.messaging_backend.repositories.AgentRepository;
import ma.tayeb.messaging_backend.repositories.CustomerRepository;
import ma.tayeb.messaging_backend.repositories.ConversationRepository;

@SpringBootApplication
public class MessagingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(MessagingBackendApplication.class, args);
	}

	@Bean
	CommandLineRunner start(
			AgentRepository agentRepository,
			CustomerRepository customerRepository,
			ConversationRepository conversationRepository) {
		return args -> {
			Agent agent = new Agent();
			agent.setId(UUID.randomUUID());
			agent.setFullName("Télé-banquier 1");
			agent.setEmail("agent@soge.ma");
			agentRepository.save(agent);

			Customer customer = new Customer();
			customer.setId(UUID.randomUUID());
			customer.setFullName("Customer 1");
			customer.setClientId("clientId-123");
			customer.setProspectId("prospectId-456");
			customerRepository.save(customer);

			Conversation conversation = new Conversation();
			conversation.setOwner(customer);
			conversationRepository.save(conversation);
		};
	}

}
