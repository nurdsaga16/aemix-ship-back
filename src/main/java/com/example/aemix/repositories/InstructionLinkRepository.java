package com.example.aemix.repositories;

import com.example.aemix.entities.InstructionLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InstructionLinkRepository extends JpaRepository<InstructionLink, Long> {

    List<InstructionLink> findAllByOrderBySortOrderAsc();
}
