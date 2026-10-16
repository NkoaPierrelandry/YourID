package com.yourid.api.ai.repository;

import com.yourid.api.ai.entity.AiContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AiContentRepository extends JpaRepository<AiContent, UUID> {
}
