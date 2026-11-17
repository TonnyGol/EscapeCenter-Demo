package Responses;

import Entities.Client;

public record ClientResponse(Long id, String firstName, String lastName, String phoneNumber, String email) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getId(), client.getFirstName(), client.getLastName(),
                client.getPhoneNumber(), client.getEmail());
    }
}
