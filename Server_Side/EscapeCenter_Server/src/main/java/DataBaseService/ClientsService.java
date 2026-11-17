package DataBaseService;

import Entities.Client;
import Repositories.ClientRepository;
import Requests.ClientDetails;
import Responses.ClientResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ClientsService {
    private static final String CLIENT_NOT_FOUND_MESSAGE = "Client not found.";
    private static final String PHONE_TAKEN_MESSAGE = "client.phoneNumber: another client already has this phone number.";

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

    /** The phone number identifies a client, so it can change only to a number no other client has. */
    @Transactional
    public void updateClient(Long id, ClientDetails details) {
        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, CLIENT_NOT_FOUND_MESSAGE));
        String phoneNumber = BookingService.normalizedPhone(details.phoneNumber());
        if (clientRepository.existsByPhoneNumberAndIdNot(phoneNumber, id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, PHONE_TAKEN_MESSAGE);
        }
        client.setPhoneNumber(phoneNumber);
        client.setFirstName(details.firstName());
        client.setLastName(details.lastName());
        client.setEmail(details.email());
    }
}
