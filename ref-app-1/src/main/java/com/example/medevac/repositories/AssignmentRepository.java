package com.example.medevac.repositories;

import com.example.medevac.entities.Assignment;
import com.example.medevac.entities.MedevacRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssignmentRepository extends JpaRepository<Assignment, Long>
{
    List<Assignment> findByResponder(Long responderId);
}
