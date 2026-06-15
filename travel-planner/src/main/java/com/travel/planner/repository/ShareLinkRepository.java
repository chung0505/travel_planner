package com.travel.planner.repository;

import com.travel.planner.model.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByToken(String token);
}
