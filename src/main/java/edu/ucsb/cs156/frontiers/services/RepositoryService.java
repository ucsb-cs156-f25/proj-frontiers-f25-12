package edu.ucsb.cs156.frontiers.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.enums.RepositoryPermissions;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class RepositoryService {
  private final JwtService jwtService;
  private final RestTemplate restTemplate;
  private final ObjectMapper mapper;

  public RepositoryService(
      JwtService jwtService, RestTemplateBuilder restTemplateBuilder, ObjectMapper mapper) {
    this.jwtService = jwtService;
    this.restTemplate = restTemplateBuilder.build();
    this.mapper = mapper;
  }

  /**
   * Creates a single student repository if it doesn't already exist, and provisions access to the
   * repository by that student
   *
   * @param course The Course in question
   * @param student RosterStudent of the student the repository should be created for
   * @param repoPrefix Name of the project or assignment. Used to title the repository, in the
   *     format repoPrefix-githubLogin
   * @param isPrivate Whether the repository is private or not
   */
  public void createStudentRepository(
      Course course,
      RosterStudent student,
      String repoPrefix,
      Boolean isPrivate,
      RepositoryPermissions permissions)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String newRepoName = repoPrefix + "-" + student.getGithubLogin();
    String token = jwtService.getInstallationToken(course);
    String existenceEndpoint =
        "https://api.github.com/repos/" + course.getOrgName() + "/" + newRepoName;
    String createEndpoint = "https://api.github.com/orgs/" + course.getOrgName() + "/repos";
    String provisionEndpoint =
        "https://api.github.com/repos/"
            + course.getOrgName()
            + "/"
            + newRepoName
            + "/collaborators/"
            + student.getGithubLogin();
    HttpHeaders existenceHeaders = new HttpHeaders();
    existenceHeaders.add("Authorization", "Bearer " + token);
    existenceHeaders.add("Accept", "application/vnd.github+json");
    existenceHeaders.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> existenceEntity = new HttpEntity<>(existenceHeaders);

    try {
      restTemplate.exchange(existenceEndpoint, HttpMethod.GET, existenceEntity, String.class);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode().equals(HttpStatus.NOT_FOUND)) {
        HttpHeaders createHeaders = new HttpHeaders();
        createHeaders.add("Authorization", "Bearer " + token);
        createHeaders.add("Accept", "application/vnd.github+json");
        createHeaders.add("X-GitHub-Api-Version", "2022-11-28");

        Map<String, Object> body = new HashMap<>();
        body.put("name", newRepoName);
        body.put("private", isPrivate);
        String bodyAsJson = mapper.writeValueAsString(body);

        HttpEntity<String> createEntity = new HttpEntity<>(bodyAsJson, createHeaders);

        restTemplate.exchange(createEndpoint, HttpMethod.POST, createEntity, String.class);
      } else {
        log.warn(
            "Unexpected response code {} when checking for existence of repository {}",
            e.getStatusCode(),
            newRepoName);
        return;
      }
    }
    try {
      Map<String, Object> provisionBody = new HashMap<>();
      provisionBody.put("permission", permissions.getApiName());
      String provisionAsJson = mapper.writeValueAsString(provisionBody);

      HttpEntity<String> provisionEntity = new HttpEntity<>(provisionAsJson, existenceHeaders);
      restTemplate.exchange(provisionEndpoint, HttpMethod.PUT, provisionEntity, String.class);
    } catch (HttpClientErrorException ignored) {

    }
  }

  /**
   * Lists all repositories in the course's organization that match a given prefix
   *
   * @param course The Course whose organization to search
   * @param prefix The prefix to match (e.g., "jpa01" will match "jpa01-username1",
   *     "jpa01-username2", etc.)
   * @return List of repository names matching the prefix
   */
  public List<String> listRepositoriesByPrefix(Course course, String prefix)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String token = jwtService.getInstallationToken(course);
    String listEndpoint =
        "https://api.github.com/orgs/" + course.getOrgName() + "/repos?per_page=100";

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      ResponseEntity<String> response =
          restTemplate.exchange(listEndpoint, HttpMethod.GET, entity, String.class);

      List<Map<String, Object>> repos = mapper.readValue(response.getBody(), List.class);

      return repos.stream()
          .map(repo -> (String) repo.get("name"))
          .filter(name -> name.startsWith(prefix + "-"))
          .collect(java.util.stream.Collectors.toList());

    } catch (HttpClientErrorException e) {
      log.error("Error listing repositories for org {}: {}", course.getOrgName(), e.getMessage());
      throw e;
    }
  }

  /**
   * Deletes a repository from the course's organization
   *
   * @param course The Course whose organization contains the repository
   * @param repoName The name of the repository to delete
   */
  public void deleteRepository(Course course, String repoName)
      throws NoSuchAlgorithmException, InvalidKeySpecException, JsonProcessingException {
    String token = jwtService.getInstallationToken(course);
    String deleteEndpoint = "https://api.github.com/repos/" + course.getOrgName() + "/" + repoName;

    HttpHeaders headers = new HttpHeaders();
    headers.add("Authorization", "Bearer " + token);
    headers.add("Accept", "application/vnd.github+json");
    headers.add("X-GitHub-Api-Version", "2022-11-28");

    HttpEntity<String> entity = new HttpEntity<>(headers);

    try {
      restTemplate.exchange(deleteEndpoint, HttpMethod.DELETE, entity, String.class);
      log.info("Successfully deleted repository: {}/{}", course.getOrgName(), repoName);
    } catch (HttpClientErrorException e) {
      log.error(
          "Error deleting repository {}/{}: {}", course.getOrgName(), repoName, e.getMessage());
      throw e;
    }
  }
}
