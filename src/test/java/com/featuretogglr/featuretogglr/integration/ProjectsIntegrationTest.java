package com.featuretogglr.featuretogglr.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.featuretogglr.featuretogglr.models.Project;
import com.featuretogglr.featuretogglr.repositories.ProjectRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
@ContextConfiguration(initializers = {ProjectsIntegrationTest.Initializer.class})
class ProjectsIntegrationTest {

    @Container
    private static PostgreSQLContainer psql = new PostgreSQLContainer("postgres:11-alpine")
            .withDatabaseName("test")
            .withUsername("foo")
            .withPassword("secret");

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private ProjectRepository repository;

    @Test
    void shouldCreateAProject() throws Exception {

        Project project = new Project("Test");

        mockMvc.perform(post("/projects")
                .content(objectMapper.writeValueAsString(project)))
                .andExpect(status().isCreated());
    }

    @Test
    void shouldListAllProjects() throws Exception {
        Project test1 = new Project("Test1");
        Project test2 = new Project("Test2");

        repository.saveAll(asList(test1, test2));

        mockMvc.perform(get("/projects"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page.totalElements").isNumber())
                .andExpect(jsonPath("$._embedded.projects").isArray());
    }

    @Test
    void shouldGetAProject() throws Exception {
        String projectName = "TestProject1";
        Project project = new Project(projectName);
        repository.save(project);

        String projectId = project.getId().toString();

        mockMvc.perform(get("/projects/" + projectId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(projectName)));
    }

    @Test
    void shouldReturn404ForNonExistentProject() throws Exception {
        mockMvc.perform(get("/projects/" + UUID.randomUUID().toString()))
                .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    void shouldChangeNameOfAProject() throws Exception {
        Project project = new Project("TestProject1");
        repository.save(project);

        UUID id = project.getId();

        mockMvc.perform(patch("/projects/" + project.getId().toString())
                .content("{ \"name\": \"New Project\" }"))
                .andExpect(status().isNoContent());

        assertEquals("New Project", repository.getOne(id).getName());
    }

    @Test
    void shouldDeleteAProject() throws Exception {
        String projectName = "TestProject1";
        Project project = new Project(projectName);
        repository.save(project);

        String projectId = project.getId().toString();

        mockMvc.perform(delete("/projects/" + projectId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/projects/" + projectId))
                .andExpect(status().isNotFound());
    }

    static class Initializer
            implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + psql.getJdbcUrl(),
                    "spring.datasource.username=" + psql.getUsername(),
                    "spring.datasource.password=" + psql.getPassword()
            ).applyTo(configurableApplicationContext.getEnvironment());
        }
    }
}
