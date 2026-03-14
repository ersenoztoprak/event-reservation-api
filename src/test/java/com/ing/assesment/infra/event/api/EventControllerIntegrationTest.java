package com.ing.assesment.infra.event.api;

import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.infra.auth.api.request.LoginRequest;
import com.ing.assesment.infra.auth.persistence.entity.UserEntity;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import com.ing.assesment.infra.common.AbstractIntegrationTest;
import com.ing.assesment.infra.event.api.request.CreateEventRequest;
import com.ing.assesment.infra.event.api.request.PublishEventRequest;
import com.ing.assesment.infra.event.api.request.UpdateEventRequest;
import com.ing.assesment.infra.event.persistence.entity.EventEntity;
import com.ing.assesment.infra.event.persistence.repository.EventJpaRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EventJpaRepository eventJpaRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanup() {
        eventJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        rateLimitingFilter.clear();
    }

    @Test
    void shouldCreateDraftEventForOrganizer() throws Exception {
        UserEntity organizer = new UserEntity();
        organizer.setEmail("organizer@test.com");
        organizer.setPasswordHash(encoder.encode("Password123"));
        organizer.setRoles(Set.of(UserRole.ORGANIZER));
        organizer.setCreatedAt(Instant.now());
        userJpaRepository.save(organizer);

        String accessToken = loginAndGetAccessToken("organizer@test.com");

        CreateEventRequest request = new CreateEventRequest(
                "Spring Boot Summit",
                "Istanbul",
                Instant.parse("2026-04-15T09:00:00Z"),
                Instant.parse("2026-04-15T18:00:00Z"),
                300
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.title").value("Spring Boot Summit"))
                .andExpect(jsonPath("$.venue").value("Istanbul"))
                .andExpect(jsonPath("$.capacity").value(300))
                .andExpect(jsonPath("$.published").value(false));

        EventEntity saved = eventJpaRepository.findAll().stream().findFirst().orElseThrow();
        assertEquals("Spring Boot Summit", saved.getTitle());
        assertEquals("Istanbul", saved.getVenue());
        assertEquals(300, saved.getCapacity());
        assertFalse(saved.isPublished());
        assertNotNull(saved.getOwnerId());
    }

    @Test
    void shouldCreateDraftEventForAdmin() throws Exception {
        UserEntity admin = new UserEntity();
        admin.setEmail("admin@test.com");
        admin.setPasswordHash(encoder.encode("Password123"));
        admin.setRoles(Set.of(UserRole.ADMIN));
        admin.setCreatedAt(Instant.now());
        userJpaRepository.save(admin);

        String accessToken = loginAndGetAccessToken("admin@test.com");

        CreateEventRequest request = new CreateEventRequest(
                "Admin Managed Event",
                "Ankara",
                Instant.parse("2026-05-20T10:00:00Z"),
                Instant.parse("2026-05-20T16:00:00Z"),
                120
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void shouldReturnForbiddenForCustomer() throws Exception {
        UserEntity customer = new UserEntity();
        customer.setEmail("customer@test.com");
        customer.setPasswordHash(encoder.encode("Password123"));
        customer.setRoles(Set.of(UserRole.CUSTOMER));
        customer.setCreatedAt(Instant.now());
        userJpaRepository.save(customer);

        String accessToken = loginAndGetAccessToken("customer@test.com");

        CreateEventRequest request = new CreateEventRequest(
                "Forbidden Event",
                "Izmir",
                Instant.parse("2026-06-10T09:00:00Z"),
                Instant.parse("2026-06-10T17:00:00Z"),
                80
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenTokenIsMissing() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
                "Unauthorized Event",
                "Bursa",
                Instant.parse("2026-06-15T09:00:00Z"),
                Instant.parse("2026-06-15T17:00:00Z"),
                60
        );

        mockMvc.perform(post("/api/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnBadRequestWhenDatesAreInvalid() throws Exception {
        UserEntity organizer = new UserEntity();
        organizer.setEmail("organizer2@test.com");
        organizer.setPasswordHash(encoder.encode("Password123"));
        organizer.setRoles(Set.of(UserRole.ORGANIZER));
        organizer.setCreatedAt(Instant.now());
        userJpaRepository.save(organizer);

        String accessToken = loginAndGetAccessToken("organizer2@test.com");

        CreateEventRequest request = new CreateEventRequest(
                "Invalid Event",
                "Istanbul",
                Instant.parse("2026-07-10T18:00:00Z"),
                Instant.parse("2026-07-10T10:00:00Z"),
                90
        );

        mockMvc.perform(post("/api/events")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    private String loginAndGetAccessToken(String email) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, "Password123");

        String loginResponse = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(loginRequest)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return JsonPath.read(loginResponse, "$.accessToken");
    }

    @Test
    void shouldUpdateEventWhenCallerIsOwner() throws Exception {
        UserEntity organizer = new UserEntity();
        organizer.setEmail("owner-update@test.com");
        organizer.setPasswordHash(encoder.encode("Password123"));
        organizer.setRoles(Set.of(UserRole.ORGANIZER));
        organizer.setCreatedAt(Instant.now());
        organizer = userJpaRepository.save(organizer);

        EventEntity event = new EventEntity();
        event.setOwnerId(organizer.getId());
        event.setTitle("Original Title");
        event.setVenue("Original Venue");
        event.setStartsAt(Instant.parse("2026-09-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2026-09-10T17:00:00Z"));
        event.setCapacity(100);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("owner-update@test.com");

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Title",
                "Updated Venue",
                Instant.parse("2026-09-10T10:00:00Z"),
                Instant.parse("2026-09-10T18:00:00Z"),
                150,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.venue").value("Updated Venue"))
                .andExpect(jsonPath("$.capacity").value(150));

        EventEntity updated = eventJpaRepository.findById(event.getId()).orElseThrow();
        assertEquals("Updated Title", updated.getTitle());
        assertEquals("Updated Venue", updated.getVenue());
        assertEquals(150, updated.getCapacity());
    }

    @Test
    void shouldUpdateEventWhenCallerIsAdmin() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("real-owner@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        UserEntity admin = new UserEntity();
        admin.setEmail("admin-update@test.com");
        admin.setPasswordHash(encoder.encode("Password123"));
        admin.setRoles(Set.of(UserRole.ADMIN));
        admin.setCreatedAt(Instant.now());
        userJpaRepository.save(admin);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Original Title");
        event.setVenue("Original Venue");
        event.setStartsAt(Instant.parse("2026-10-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2026-10-10T17:00:00Z"));
        event.setCapacity(200);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("admin-update@test.com");

        UpdateEventRequest request = new UpdateEventRequest(
                "Admin Updated Event",
                "Ankara",
                Instant.parse("2026-10-10T10:00:00Z"),
                Instant.parse("2026-10-10T19:00:00Z"),
                220,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Admin Updated Event"))
                .andExpect(jsonPath("$.venue").value("Ankara"))
                .andExpect(jsonPath("$.capacity").value(220));
    }

    @Test
    void shouldReturnForbiddenWhenCallerIsNotOwnerOrAdmin() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("event-owner@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        UserEntity anotherOrganizer = new UserEntity();
        anotherOrganizer.setEmail("another-organizer@test.com");
        anotherOrganizer.setPasswordHash(encoder.encode("Password123"));
        anotherOrganizer.setRoles(Set.of(UserRole.ORGANIZER));
        anotherOrganizer.setCreatedAt(Instant.now());
        userJpaRepository.save(anotherOrganizer);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Original Title");
        event.setVenue("Original Venue");
        event.setStartsAt(Instant.parse("2026-11-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2026-11-10T17:00:00Z"));
        event.setCapacity(150);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("another-organizer@test.com");

        UpdateEventRequest request = new UpdateEventRequest(
                "Hacked Title",
                "Hacked Venue",
                Instant.parse("2026-11-10T10:00:00Z"),
                Instant.parse("2026-11-10T18:00:00Z"),
                180,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenUpdatingWithoutToken() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("owner-no-token@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Original Title");
        event.setVenue("Original Venue");
        event.setStartsAt(Instant.parse("2026-12-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2026-12-10T17:00:00Z"));
        event.setCapacity(100);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Without Token",
                "Bursa",
                Instant.parse("2026-12-10T10:00:00Z"),
                Instant.parse("2026-12-10T18:00:00Z"),
                110,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenEventDoesNotExist() throws Exception {
        UserEntity admin = new UserEntity();
        admin.setEmail("admin-notfound@test.com");
        admin.setPasswordHash(encoder.encode("Password123"));
        admin.setRoles(Set.of(UserRole.ADMIN));
        admin.setCreatedAt(Instant.now());
        userJpaRepository.save(admin);

        String accessToken = loginAndGetAccessToken("admin-notfound@test.com");

        UpdateEventRequest request = new UpdateEventRequest(
                "Updated Missing Event",
                "Istanbul",
                Instant.parse("2026-12-20T10:00:00Z"),
                Instant.parse("2026-12-20T18:00:00Z"),
                90,
                0L
        );

        mockMvc.perform(put("/api/events/{id}", 999999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenUpdatePayloadIsInvalid() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("owner-invalid@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Original Title");
        event.setVenue("Original Venue");
        event.setStartsAt(Instant.parse("2026-12-25T09:00:00Z"));
        event.setEndsAt(Instant.parse("2026-12-25T17:00:00Z"));
        event.setCapacity(100);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("owner-invalid@test.com");

        UpdateEventRequest request = new UpdateEventRequest(
                "Broken Update",
                "Broken Venue",
                Instant.parse("2026-12-25T18:00:00Z"),
                Instant.parse("2026-12-25T10:00:00Z"),
                100,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturnConflictWhenVersionIsStale() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("owner-conflict@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Original Title");
        event.setVenue("Original Venue");
        event.setStartsAt(Instant.parse("2027-01-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-01-10T17:00:00Z"));
        event.setCapacity(100);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("owner-conflict@test.com");

        UpdateEventRequest firstUpdate = new UpdateEventRequest(
                "First Update",
                "Venue One",
                Instant.parse("2027-01-10T10:00:00Z"),
                Instant.parse("2027-01-10T18:00:00Z"),
                120,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstUpdate)))
                .andExpect(status().isOk());

        UpdateEventRequest staleUpdate = new UpdateEventRequest(
                "Stale Update",
                "Venue Two",
                Instant.parse("2027-01-10T11:00:00Z"),
                Instant.parse("2027-01-10T19:00:00Z"),
                130,
                event.getVersion()
        );

        mockMvc.perform(put("/api/events/{id}", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(staleUpdate)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldPublishEventWhenCallerIsOwner() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("publish-owner@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Draft Event");
        event.setVenue("Istanbul");
        event.setStartsAt(Instant.parse("2027-06-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-06-10T17:00:00Z"));
        event.setCapacity(100);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("publish-owner@test.com");

        PublishEventRequest request = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(event.getId()))
                .andExpect(jsonPath("$.published").value(true));

        EventEntity published = eventJpaRepository.findById(event.getId()).orElseThrow();
        assertTrue(published.isPublished());
    }

    @Test
    void shouldPublishEventWhenCallerIsAdmin() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("real-owner-publish@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        UserEntity admin = new UserEntity();
        admin.setEmail("admin-publish@test.com");
        admin.setPasswordHash(encoder.encode("Password123"));
        admin.setRoles(Set.of(UserRole.ADMIN));
        admin.setCreatedAt(Instant.now());
        admin = userJpaRepository.save(admin);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Admin Publish Draft");
        event.setVenue("Ankara");
        event.setStartsAt(Instant.parse("2027-07-01T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-07-01T17:00:00Z"));
        event.setCapacity(120);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("admin-publish@test.com");

        PublishEventRequest request = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.published").value(true));
    }

    @Test
    void shouldReturnForbiddenWhenCallerIsNotOwnerOrAdminForPublish() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("publish-owner2@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        UserEntity anotherOrganizer = new UserEntity();
        anotherOrganizer.setEmail("another-organizer-publish@test.com");
        anotherOrganizer.setPasswordHash(encoder.encode("Password123"));
        anotherOrganizer.setRoles(Set.of(UserRole.ORGANIZER));
        anotherOrganizer.setCreatedAt(Instant.now());
        anotherOrganizer = userJpaRepository.save(anotherOrganizer);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Forbidden Publish Event");
        event.setVenue("Izmir");
        event.setStartsAt(Instant.parse("2027-08-01T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-08-01T17:00:00Z"));
        event.setCapacity(80);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("another-organizer-publish@test.com");

        PublishEventRequest request = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenPublishingWithoutToken() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("publish-no-token-owner@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("No Token Publish Event");
        event.setVenue("Bursa");
        event.setStartsAt(Instant.parse("2027-09-01T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-09-01T17:00:00Z"));
        event.setCapacity(70);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        PublishEventRequest request = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenPublishingMissingEvent() throws Exception {
        UserEntity admin = new UserEntity();
        admin.setEmail("admin-publish-notfound@test.com");
        admin.setPasswordHash(encoder.encode("Password123"));
        admin.setRoles(Set.of(UserRole.ADMIN));
        admin.setCreatedAt(Instant.now());
        userJpaRepository.save(admin);

        String accessToken = loginAndGetAccessToken("admin-publish-notfound@test.com");

        PublishEventRequest request = new PublishEventRequest(0L);

        mockMvc.perform(post("/api/events/{id}/publish", 999999L)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenEventIsAlreadyPublished() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("already-published-owner@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Already Published Event");
        event.setVenue("Istanbul");
        event.setStartsAt(Instant.parse("2027-10-01T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-10-01T17:00:00Z"));
        event.setCapacity(200);
        event.setPublished(true);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("already-published-owner@test.com");

        PublishEventRequest request = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnConflictWhenPublishVersionIsStale() throws Exception {
        UserEntity owner = new UserEntity();
        owner.setEmail("stale-publish-owner@test.com");
        owner.setPasswordHash(encoder.encode("Password123"));
        owner.setRoles(Set.of(UserRole.ORGANIZER));
        owner.setCreatedAt(Instant.now());
        owner = userJpaRepository.save(owner);

        EventEntity event = new EventEntity();
        event.setOwnerId(owner.getId());
        event.setTitle("Stale Publish Event");
        event.setVenue("Ankara");
        event.setStartsAt(Instant.parse("2027-11-01T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-11-01T17:00:00Z"));
        event.setCapacity(150);
        event.setPublished(false);
        event = eventJpaRepository.saveAndFlush(event);

        String accessToken = loginAndGetAccessToken("stale-publish-owner@test.com");

        PublishEventRequest firstPublish = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(firstPublish)))
                .andExpect(status().isOk());

        PublishEventRequest stalePublish = new PublishEventRequest(event.getVersion());

        mockMvc.perform(post("/api/events/{id}/publish", event.getId())
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(stalePublish)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListAllEventsWhenCallerIsAdmin() throws Exception {
        UserEntity owner1 = saveUser("owner1-list@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner2 = saveUser("owner2-list@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        saveUser("admin-list@test.com", "Password123", Set.of(UserRole.ADMIN));

        saveEvent(owner1.getId(), "Owner1 Event");
        saveEvent(owner2.getId(), "Owner2 Event");

        String accessToken = loginAndGetAccessToken("admin-list@test.com");

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void shouldFilterEventsByOwnerIdWhenCallerIsAdmin() throws Exception {
        UserEntity owner1 = saveUser("owner1-filter@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner2 = saveUser("owner2-filter@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        saveUser("admin-filter@test.com", "Password123", Set.of(UserRole.ADMIN));

        saveEvent(owner1.getId(), "Owner1 Event A");
        saveEvent(owner1.getId(), "Owner1 Event B");
        saveEvent(owner2.getId(), "Owner2 Event");

        String accessToken = loginAndGetAccessToken("admin-filter@test.com");

        mockMvc.perform(get("/api/events")
                        .param("ownerId", owner1.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ownerId").value(owner1.getId()))
                .andExpect(jsonPath("$[1].ownerId").value(owner1.getId()));
    }

    @Test
    void shouldListOnlyOwnEventsWhenCallerIsOrganizer() throws Exception {
        UserEntity organizer = saveUser("organizer-list@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity anotherOrganizer = saveUser("another-organizer-list@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(organizer.getId(), "My Event 1");
        saveEvent(organizer.getId(), "My Event 2");
        saveEvent(anotherOrganizer.getId(), "Other Event");

        String accessToken = loginAndGetAccessToken("organizer-list@test.com");

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].ownerId").value(organizer.getId()))
                .andExpect(jsonPath("$[1].ownerId").value(organizer.getId()));
    }

    @Test
    void shouldListOwnEventsWhenOrganizerFiltersByOwnOwnerId() throws Exception {
        UserEntity organizer = saveUser("organizer-own-filter@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity anotherOrganizer = saveUser("another-own-filter@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(organizer.getId(), "My Event");
        saveEvent(anotherOrganizer.getId(), "Other Event");

        String accessToken = loginAndGetAccessToken("organizer-own-filter@test.com");

        mockMvc.perform(get("/api/events")
                        .param("ownerId", organizer.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ownerId").value(organizer.getId()));
    }

    @Test
    void shouldReturnForbiddenWhenOrganizerRequestsAnotherOwnersEvents() throws Exception {
        UserEntity organizer = saveUser("organizer-forbidden-list@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity anotherOrganizer = saveUser("another-forbidden-list@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(organizer.getId(), "My Event");
        saveEvent(anotherOrganizer.getId(), "Other Event");

        String accessToken = loginAndGetAccessToken("organizer-forbidden-list@test.com");

        mockMvc.perform(get("/api/events")
                        .param("ownerId", anotherOrganizer.getId().toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenCallerIsCustomer() throws Exception {
        saveUser("customer-list@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        String accessToken = loginAndGetAccessToken("customer-list@test.com");

        mockMvc.perform(get("/api/events")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenListingEventsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/events")).andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnOnlyPublishedEventsForPublicSearch() throws Exception {
        UserEntity owner = saveUser("public-owner@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(
                owner.getId(),
                "Published Event 1",
                "Istanbul",
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100,
                true
        );

        saveEvent(
                owner.getId(),
                "Draft Event",
                "Ankara",
                Instant.parse("2027-06-11T09:00:00Z"),
                Instant.parse("2027-06-11T17:00:00Z"),
                100,
                false
        );

        saveEvent(
                owner.getId(),
                "Published Event 2",
                "Izmir",
                Instant.parse("2027-06-12T09:00:00Z"),
                Instant.parse("2027-06-12T17:00:00Z"),
                100,
                true
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].published").value(true))
                .andExpect(jsonPath("$[1].published").value(true));
    }

    @Test
    void shouldFilterPublicEventsByFromDate() throws Exception {
        UserEntity owner = saveUser("public-owner2@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(
                owner.getId(),
                "Old Event",
                "Istanbul",
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100,
                true
        );

        saveEvent(
                owner.getId(),
                "New Event",
                "Ankara",
                Instant.parse("2027-07-10T09:00:00Z"),
                Instant.parse("2027-07-10T17:00:00Z"),
                100,
                true
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/public")
                        .param("from", "2027-07-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("New Event"));
    }

    @Test
    void shouldFilterPublicEventsByToDate() throws Exception {
        UserEntity owner = saveUser("public-owner3@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(
                owner.getId(),
                "Early Event",
                "Istanbul",
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100,
                true
        );

        saveEvent(
                owner.getId(),
                "Late Event",
                "Ankara",
                Instant.parse("2027-08-10T09:00:00Z"),
                Instant.parse("2027-08-10T17:00:00Z"),
                100,
                true
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/public")
                        .param("to", "2027-07-01T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Early Event"));
    }

    @Test
    void shouldFilterPublicEventsByQueryOnTitle() throws Exception {
        UserEntity owner = saveUser("public-owner4@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(
                owner.getId(),
                "Spring Boot Summit",
                "Istanbul",
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100,
                true
        );

        saveEvent(
                owner.getId(),
                "Kotlin Days",
                "Ankara",
                Instant.parse("2027-06-11T09:00:00Z"),
                Instant.parse("2027-06-11T17:00:00Z"),
                100,
                true
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/public")
                        .param("q", "spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Spring Boot Summit"));
    }

    @Test
    void shouldFilterPublicEventsByQueryOnVenue() throws Exception {
        UserEntity owner = saveUser("public-owner5@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(
                owner.getId(),
                "Tech Event",
                "Istanbul Congress Center",
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100,
                true
        );

        saveEvent(
                owner.getId(),
                "Another Event",
                "Ankara Arena",
                Instant.parse("2027-06-11T09:00:00Z"),
                Instant.parse("2027-06-11T17:00:00Z"),
                100,
                true
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/public")
                        .param("q", "istanbul"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].venue").value("Istanbul Congress Center"));
    }

    @Test
    void shouldAllowPublicAccessToPublicEventSearch() throws Exception {
        mockMvc.perform(get("/api/events/public"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnPublicEventsOrderedByStartDateAscending() throws Exception {
        UserEntity owner = saveUser("public-owner6@test.com", "Password123", Set.of(UserRole.ORGANIZER));

        saveEvent(
                owner.getId(),
                "Later Event",
                "Istanbul",
                Instant.parse("2027-08-10T09:00:00Z"),
                Instant.parse("2027-08-10T17:00:00Z"),
                100,
                true
        );

        saveEvent(
                owner.getId(),
                "Earlier Event",
                "Ankara",
                Instant.parse("2027-06-10T09:00:00Z"),
                Instant.parse("2027-06-10T17:00:00Z"),
                100,
                true
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/events/public"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Earlier Event"))
                .andExpect(jsonPath("$[1].title").value("Later Event"));
    }

    private UserEntity saveUser(String email, String password, Set<UserRole> roles) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRoles(roles);
        user.setCreatedAt(Instant.now());
        return userJpaRepository.save(user);
    }

    private void saveEvent(Long ownerId, String title) {
        EventEntity event = new EventEntity();
        event.setOwnerId(ownerId);
        event.setTitle(title);
        event.setVenue("Venue");
        event.setStartsAt(Instant.parse("2027-12-10T09:00:00Z"));
        event.setEndsAt(Instant.parse("2027-12-10T17:00:00Z"));
        event.setCapacity(100);
        event.setPublished(false);
        eventJpaRepository.saveAndFlush(event);
    }

    private EventEntity saveEvent(Long ownerId,
                                  String title,
                                  String venue,
                                  Instant startsAt,
                                  Instant endsAt,
                                  int capacity,
                                  boolean published) {
        EventEntity event = new EventEntity();
        event.setOwnerId(ownerId);
        event.setTitle(title);
        event.setVenue(venue);
        event.setStartsAt(startsAt);
        event.setEndsAt(endsAt);
        event.setCapacity(capacity);
        event.setPublished(published);
        return eventJpaRepository.saveAndFlush(event);
    }
}
