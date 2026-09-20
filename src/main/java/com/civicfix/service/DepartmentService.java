package com.civicfix.service;

import com.civicfix.entity.Department;
import com.civicfix.exception.ResourceNotFoundException;
import com.civicfix.repository.DepartmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public DepartmentService(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    @Transactional(readOnly = true)
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Department getDepartmentById(Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + id));
    }

    @Transactional
    public Department createDepartmentIfAbsent(String name, String description) {
        return departmentRepository.findByName(name)
                .orElseGet(() -> departmentRepository.save(new Department(name, description)));
    }
}
