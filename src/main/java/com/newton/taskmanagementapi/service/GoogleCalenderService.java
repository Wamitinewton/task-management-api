package com.newton.taskmanagementapi.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import com.newton.taskmanagementapi.exception.GoogleCalendarException;
import com.newton.taskmanagementapi.model.Task;
import com.newton.taskmanagementapi.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.ZoneId;
import java.util.Date;

@Service
@Slf4j
public class GoogleCalenderService {

    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Value("${google.calendar.application-name}")
    private String applicationName;

    public String createCalendarEvent(Task task, User user) {
        try {
            Calendar service = getCalendarService(user);
            Event event = createEventFromTask(task);

            event = service.events().insert("primary", event).execute();
            log.info("Created calendar event: {}", event.getId());

            return event.getId();
        } catch (Exception e) {
            log.error("Failed to create calendar event for task: {}", task.getId(), e);
            throw new GoogleCalendarException("Failed to create calendar event", e);
        }
    }

    public void updateCalendarEvent(Task task, User user) {
        try {
            if (task.getGoogleEventId() == null) {
                log.warn("Task {} has no Google event ID", task.getId());
                return;
            }

            Calendar service = getCalendarService(user);
            Event event = createEventFromTask(task);

            service.events().update("primary", task.getGoogleEventId(), event).execute();
            log.info("Updated calendar event: {}", task.getGoogleEventId());
        } catch (Exception e) {
            log.error("Failed to update calendar event for task: {}", task.getId(), e);
            throw new GoogleCalendarException("Failed to update calendar event", e);
        }
    }

    public void deleteCalendarEvent(String eventId, User user) {
        try {
            if (eventId == null) {
                log.warn("No event ID provided for deletion");
                return;
            }

            Calendar service = getCalendarService(user);
            service.events().delete("primary", eventId).execute();
            log.info("Deleted calendar event: {}", eventId);
        } catch (Exception e) {
            log.error("Failed to delete calendar event: {}", eventId, e);
            throw new GoogleCalendarException("Failed to delete calendar event", e);
        }
    }

    public Event getCalendarEvent(String eventId, User user) {
        try {
            Calendar service = getCalendarService(user);
            return service.events().get("primary", eventId).execute();
        } catch (Exception e) {
            log.error("Failed to get calendar event: {}", eventId, e);
            throw new GoogleCalendarException("Failed to get calendar event", e);
        }
    }


    private Calendar getCalendarService(User user) throws GeneralSecurityException, IOException {
        if (user.getGoogleAccessToken() == null) {
            throw new GoogleCalendarException("Google Access Token is null");
        }

        NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();

        GoogleCredentials credentials = GoogleCredentials.create(
                new AccessToken(user.getGoogleAccessToken(), null)
        );

        HttpRequestInitializer requestInitializer = new HttpCredentialsAdapter(credentials);

        return new Calendar.Builder(httpTransport, JSON_FACTORY, requestInitializer)
                .setApplicationName(applicationName)
                .build();
    }

    private Event createEventFromTask(Task task) {
        Event event = new Event()
                .setSummary(task.getTitle())
                .setDescription(task.getDescription());

        if (task.getDueDate() != null) {
            DateTime startDateTime = new DateTime(
                    Date.from(task.getDueDate().atZone(ZoneId.systemDefault()).toInstant())
            );
            EventDateTime start = new EventDateTime()
                    .setDateTime(startDateTime)
                    .setTimeZone(ZoneId.systemDefault().getId());
            event.setStart(start);

            // Set end time to 1 hour after start
            DateTime endDateTime = new DateTime(
                    Date.from(task.getDueDate().plusHours(1).atZone(ZoneId.systemDefault()).toInstant())
            );
            EventDateTime end = new EventDateTime()
                    .setDateTime(endDateTime)
                    .setTimeZone(ZoneId.systemDefault().getId());
            event.setEnd(end);
        }

        // Add task completion status to description
        String statusText = task.getCompleted() ? "Status: Completed" : "Status: Pending";
        String description = task.getDescription() != null ?
                task.getDescription() + "\n\n" + statusText : statusText;
        event.setDescription(description);

        return event;
    }
}
