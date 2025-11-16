package com.reliaquest.api.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.reliaquest.api.model.CreateEmployeeDTO;
import com.reliaquest.api.model.Employee;
import com.reliaquest.api.service.EmployeeService;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping("/employee")
@RequiredArgsConstructor
@Slf4j
public class EmployeeController implements IEmployeeController<Employee, CreateEmployeeDTO> {

    private final EmployeeService service;

    @Override
    public ResponseEntity<List<Employee>> getAllEmployees() {
        log.debug("Fetching all employees");
        return ResponseEntity.ok(service.getAll());
    }

    @Override
    public ResponseEntity<List<Employee>> getEmployeesByNameSearch(String searchString) {
        log.debug("Searching employees by name with search string: {}", searchString);
        List<Employee> list = service.searchByName(searchString);
        if((list == null || list.isEmpty())){
            log.debug("No employees found matching search string: {}", searchString);
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.ok(list);
        }
    }

    @Override
    public ResponseEntity<Employee> getEmployeeById(String id) {
        log.debug("Fetching employee by id: {}", id);
        if (id == null || id.isBlank()) {
            log.warn("Invalid employee id provided: {}", id);
            return ResponseEntity.badRequest().build();
        }
        Employee employee = service.getById(id);
        return (employee == null) ? ResponseEntity.notFound().build() : ResponseEntity.ok(employee);
    }

    @Override
    public ResponseEntity<Integer> getHighestSalaryOfEmployees() {
        log.debug("Calculating highest salary among employees");
        int max = service.getAll().stream()
                .map(Employee::getEmployeeSalary)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0);

        return ResponseEntity.ok(max);
    }

    @Override
    public ResponseEntity<List<String>> getTopTenHighestEarningEmployeeNames() {
        log.debug("Fetching top ten highest earning employee names");
        List<String> list = service.getAll().stream()
                .sorted(Comparator.comparing(Employee::getEmployeeSalary).reversed())
                .limit(10)
                .map(Employee::getEmployeeName)
                .toList();

        return ResponseEntity.ok(list);
    }

    @Override
    public ResponseEntity<Employee> createEmployee(CreateEmployeeDTO input) {
        log.info("Creating new employee with name: {}", input.getName());
        return ResponseEntity.ok(service.create(input));
    }

    @Override
    public ResponseEntity<String> deleteEmployeeById(String id) {
        try {
            Employee employee = service.getById(id);
            if (service.delete(id)) {
                log.info("Employee with name {} deleted successfully.", employee.getEmployeeName());
                return ResponseEntity.ok(employee.getEmployeeName());
            } else {
                log.warn("Employee with id {} not found for deletion.", id);
                return ResponseEntity.notFound().build();
            }
        } catch (JsonProcessingException e) {
            log.warn(e.getMessage());
            return ResponseEntity.badRequest().build();
        }
    }
}
