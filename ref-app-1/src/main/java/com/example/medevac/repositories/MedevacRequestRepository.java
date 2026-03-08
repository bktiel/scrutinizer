package com.example.medevac.repositories;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.medevac.entities.MedevacRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedevacRequestRepository extends JpaRepository<MedevacRequest, Long>
{
}
