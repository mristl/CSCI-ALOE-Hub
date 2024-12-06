package app.repositories;

import app.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    List<Event> findByTitleContainingAndLocationContainingAndDateBetween(String title, String location, LocalDate startDate, LocalDate endDate);

    List<Event> findByTitleContainingAndLocationContainingAndDateGreaterThanEqual(String title, String location, LocalDate startDate);

    List<Event> findByTitleContainingAndLocationContainingAndDateLessThanEqual(String title, String location, LocalDate endDate);

    List<Event> findByTitleContainingAndLocationContaining(String title, String location);
}
