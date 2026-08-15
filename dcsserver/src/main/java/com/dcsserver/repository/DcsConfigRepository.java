package com.dcsserver.repository;

import com.dcsserver.domain.DcsConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DcsConfigRepository extends JpaRepository<DcsConfig, String> {

    @Query("SELECT DISTINCT c.dcsServerIp FROM DcsConfig c WHERE c.dcsServerIp IS NOT NULL AND c.dcsServerIp <> ''")
    List<String> findDistinctServerIps();
}
