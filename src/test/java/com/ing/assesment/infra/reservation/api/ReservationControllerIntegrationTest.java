package com.ing.assesment.infra.reservation.api;

import com.ing.assesment.domain.auth.model.UserRole;
import com.ing.assesment.infra.audit.persistence.repository.AuditLogJpaRepository;
import com.ing.assesment.infra.auth.api.request.LoginRequest;
import com.ing.assesment.infra.auth.persistence.entity.UserEntity;
import com.ing.assesment.infra.auth.persistence.repository.UserJpaRepository;
import com.ing.assesment.infra.common.AbstractIntegrationTest;
import com.ing.assesment.infra.event.persistence.entity.EventEntity;
import com.ing.assesment.infra.event.persistence.repository.EventJpaRepository;
import com.ing.assesment.infra.idempotency.persistence.repository.IdempotencyKeyJpaRepository;
import com.ing.assesment.infra.reservation.api.request.CreateReservationRequest;
import com.ing.assesment.infra.reservation.persistence.entity.ReservationEntity;
import com.ing.assesment.infra.reservation.persistence.repository.ReservationJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReservationControllerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private EventJpaRepository eventJpaRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @Autowired
    private IdempotencyKeyJpaRepository idempotencyKeyJpaRepository;

    @Autowired
    private AuditLogJpaRepository auditLogJpaRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @BeforeEach
    void cleanup() {
        reservationJpaRepository.deleteAll();
        idempotencyKeyJpaRepository.deleteAll();
        eventJpaRepository.deleteAll();
        userJpaRepository.deleteAll();
        auditLogJpaRepository.deleteAll();
        rateLimitingFilter.clear();
    }

    @Test
    void shouldCreatePendingReservationSuccessfully() throws Exception {
        UserEntity organizer = saveUser("organizer@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("customer@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        String token = loginAndGetAccessToken("customer@test.com", "Password123");

        CreateReservationRequest request = new CreateReservationRequest(3);

        mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.eventId").value(event.getId()))
                .andExpect(jsonPath("$.userId").value(customer.getId()))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.seats").value(3));

        List<ReservationEntity> reservations = reservationJpaRepository.findAll();
        assertEquals(1, reservations.size());
        assertEquals(3, reservations.get(0).getSeats());
    }

    @Test
    void shouldReturnConflictWhenCapacityExceeded() throws Exception {
        UserEntity organizer = saveUser("organizer2@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("customer2@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 5);
        String token = loginAndGetAccessToken("customer2@test.com", "Password123");

        CreateReservationRequest request = new CreateReservationRequest(6);

        mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-2")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict());

        assertEquals(0, reservationJpaRepository.count());
    }

    @Test
    void shouldReturnConflictWhenEventIsDraft() throws Exception {
        UserEntity organizer = saveUser("organizer3@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("customer3@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = saveDraftFutureEvent(organizer.getId(), 50);
        String token = loginAndGetAccessToken("customer3@test.com", "Password123");

        CreateReservationRequest request = new CreateReservationRequest(2);

        mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldRequireIdempotencyKey() throws Exception {
        UserEntity organizer = saveUser("organizer4@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("customer4@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 50);
        String token = loginAndGetAccessToken("customer4@test.com", "Password123");

        CreateReservationRequest request = new CreateReservationRequest(2);

        mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldBeIdempotentForSameKeyAndSameRequest() throws Exception {
        UserEntity organizer = saveUser("organizer5@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("customer5@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 50);
        String token = loginAndGetAccessToken("customer5@test.com", "Password123");

        CreateReservationRequest request = new CreateReservationRequest(2);

        String firstResponse = mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String secondResponse = mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer firstId = com.jayway.jsonpath.JsonPath.read(firstResponse, "$.id");
        Integer secondId = com.jayway.jsonpath.JsonPath.read(secondResponse, "$.id");

        assertEquals(firstId, secondId);
        assertEquals(1, reservationJpaRepository.count());
    }

    @Test
    void shouldReturnConflictForSameIdempotencyKeyWithDifferentPayload() throws Exception {
        UserEntity organizer = saveUser("organizer6@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("customer6@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 50);
        String token = loginAndGetAccessToken("customer6@test.com", "Password123");

        mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateReservationRequest(2))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/events/{id}/reservations", event.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "reservation-6")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateReservationRequest(4))))
                .andExpect(status().isConflict());

        assertEquals(1, reservationJpaRepository.count());
    }

    @Test
    void concurrentReservationsShouldBothSucceedWhenCapacityAllows() throws Exception {
        UserEntity organizer = saveUser("organizer7@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer1 = saveUser("customer7a@test.com", "Password123", Set.of(UserRole.CUSTOMER));
        UserEntity customer2 = saveUser("customer7b@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 10);

        String token1 = loginAndGetAccessToken("customer7a@test.com", "Password123");
        String token2 = loginAndGetAccessToken("customer7b@test.com", "Password123");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> task1 = () -> performConcurrentReservation(event.getId(), token1, "concurrent-1", 4, ready, start);
        Callable<Integer> task2 = () -> performConcurrentReservation(event.getId(), token2, "concurrent-2", 4, ready, start);

        Future<Integer> future1 = executor.submit(task1);
        Future<Integer> future2 = executor.submit(task2);

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int status1 = future1.get(10, TimeUnit.SECONDS);
        int status2 = future2.get(10, TimeUnit.SECONDS);

        executor.shutdown();

        assertEquals(201, status1);
        assertEquals(201, status2);

        List<ReservationEntity> reservations = reservationJpaRepository.findAll();
        assertEquals(2, reservations.size());

        int totalSeats = reservations.stream().mapToInt(ReservationEntity::getSeats).sum();
        assertEquals(8, totalSeats);
    }

    @Test
    void concurrentReservationsShouldPreventOversellingWhenCapacityExceeded() throws Exception {
        UserEntity organizer = saveUser("organizer8@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer1 = saveUser("customer8a@test.com", "Password123", Set.of(UserRole.CUSTOMER));
        UserEntity customer2 = saveUser("customer8b@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 5);

        String token1 = loginAndGetAccessToken("customer8a@test.com", "Password123");
        String token2 = loginAndGetAccessToken("customer8b@test.com", "Password123");

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<Integer> task1 = () -> performConcurrentReservation(event.getId(), token1, "concurrent-3", 4, ready, start);
        Callable<Integer> task2 = () -> performConcurrentReservation(event.getId(), token2, "concurrent-4", 4, ready, start);

        Future<Integer> future1 = executor.submit(task1);
        Future<Integer> future2 = executor.submit(task2);

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();

        int status1 = future1.get(10, TimeUnit.SECONDS);
        int status2 = future2.get(10, TimeUnit.SECONDS);

        executor.shutdown();

        assertTrue((status1 == 201 && status2 == 409) || (status1 == 409 && status2 == 201));

        List<ReservationEntity> reservations = reservationJpaRepository.findAll();
        assertEquals(1, reservations.size());
        assertEquals(4, reservations.get(0).getSeats());

        int totalSeats = reservations.stream().mapToInt(ReservationEntity::getSeats).sum();
        assertTrue(totalSeats <= 5);
    }

    private int performConcurrentReservation(Long eventId,
                                             String token,
                                             String idempotencyKey,
                                             int seats,
                                             CountDownLatch ready,
                                             CountDownLatch start) throws Exception {
        ready.countDown();
        start.await(5, TimeUnit.SECONDS);

        return mockMvc.perform(post("/api/events/{id}/reservations", eventId)
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(new CreateReservationRequest(seats))))
                .andReturn()
                .getResponse()
                .getStatus();
    }

    @Test
    void shouldConfirmReservationWhenCallerIsOwner() throws Exception {
        UserEntity organizer = saveUser("confirm-org@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("confirm-owner@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                customer.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("confirm-owner@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()))
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        ReservationEntity updated = reservationJpaRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(com.ing.assesment.domain.reservation.model.ReservationStatus.CONFIRMED, updated.getStatus());
    }

    @Test
    void shouldConfirmReservationWhenCallerIsAdmin() throws Exception {
        UserEntity organizer = saveUser("confirm-org2@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("confirm-customer2@test.com", "Password123", Set.of(UserRole.CUSTOMER));
        saveUser("confirm-admin@test.com", "Password123", Set.of(UserRole.ADMIN));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                customer.getId(),
                3,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("confirm-admin@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void shouldReturnForbiddenWhenCallerIsNotOwnerOrAdmin() throws Exception {
        UserEntity organizer = saveUser("confirm-org3@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("confirm-real-owner@test.com", "Password123", Set.of(UserRole.CUSTOMER));
        UserEntity anotherCustomer = saveUser("confirm-other@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                1,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("confirm-other@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenConfirmingWithoutToken() throws Exception {
        UserEntity organizer = saveUser("confirm-org4@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("confirm-owner4@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenReservationDoesNotExist() throws Exception {
        saveUser("confirm-admin2@test.com", "Password123", Set.of(UserRole.ADMIN));

        String token = loginAndGetAccessToken("confirm-admin2@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenReservationAlreadyConfirmed() throws Exception {
        UserEntity organizer = saveUser("confirm-org5@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("confirm-owner5@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.CONFIRMED
        );

        String token = loginAndGetAccessToken("confirm-owner5@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturnConflictWhenReservationIsCancelled() throws Exception {
        UserEntity organizer = saveUser("confirm-org6@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("confirm-owner6@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.CANCELLED
        );

        String token = loginAndGetAccessToken("confirm-owner6@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldCancelReservationWhenCallerIsOwner() throws Exception {
        UserEntity organizer = saveUser("cancel-org@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("cancel-owner@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                customer.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("cancel-owner@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(reservation.getId()))
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        ReservationEntity updated = reservationJpaRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(com.ing.assesment.domain.reservation.model.ReservationStatus.CANCELLED, updated.getStatus());
    }

    @Test
    void shouldCancelReservationWhenCallerIsAdmin() throws Exception {
        UserEntity organizer = saveUser("cancel-org2@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("cancel-customer2@test.com", "Password123", Set.of(UserRole.CUSTOMER));
        saveUser("cancel-admin@test.com", "Password123", Set.of(UserRole.ADMIN));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                customer.getId(),
                3,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("cancel-admin@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldCancelConfirmedReservation() throws Exception {
        UserEntity organizer = saveUser("cancel-org3@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("cancel-owner3@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                customer.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.CONFIRMED
        );

        String token = loginAndGetAccessToken("cancel-owner3@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturnForbiddenWhenCancellingWithoutToken() throws Exception {
        UserEntity organizer = saveUser("cancel-org5@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("cancel-owner5@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnForbiddenWhenCallerIsNotOwnerOrAdminForCancel() throws Exception {
        UserEntity organizer = saveUser("cancel-org4@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("cancel-real-owner@test.com", "Password123", Set.of(UserRole.CUSTOMER));
        UserEntity anotherCustomer = saveUser("cancel-other@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                1,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("cancel-other@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturnNotFoundWhenReservationDoesNotExistForCancel() throws Exception {
        saveUser("cancel-admin2@test.com", "Password123", Set.of(UserRole.ADMIN));

        String token = loginAndGetAccessToken("cancel-admin2@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/cancel", 999999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnConflictWhenReservationAlreadyCancelled() throws Exception {
        UserEntity organizer = saveUser("cancel-org6@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity owner = saveUser("cancel-owner6@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                owner.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.CANCELLED
        );

        String token = loginAndGetAccessToken("cancel-owner6@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldWriteAuditLogWhenReservationIsConfirmed() throws Exception {
        UserEntity organizer = saveUser("audit-org@test.com", "Password123", Set.of(UserRole.ORGANIZER));
        UserEntity customer = saveUser("audit-owner@test.com", "Password123", Set.of(UserRole.CUSTOMER));

        EventEntity event = savePublishedFutureEvent(organizer.getId(), 100);
        ReservationEntity reservation = saveReservation(
                event.getId(),
                customer.getId(),
                2,
                com.ing.assesment.domain.reservation.model.ReservationStatus.PENDING
        );

        String token = loginAndGetAccessToken("audit-owner@test.com", "Password123");

        mockMvc.perform(post("/api/reservations/{id}/confirm", reservation.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        List<com.ing.assesment.infra.audit.persistence.entity.AuditLogEntity> logs = auditLogJpaRepository.findAll();

        assertEquals(1, logs.size());

        com.ing.assesment.infra.audit.persistence.entity.AuditLogEntity log = logs.get(0);

        assertEquals(customer.getId(), log.getActorId());
        assertEquals("CONFIRM_RESERVATION", log.getAction());
        assertEquals("RESERVATION", log.getResourceType());
        assertEquals(String.valueOf(reservation.getId()), log.getResourceId());
        assertNotNull(log.getCreatedAt());
        assertNotNull(log.getIp());
        assertNotNull(log.getUserAgent());
    }

    private ReservationEntity saveReservation(Long eventId, Long userId, int seats,
                                              com.ing.assesment.domain.reservation.model.ReservationStatus status) {
        ReservationEntity reservation = new ReservationEntity();
        reservation.setEventId(eventId);
        reservation.setUserId(userId);
        reservation.setSeats(seats);
        reservation.setStatus(status);
        reservation.setCreatedAt(Instant.now());
        return reservationJpaRepository.saveAndFlush(reservation);
    }

    private UserEntity saveUser(String email, String password, Set<UserRole> roles) {
        UserEntity user = new UserEntity();
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(password));
        user.setRoles(roles);
        user.setCreatedAt(Instant.now());
        return userJpaRepository.saveAndFlush(user);
    }

    private EventEntity savePublishedFutureEvent(Long ownerId, int capacity) {
        EventEntity event = new EventEntity();
        event.setOwnerId(ownerId);
        event.setTitle("Reservable Event");
        event.setVenue("Istanbul");
        event.setStartsAt(Instant.now().plusSeconds(7200));
        event.setEndsAt(Instant.now().plusSeconds(10800));
        event.setCapacity(capacity);
        event.setPublished(true);
        return eventJpaRepository.saveAndFlush(event);
    }

    private EventEntity saveDraftFutureEvent(Long ownerId, int capacity) {
        EventEntity event = new EventEntity();
        event.setOwnerId(ownerId);
        event.setTitle("Draft Event");
        event.setVenue("Istanbul");
        event.setStartsAt(Instant.now().plusSeconds(7200));
        event.setEndsAt(Instant.now().plusSeconds(10800));
        event.setCapacity(capacity);
        event.setPublished(false);
        return eventJpaRepository.saveAndFlush(event);
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        LoginRequest loginRequest = new LoginRequest(email, password);

        String loginResponse = mockMvc.perform(
                        post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(toJson(loginRequest))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return com.jayway.jsonpath.JsonPath.read(loginResponse, "$.accessToken");
    }
}
