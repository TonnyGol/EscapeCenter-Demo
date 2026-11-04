package Responses;

import Entities.Client;

public record ClientResponse(String firstName, String lastName, String phoneNumber, String email) {

    public static ClientResponse from(Client client) {
        return new ClientResponse(client.getFirstName(), client.getLastName(), client.getPhoneNumber(), client.getEmail());
    }
}
