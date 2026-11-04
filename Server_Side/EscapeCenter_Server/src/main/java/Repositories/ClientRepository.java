package Repositories;

import Entities.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    Optional<Client> findByPhoneNumber(String phoneNumber);

    @Query("select c from Client c where lower(c.firstName) like lower(concat('%', :query, '%')) "
            + "or lower(c.lastName) like lower(concat('%', :query, '%')) "
            + "or c.phoneNumber like concat('%', :query, '%')")
    List<Client> search(@Param("query") String query);
}
