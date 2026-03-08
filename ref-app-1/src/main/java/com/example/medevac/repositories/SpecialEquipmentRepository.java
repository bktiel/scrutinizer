package com.example.medevac.repositories;

import com.example.medevac.entities.SpecialEquipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialEquipmentRepository extends JpaRepository<SpecialEquipment, Long>
{
    SpecialEquipment findBySpecialEquipment(String specialEquipment);
}
