package edu.ucsb.cs156.frontiers.jobs;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class DeleteAssignmentRepositoriesJobTests {

  @Mock private RepositoryService repositoryService;
  @Mock private JobContext jobContext;

  @Test
  void testAccept_withReposToDelete() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    List<String> repos = Arrays.asList("jpa01-student1", "jpa01-student2", "jpa01-student3");

    when(repositoryService.listRepositoriesByPrefix(course, "jpa01")).thenReturn(repos);

    doNothing().when(repositoryService).deleteRepository(eq(course), anyString());

    DeleteAssignmentRepositoriesJob job =
        DeleteAssignmentRepositoriesJob.builder()
            .course(course)
            .repositoryService(repositoryService)
            .assignmentName("jpa01")
            .build();

    job.accept(jobContext);

    verify(repositoryService).listRepositoriesByPrefix(course, "jpa01");
    verify(repositoryService, times(3)).deleteRepository(eq(course), anyString());
    verify(jobContext, atLeastOnce()).log(contains("Starting deletion"));
    verify(jobContext, atLeastOnce()).log(contains("Found 3 repositories"));
    verify(jobContext, atLeastOnce()).log(contains("Deletion complete"));
  }

  @Test
  void testAccept_noReposFound() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    when(repositoryService.listRepositoriesByPrefix(course, "jpa01"))
        .thenReturn(Collections.emptyList());

    DeleteAssignmentRepositoriesJob job =
        DeleteAssignmentRepositoriesJob.builder()
            .course(course)
            .repositoryService(repositoryService)
            .assignmentName("jpa01")
            .build();

    job.accept(jobContext);

    verify(repositoryService).listRepositoriesByPrefix(course, "jpa01");
    verify(repositoryService, never()).deleteRepository(any(), any());
    verify(jobContext, atLeastOnce()).log(contains("No repositories found"));
  }

  @Test
  void testAccept_withFailures() throws Exception {
    Course course = Course.builder().orgName("test-org").installationId("123").build();

    List<String> repos = Arrays.asList("jpa01-student1", "jpa01-student2");

    when(repositoryService.listRepositoriesByPrefix(course, "jpa01")).thenReturn(repos);

    doNothing().when(repositoryService).deleteRepository(course, "jpa01-student1");
    doThrow(new RuntimeException("Delete failed"))
        .when(repositoryService)
        .deleteRepository(course, "jpa01-student2");

    DeleteAssignmentRepositoriesJob job =
        DeleteAssignmentRepositoriesJob.builder()
            .course(course)
            .repositoryService(repositoryService)
            .assignmentName("jpa01")
            .build();

    job.accept(jobContext);

    verify(repositoryService, times(2)).deleteRepository(eq(course), anyString());
    verify(jobContext, atLeastOnce()).log(contains("Successfully deleted: jpa01-student1"));
    verify(jobContext, atLeastOnce()).log(contains("Failed to delete jpa01-student2"));
    verify(jobContext, atLeastOnce()).log(contains("Success: 1"));
    verify(jobContext, atLeastOnce()).log(contains("Failed: 1"));
  }
}
