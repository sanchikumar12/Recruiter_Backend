package com.pi.job.specification;

import com.pi.job.entity.Job;
import com.pi.job.enums.EmploymentType;
import com.pi.job.enums.ExperienceLevel;
import com.pi.job.enums.JobStatus;
import com.pi.job.enums.WorkMode;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class JobSpecification {

    public static Specification<Job> filterJobs(
            String keyword,
            String location,
            WorkMode workMode,
            EmploymentType employmentType,
            ExperienceLevel experienceLevel,
            BigDecimal minSalary,
            BigDecimal maxSalary,
            JobStatus status) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            if (keyword != null && !keyword.trim().isEmpty()) {
                String pattern = "%" + keyword.trim().toLowerCase() + "%";
                Predicate titlePredicate = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descPredicate = cb.like(cb.lower(root.get("description")), pattern);
                Predicate deptPredicate = cb.like(cb.lower(root.get("department")), pattern);
                predicates.add(cb.or(titlePredicate, descPredicate, deptPredicate));
            }

            if (location != null && !location.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("location")), "%" + location.trim().toLowerCase() + "%"));
            }

            if (workMode != null) {
                predicates.add(cb.equal(root.get("workMode"), workMode));
            }

            if (employmentType != null) {
                predicates.add(cb.equal(root.get("employmentType"), employmentType));
            }

            if (experienceLevel != null) {
                predicates.add(cb.equal(root.get("experienceLevel"), experienceLevel));
            }

            if (minSalary != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salaryMax"), minSalary));
            }

            if (maxSalary != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salaryMin"), maxSalary));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
