package com.reliaquest.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reliaquest.api.controller.EmployeeController;
import com.reliaquest.api.model.CreateEmployeeDTO;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeController.class)
class EmployeeControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private EmployeeService service;

    // ------------------------------------------------------
    // GET /employee/{id}
    // ------------------------------------------------------
    @Test
    @DisplayName("getEmployeeById - returns 200 with employee")
    void getById_Returns200() throws Exception {
        Employee e = new Employee();
        e.setId("6ca4dc9b-c0c9-45e6-8498-79458fe9ecd0");
        e.setEmployeeName("John");

        when(service.getById("6ca4dc9b-c0c9-45e6-8498-79458fe9ecd0")).thenReturn(e);

        mockMvc.perform(get("/employee/{id}", "6ca4dc9b-c0c9-45e6-8498-79458fe9ecd0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("6ca4dc9b-c0c9-45e6-8498-79458fe9ecd0"))
                .andExpect(jsonPath("$.employee_name").value("John"));
    }

    @Test
    @DisplayName("getEmployeeById - returns 404 when not found")
    void getById_Returns404() throws Exception {
        when(service.getById("6ca4dc9b-c0c9-45e6-8498-79458fe9ecd0")).thenReturn(null);

        mockMvc.perform(get("/employee/{id}", "6ca4dc9b-c0c9-45e6-8498-79458fe9ecd0"))
                .andExpect(status().isNotFound());
    }

    // ------------------------------------------------------
    // GET /employee/search/{fragment}
    // ------------------------------------------------------
    @Test
    @DisplayName("searchByName - returns 200 with list")
    void search_Returns200() throws Exception {
        Employee e1 = new Employee();
        e1.setId("1");
        e1.setEmployeeName("Alice");

        Employee e2 = new Employee();
        e2.setId("2");
        e2.setEmployeeName("Alicia");

        when(service.searchByName("ali")).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/employee/search/{fragment}", "ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].employee_name").value("Alice"))
                .andExpect(jsonPath("$[1].employee_name").value("Alicia"));
    }

    @Test
    @DisplayName("searchByName - returns 204 for empty result")
    void search_Returns204() throws Exception {
        when(service.searchByName("nobody")).thenReturn(List.of());

        mockMvc.perform(get("/employee/search/{fragment}", "nobody")).andExpect(status().isNoContent());
    }

    // ------------------------------------------------------
    // GET /employee
    // ------------------------------------------------------
    @Test
    @DisplayName("getAllEmployees - returns 200 with list")
    void getAllEmployees_Returns200() throws Exception {
        Employee e = new Employee();
        e.setId("111");
        e.setEmployeeName("Test User");

        when(service.getAll()).thenReturn(List.of(e));

        mockMvc.perform(get("/employee"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("111"));
    }

    // ------------------------------------------------------
    // GET /employee/highestSalary
    // ------------------------------------------------------
    @Test
    @DisplayName("getHighestSalaryOfEmployees - returns 200 with correct max")
    void getHighestSalary_ReturnsCorrectValue() throws Exception {
        Employee e1 = new Employee();
        e1.setEmployeeSalary(200);

        Employee e2 = new Employee();
        e2.setEmployeeSalary(500);

        when(service.getAll()).thenReturn(List.of(e1, e2));

        mockMvc.perform(get("/employee/highestSalary"))
                .andExpect(status().isOk())
                .andExpect(content().string("500"));
    }

    // ------------------------------------------------------
    // GET /employee/topTenHighestEarningEmployeeNames
    // ------------------------------------------------------
    @Test
    @DisplayName("topTenHighestEarningEmployeeNames - returns sorted top 10")
    void getTopTen_ReturnsCorrectList() throws Exception {
        Employee e1 = new Employee();
        e1.setEmployeeName("A");
        e1.setEmployeeSalary(100);
        Employee e2 = new Employee();
        e2.setEmployeeName("B");
        e2.setEmployeeSalary(300);
        Employee e3 = new Employee();
        e3.setEmployeeName("C");
        e3.setEmployeeSalary(200);

        when(service.getAll()).thenReturn(List.of(e1, e2, e3));

        mockMvc.perform(get("/employee/topTenHighestEarningEmployeeNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0]").value("B"))
                .andExpect(jsonPath("$[1]").value("C"))
                .andExpect(jsonPath("$[2]").value("A"));
    }

    // ------------------------------------------------------
    // POST /employee
    // ------------------------------------------------------
    @Test
    @DisplayName("createEmployee - returns 200 with created employee")
    void createEmployee_Returns200() throws Exception {
        CreateEmployeeDTO dto = new CreateEmployeeDTO("New Guy", 400, 33, "VP of Stuff", "guy@gmail.com");

        Employee created = new Employee();
        created.setId("999");
        created.setEmployeeName("New Guy");

        when(service.create(any(CreateEmployeeDTO.class))).thenReturn(created);

        mockMvc.perform(post("/employee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("999"))
                .andExpect(jsonPath("$.employee_name").value("New Guy"));
    }

    // ------------------------------------------------------
    // DELETE /employee/{id}
    // ------------------------------------------------------
    @Test
    @DisplayName("deleteEmployeeById - returns 200 with success message")
    void deleteEmployee_Returns200() throws Exception {
        when(service.getById("123")).thenReturn(new Employee());
        when(service.delete("123")).thenReturn(true);

        mockMvc.perform(delete("/employee/{id}", "123"))
                .andExpect(status().isOk())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("deleteEmployeeById - returns 404 when service returns false")
    void deleteEmployee_Returns404_WhenFalse() throws Exception {
        when(service.delete("123")).thenReturn(false);

        mockMvc.perform(delete("/employee/{id}", "123"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    @DisplayName("deleteEmployeeById - returns 400 on JsonProcessingException")
    void deleteEmployee_Returns400_OnException() throws Exception {
        when(service.delete("123")).thenThrow(new JsonProcessingException("test error") {});

        mockMvc.perform(delete("/employee/{id}", "123"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(""));
    }
}
