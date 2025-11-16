package com.reliaquest.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.reliaquest.api.model.*;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.*;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class EmployeeServiceTest {

    @Mock
    private WebClient webClient;

    // --- GET ALL ---
    @Mock
    private WebClient.RequestHeadersUriSpec getUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec getHeadersSpec;

    @Mock
    private WebClient.ResponseSpec getResponseSpec;

    // --- GET BY ID ---
    @Mock
    private WebClient.RequestHeadersUriSpec getByIdUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec getByIdHeadersSpec;

    @Mock
    private ClientResponse clientResponse;

    // --- POST ---
    @Mock
    private WebClient.RequestBodyUriSpec postUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec postHeadersSpec;

    @Mock
    private WebClient.ResponseSpec postResponseSpec;

    // --- DELETE ---
    @Mock
    private WebClient.RequestBodyUriSpec deleteUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec deleteHeadersSpec;

    @Mock
    private WebClient.ResponseSpec deleteResponseSpec;

    private EmployeeService service;

    @BeforeEach
    void setup() {
        service = new EmployeeService(webClient);

        // GET ALL chain
        when(webClient.get()).thenReturn(getUriSpec);
        when(getUriSpec.retrieve()).thenReturn(getResponseSpec);

        // POST chain
        when(webClient.post()).thenReturn(postUriSpec);
        when(postUriSpec.bodyValue(any())).thenReturn(postHeadersSpec);
        when(postHeadersSpec.retrieve()).thenReturn(postResponseSpec);

        // DELETE chain
        when(webClient.method(HttpMethod.DELETE)).thenReturn(deleteUriSpec);
        when(deleteUriSpec.bodyValue(any())).thenReturn(deleteHeadersSpec);
        when(deleteHeadersSpec.retrieve()).thenReturn(deleteResponseSpec);
    }

    // ------------------------------------------------------------
    // GET ALL
    // ------------------------------------------------------------
    @Test
    void getAll_ReturnsList() {
        Employee emp = new Employee();
        emp.setId("1");
        emp.setEmployeeName("Test");

        EmployeesResponse resp = new EmployeesResponse();
        resp.setData(List.of(emp));

        when(getResponseSpec.bodyToMono(EmployeesResponse.class)).thenReturn(Mono.just(resp));

        List<Employee> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals("Test", result.get(0).getEmployeeName());
    }

    // ------------------------------------------------------------
    // GET BY ID success
    // ------------------------------------------------------------
    @Test
    void getById_ReturnsEmployee() {
        UUID uuid = UUID.randomUUID();

        when(webClient.get()).thenReturn(getByIdUriSpec);
        when(getByIdUriSpec.uri("/{id}", uuid)).thenReturn(getByIdHeadersSpec);

        when(getByIdHeadersSpec.exchangeToMono(any())).then(inv -> {
            var fn = inv.getArgument(0, java.util.function.Function.class);
            return (Mono) fn.apply(clientResponse);
        });

        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);

        Employee e = new Employee();
        e.setId(uuid.toString());
        e.setEmployeeName("John");

        SingleEmployeeResponse ser = new SingleEmployeeResponse();
        ser.setData(e);

        when(clientResponse.bodyToMono(SingleEmployeeResponse.class)).thenReturn(Mono.just(ser));

        Employee result = service.getById(uuid.toString());

        assertNotNull(result);
        assertEquals("John", result.getEmployeeName());
    }

    // ------------------------------------------------------------
    // GET BY ID NotFound
    // ------------------------------------------------------------
    @Test
    void getById_NotFound_ReturnsNull() {
        UUID uuid = UUID.randomUUID();

        when(webClient.get()).thenReturn(getByIdUriSpec);
        when(getByIdUriSpec.uri("/{id}", uuid)).thenReturn(getByIdHeadersSpec);

        when(getByIdHeadersSpec.exchangeToMono(any())).then(inv -> {
            var fn = inv.getArgument(0, java.util.function.Function.class);
            return (Mono) fn.apply(clientResponse);
        });

        when(clientResponse.statusCode()).thenReturn(HttpStatus.NOT_FOUND);

        Employee result = service.getById(uuid.toString());

        assertNull(result);
    }

    // ------------------------------------------------------------
    // POST create
    // ------------------------------------------------------------
    @Test
    void create_ReturnsEmployee() {
        Employee e = new Employee();
        e.setId("1");
        e.setEmployeeName("Created");

        SingleEmployeeResponse ser = new SingleEmployeeResponse();
        ser.setData(e);

        when(postResponseSpec.bodyToMono(SingleEmployeeResponse.class)).thenReturn(Mono.just(ser));

        CreateEmployeeDTO dto = new CreateEmployeeDTO("John", 100, 10, "Dev", "IT");

        Employee result = service.create(dto);

        assertNotNull(result);
        assertEquals("1", result.getId());
    }

    // ------------------------------------------------------------
    // DELETE success
    // ------------------------------------------------------------
    @Test
    void delete_ReturnsTrue() throws JsonProcessingException {
        UUID uuid = UUID.randomUUID();

        // mock getById call inside delete()
        when(webClient.get()).thenReturn(getByIdUriSpec);
        when(getByIdUriSpec.uri("/{id}", uuid)).thenReturn(getByIdHeadersSpec);

        when(getByIdHeadersSpec.exchangeToMono(any())).then(inv -> {
            var fn = inv.getArgument(0, java.util.function.Function.class);
            return (Mono) fn.apply(clientResponse);
        });

        when(clientResponse.statusCode()).thenReturn(HttpStatus.OK);

        Employee e = new Employee();
        e.setId(uuid.toString());
        e.setEmployeeName("ToDelete");

        SingleEmployeeResponse ser = new SingleEmployeeResponse();
        ser.setData(e);

        when(clientResponse.bodyToMono(SingleEmployeeResponse.class)).thenReturn(Mono.just(ser));

        // mock DELETE success
        when(deleteResponseSpec.toBodilessEntity()).thenReturn(Mono.empty());

        boolean result = service.delete(uuid.toString());

        assertTrue(result);
    }
}
