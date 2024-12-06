package app.rest.controllers;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import app.entities.Event;
import app.entities.Organization;
import app.entities.Tag;
import app.repositories.EventRepository;
import app.repositories.OrganizationRepository;
import app.repositories.TagRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
@Path("/events")
public class EventController {

    private static final Logger logger = LoggerFactory.getLogger(EventController.class);

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private TagRepository tagRepository;
    
    @POST
    @Path("/add")
    @Consumes(javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED)
    public Response addEvent(
        @FormParam("title") String title,
        @FormParam("description") String description,
        @FormParam("date") String date,  // Expecting date in 'yyyy-MM-dd' format
        @FormParam("time") String time,  // Expecting time in 'HH:mm' format
        @FormParam("location") String location,
        @FormParam("organizationId") Long organizationId,
        @FormParam("tagIds") String tagIds) {  // A comma-separated string of tag IDs (optional)

        try {
            // Validate inputs
            if (title == null || title.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Title is required").build();
            }
            if (date == null || time == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Date and time are required").build();
            }

            // Convert the date and time from String to LocalDate and LocalTime
            LocalDate eventDate = LocalDate.parse(date);  // Converts 'yyyy-MM-dd' to LocalDate
            LocalTime eventTime = LocalTime.parse(time);  // Converts 'HH:mm' to LocalTime

            // Fetch organization by ID
            Organization organization = organizationRepository.findById(organizationId).orElse(null);
            if (organization == null) {
                return Response.status(Response.Status.NOT_FOUND).entity("Organization not found").build();
            }

            // Create and save the event
            Event event = new Event(title, description, eventDate, eventTime, location, organization);

            // Handle optional tags
            if (tagIds != null && !tagIds.trim().isEmpty()) {
                // Split the comma-separated tag IDs string and fetch the tags from the repository
                String[] tagIdArray = tagIds.split(",");
                List<Tag> tags = new ArrayList<>();
                for (String tagIdStr : tagIdArray) {
                    try {
                        Long tagId = Long.parseLong(tagIdStr.trim());
                        // Use the injected tagRepository here
                        Tag tag = tagRepository.findById(tagId).orElse(null);
                        if (tag != null) {
                            tags.add(tag);
                        } else {
                            logger.warn("Tag with ID {} not found", tagId);
                        }
                    } catch (NumberFormatException e) {
                        logger.warn("Invalid tag ID format: {}", tagIdStr);
                    }
                }
                event.setTags(tags);  // Assign tags to the event
            }

            // Save the event with the optional tags
            eventRepository.save(event);

            logger.info("Event saved with ID: {}", event.getId());
            return Response.status(Response.Status.CREATED).entity("Event saved with ID: " + event.getId()).build();

        } catch (Exception e) {
            logger.error("Error saving event", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Error saving event").build();
        }
    }


    @GET
    @Path("/all")
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<Event> getAllEvents() {
        return eventRepository.findAll();
    }

    @GET
    @Path("/{id}")
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response getEventById(@PathParam("id") Long id) {
        Optional<Event> event = eventRepository.findById(id);
        if (event.isPresent()) {
            return Response.ok(event.get()).build();
        } else {
            return Response.status(Response.Status.NOT_FOUND).entity("Event not found").build();
        }
    }



    @DELETE
    @Path("/{id}")
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public Response deleteEvent(@PathParam("id") Long id) {
        Optional<Event> event = eventRepository.findById(id);
        if (event.isPresent()) {
            eventRepository.delete(event.get());
            // Return success message after deletion
            return Response.status(Response.Status.OK)
                           .entity("Event successfully deleted")
                           .build();
        } else {
            // Event not found
            return Response.status(Response.Status.NOT_FOUND)
                           .entity("Event not found")
                           .build();
        }
    }


    @GET
    @Path("/search")
    @Produces(javax.ws.rs.core.MediaType.APPLICATION_JSON)
    public List<Event> searchEvents(
        @QueryParam("title") String title,
        @QueryParam("location") String location,
        @QueryParam("startDate") String startDate,  // Expecting 'yyyy-MM-dd'
        @QueryParam("endDate") String endDate) {    // Expecting 'yyyy-MM-dd'

        // Searching based on user-provided filters
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;

        // Example filtering based on title, location, and date range
        if (title == null) {
            title = ""; // Set default empty string for title
        }
        if (location == null) {
            location = ""; // Set default empty string for location
        }

        if (start != null && end != null) {
            return eventRepository.findByTitleContainingAndLocationContainingAndDateBetween(title, location, start, end);
        } else if (start != null) {
            return eventRepository.findByTitleContainingAndLocationContainingAndDateGreaterThanEqual(title, location, start);
        } else if (end != null) {
            return eventRepository.findByTitleContainingAndLocationContainingAndDateLessThanEqual(title, location, end);
        } else {
            return eventRepository.findByTitleContainingAndLocationContaining(title, location);
        }
    }}

