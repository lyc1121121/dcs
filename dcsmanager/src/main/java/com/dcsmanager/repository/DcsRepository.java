package com.dcsmanager.repository;

import com.dcsmanager.domain.Dcs;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DcsRepository extends JpaRepository<Dcs, String> {

    List<Dcs> findAllByOrderByDcsIdAsc();
}
