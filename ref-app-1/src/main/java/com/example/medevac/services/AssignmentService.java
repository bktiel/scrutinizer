package com.example.medevac.services;

import com.example.medevac.entities.Assignment;
import com.example.medevac.pojos.AssignmentDto;
import com.example.medevac.repositories.AssignmentRepository;
import com.example.medevac.repositories.NineLineUserRepository;
import com.example.medevac.repositories.SpecialEquipmentRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Validation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

@Service
public class AssignmentService {
    private final AssignmentRepository repo;
    private final SpecialEquipmentRepository equipmentRepo;
    private final NineLineUserRepository userRepo;
    private MedevacRequestService service;

    public AssignmentService(AssignmentRepository repo, SpecialEquipmentRepository equipmentRepo, NineLineUserRepository userRepo, MedevacRequestService service) {
        this.repo = repo;
        this.equipmentRepo = equipmentRepo;
        this.userRepo = userRepo;
    }

    @Autowired
    public void setMedevacRequestService(MedevacRequestService service) {
        this.service = service;
    }

    @Transactional
    public AssignmentDto saveAssignment(AssignmentDto assignment) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var violations = validator.validate(assignment);

            if (!violations.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id value.");
            }
        }

        final var requestOptional = service.findMedevacRequestById(assignment.requestId());

        if (requestOptional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Request not found.");
        }

        final var request = requestOptional.get();

        return AssignmentDto.from(repo.saveAndFlush(assignment.to(request, equipmentRepo)));
    }

    @Transactional
    public List<AssignmentDto> saveAllAssignments(List<AssignmentDto> dtos) {
        var idSet = new HashSet<Long>();

        for (var assignment : dtos) {
            try (var factory = Validation.buildDefaultValidatorFactory()) {
                var validator = factory.getValidator();
                var violations = validator.validate(assignment);

                if (!violations.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid id value.");
                }
            }

            if (idSet.contains(assignment.requestId())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicated values.");
            }

            idSet.add(assignment.requestId());
        }

        var set = new ArrayList<Assignment>();

        for (var assignDTO : dtos) {
            final var requestOptional = service.findRealMedevacRequestById(assignDTO.requestId());

            if (requestOptional.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request not found.");
            }

            final var request = requestOptional.get();

            set.add(assignDTO.to(request, equipmentRepo));
        }

        return repo.saveAllAndFlush(set).stream().map(AssignmentDto::from).toList();
    }

    public List<AssignmentDto> findAllAssignments() {
        final var assigns = repo.findAll();
        var result = new ArrayList<AssignmentDto>();

        for (var assign : assigns) {
            result.add(AssignmentDto.from(assign));
        }

        return result;
    }
}
