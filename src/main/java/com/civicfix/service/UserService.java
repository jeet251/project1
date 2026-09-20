package com.civicfix.service;

import com.civicfix.dto.RegisterRequest;
import com.civicfix.dto.UserDto;
import com.civicfix.entity.Department;
import com.civicfix.entity.Role;
import com.civicfix.entity.User;
import com.civicfix.exception.BadRequestException;
import com.civicfix.exception.ResourceNotFoundException;
import com.civicfix.repository.DepartmentRepository;
import com.civicfix.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       DepartmentRepository departmentRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.departmentRepository = departmentRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail().toLowerCase().trim())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        User user = new User();
        user.setName(request.getName().trim());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPhone(request.getPhone().trim());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole() != null ? request.getRole() : Role.ROLE_CITIZEN);

        if (request.getDepartmentId() != null) {
            Department department = departmentRepository.findById(request.getDepartmentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Department not found with ID: " + request.getDepartmentId()));
            user.setDepartment(department);
        }

        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase().trim())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + id));
    }

    @Transactional(readOnly = true)
    public List<UserDto> getOfficers(Long departmentId) {
        List<User> officers;
        if (departmentId != null) {
            officers = userRepository.findByRoleAndDepartmentId(Role.ROLE_OFFICER, departmentId);
        } else {
            officers = userRepository.findByRole(Role.ROLE_OFFICER);
        }
        return officers.stream().map(UserDto::fromEntity).collect(Collectors.toList());
    }
}
