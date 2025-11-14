## Lifecycle of Frontiers Entities

In Frontiers, there are several different entities that all interact:

- _User_
- _Course_
- _RosterStudent_
- _CourseStaff_

A _User_ is an individual account associated with an individual person. Each person who uses Frontiers should only have 1 account.

A _Course_ is an offering of an individual class. One _course_ is only used for 1 _term_.

A _RosterStudent_ is a single student on the roster of a particular _Course_. Like a _Course_, the lifecycle of a student is for only one term. Each _RosterStudent_ is associated with one _User_. However, one _User_ can be associated with more than one _RosterStudent_.

A _CourseStaff_ is a single staff member on a particular _Course_. Like a _RosterStudent_, it is associated with a single _Course_ and a single _User_. They also have a lifecycle of 1 _term_. One _User_ may be associated with many _CourseStaff_ instances.

## Using Frontiers

An instructor should first create a _Course_ through _Swagger_. Then, they should upload their Roster in eGrades format. It will then create the associated Roster Students. Then, they should link their Course to their GitHub Organization, using the url `https://frontiers.dokku-00.cs.ucsb.edu/api/courses/redirect?courseId=<courseId>` where `courseId` is the ID of their course. GitHub will then prompt them for which Organization they'd like to install it to. When they do, they will then be returned to Frontiers.

Then, as students sign into Frontiers and link their GitHubs, their _User_ accounts will be automatically linked with their associated Roster Students.

## Database Design

![img.png](img.png)
