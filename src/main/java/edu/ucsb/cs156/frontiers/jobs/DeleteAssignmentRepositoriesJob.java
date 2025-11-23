package edu.ucsb.cs156.frontiers.jobs;

import edu.ucsb.cs156.frontiers.entities.Course;
import edu.ucsb.cs156.frontiers.services.RepositoryService;
import edu.ucsb.cs156.frontiers.services.jobs.JobContext;
import edu.ucsb.cs156.frontiers.services.jobs.JobContextConsumer;
import java.util.List;
import lombok.Builder;

@Builder
public class DeleteAssignmentRepositoriesJob implements JobContextConsumer {
  Course course;
  RepositoryService repositoryService;
  String assignmentName;

  @Override
  public void accept(JobContext ctx) throws Exception {
    ctx.log("Starting deletion of repos for assignment: " + assignmentName);

    // Get all repos matching the assignment naming convention
    List<String> reposToDelete = repositoryService.listRepositoriesByPrefix(course, assignmentName);

    if (reposToDelete.isEmpty()) {
      ctx.log("No repositories found matching prefix: " + assignmentName);
      return;
    }

    ctx.log("Found " + reposToDelete.size() + " repositories to delete");

    int successCount = 0;
    int failureCount = 0;

    for (String repoName : reposToDelete) {
      try {
        repositoryService.deleteRepository(course, repoName);
        ctx.log("Successfully deleted: " + repoName);
        successCount++;
      } catch (Exception e) {
        ctx.log("Failed to delete " + repoName + ": " + e.getMessage());
        failureCount++;
      }
    }

    ctx.log("Deletion complete. Success: " + successCount + ", Failed: " + failureCount);
  }
}
