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
			agent.setId(UUID.fromString("2cc81633-cf25-4948-90da-3e2df420c6ac"));
			agent.setFullName("Télé-banquier 1");
			agent.setEmail("agent1@soge.ma");
			agentRepository.save(agent);
			
			Agent agent2 = new Agent();
			agent2.setId(UUID.fromString("efe6d435-c699-45e5-897c-8bc395056084"));
			agent2.setFullName("Télé-banquier 2");
			agent2.setEmail("agent2@soge.ma");
			agentRepository.save(agent2);


			Customer customer1 = new Customer();
			customer1.setId(UUID.fromString("d2707eb4-b1c6-4885-97fc-08091238699e"));
			customer1.setFullName("Customer 1");
			customer1.setClientId("clientId-123");
			customer1.setProspectId("prospectId-456");
			customerRepository.save(customer1);

			Customer customer2 = new Customer();
			customer2.setId(UUID.fromString("49de2257-4301-49c4-9e4a-425648558076"));
			customer2.setFullName("Customer 2");
			customer2.setClientId("clientId-456");
			customer2.setProspectId("prospectId-123");
			customerRepository.save(customer2);


			Conversation conversation = new Conversation();
			conversation.setId(UUID.fromString("64d498e1-c605-44a0-9570-f9629c9a503e"));
			conversation.setOwner(customer1);
			conversationRepository.save(conversation);

			Conversation conversation2 = new Conversation();
			conversation2.setId(UUID.fromString("14004046-a4e7-4ffd-a26b-d9882a05b96a"));
			conversation2.setOwner(customer2);
			conversationRepository.save(conversation2);
		};
	}

}
