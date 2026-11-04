package DataBaseService;

import Entities.Client;
import Repositories.ClientRepository;
import Responses.ClientResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClientsService {
    private final ClientRepository clientRepository;

    public ClientsService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }

    /** @param query part of a first name, last name or phone number; blank returns every client */
    @Transactional(readOnly = true)
    public List<ClientResponse> searchClients(String query) {
        List<Client> found = query == null || query.isBlank()
                ? clientRepository.findAll()
                : clientRepository.search(query.trim());
        return found.stream().map(ClientResponse::from).toList();
    }
}
