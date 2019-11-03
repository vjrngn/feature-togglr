package com.featuretogglr.featuretogglr.repositories;

import com.featuretogglr.featuretogglr.models.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;


public interface ProjectRepository extends JpaRepository<Project, UUID> {
}
