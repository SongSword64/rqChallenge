package com.reliaquest.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.reliaquest.api.model.CreateEmployeeDTO;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.model.EmployeesResponse;
import com.reliaquest.api.model.SingleEmployeeResponse;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final WebClient employeeClient;

    private static final Retry RETRY_429 = Retry.backoff(3, Duration.ofMillis(200))
            .filter(ex -> ex instanceof WebClientResponseException.TooManyRequests);

    /** Fetch all employees, return empty list if none */
    @Cacheable("employees")
    public List<Employee> getAll() {
        return employeeClient
                .get()
                .retrieve()
                .bodyToMono(EmployeesResponse.class)
                .map(EmployeesResponse::getData)
                .retryWhen(RETRY_429)
                .onErrorResume(WebClientResponseException.TooManyRequests.class, ex -> {
                    log.warn("429 Too Many Requests on getAll, returning empty list");
                    return Mono.just(Collections.emptyList());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .blockOptional()
                .orElse(Collections.emptyList());
    }

    /** Fetch employee by ID, return null if not found */
    public Employee getById(String id) {
        // Parse UUID safely
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid UUID format for getById({})", id);
            return null;
        }

        return employeeClient
                .get()
                .uri("/{id}", uuid)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {
                        return resp.bodyToMono(SingleEmployeeResponse.class)
                                .flatMap(r -> Mono.justOrEmpty(r.getData()));
                    } else if (resp.statusCode().equals(HttpStatus.NOT_FOUND)) {
                        return Mono.empty();
                    } else {
                        return resp.createException().flatMap(Mono::error);
                    }
                })
                .retryWhen(RETRY_429)
                .onErrorResume(WebClientResponseException.NotFound.class, ex -> Mono.empty())
                .onErrorResume(WebClientResponseException.TooManyRequests.class, ex -> {
                    log.warn("429 Too Many Requests on getById({}), returning null", uuid);
                    return Mono.empty();
                })
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    /** Search employees by name fragment, return empty list if none */
    public List<Employee> searchByName(String fragment) {
        String lowerFragment = fragment.toLowerCase();
        return getAll().stream()
                .filter(e -> e.getEmployeeName() != null
                        && e.getEmployeeName().toLowerCase().contains(lowerFragment))
                .toList();
    }

    /** Create a new employee */
    @CacheEvict(value = "employees", allEntries = true)
    public Employee create(CreateEmployeeDTO input) {
        return employeeClient
                .post()
                .bodyValue(input)
                .retrieve()
                .bodyToMono(SingleEmployeeResponse.class)
                .map(resp -> resp.getData())
                .retryWhen(RETRY_429)
                .subscribeOn(Schedulers.boundedElastic())
                .block();
    }

    /** Delete employee by ID */
    @CacheEvict(value = "employees", allEntries = true)
    public boolean delete(String id) throws JsonProcessingException {
        Employee existingEmployee = getById(id);
        if (existingEmployee == null) {
            log.info("Employee with id {} not found for deletion", id);
            return false;
        }
        Map<String, Object> requestBody = Map.of("name", existingEmployee.getEmployeeName());
        try {
            employeeClient
                    .method(HttpMethod.DELETE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .toBodilessEntity()
                    .retryWhen(RETRY_429)
                    .block();

            return true;
        } catch (WebClientResponseException.NotFound e) {
            log.warn("Employee {} not found on delete", id);
            return false;
        } catch (WebClientResponseException.TooManyRequests e) {
            log.warn("429 Too Many Requests on delete({})", id);
            return false;
        }
    }
}
