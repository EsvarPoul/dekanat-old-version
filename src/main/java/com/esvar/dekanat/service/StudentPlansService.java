package com.esvar.dekanat.service;

import com.esvar.dekanat.entity.PlansEntity;
import com.esvar.dekanat.entity.StudentEntity;
import com.esvar.dekanat.entity.StudentGroupEntity;
import com.esvar.dekanat.entity.StudentPlansEntity;
import com.esvar.dekanat.entity.StudentPlansPK;
import com.esvar.dekanat.repository.PlanRepository;
import com.esvar.dekanat.repository.StudentPlansRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StudentPlansService{

    private final StudentPlansRepository studentPlansRepository;
    private final StudentService studentService;
    private final PlanRepository planRepository;
    private final MarksInitializerService marksInitializerService;


    public StudentPlansService(StudentPlansRepository studentPlansRepository,
                               StudentService studentService,
                               PlanRepository planRepository,
                               MarksInitializerService marksInitializerService) {
        this.studentPlansRepository = studentPlansRepository;
        this.studentService = studentService;
        this.planRepository = planRepository;
        this.marksInitializerService = marksInitializerService;
    }

    /**
     * Зберігає пов'язання між студентом і навчальним планом.
     *
     * @param studentPlan StudentPlansEntity - об'єкт для збереження.
     */
    public void saveStudentPlan(StudentPlansEntity studentPlan) {
        if (studentPlan == null || studentPlan.getStudent() == null || studentPlan.getPlan() == null) {
            throw new IllegalArgumentException("Студент і план повинні бути задані.");
        }

        // Перевіряємо, чи запис вже існує (унікальність за (student_id, plan_id))
        boolean exists = studentPlansRepository.existsByStudentIdAndPlanId(
                studentPlan.getStudent().getId(),
                studentPlan.getPlan().getId()
        );

        if (!exists) {
            // Якщо запис не існує, зберігаємо новий
            studentPlan.setId(new StudentPlansPK(
                    studentPlan.getStudent().getId(),
                    studentPlan.getPlan().getId()
            ));
            studentPlansRepository.save(studentPlan);
        } else {
            // Якщо запис вже існує, можна або проігнорувати, або оновити його

        }
    }

    /**
     * Оновлює записи у таблиці student_plans для певного плану.
     *
     * @param updatedPlan PlansEntity - оновлений план.
     * @param studentIds  List<Long> - список ID студентів.
     * @param allowedGroups List<StudentGroupEntity> - групи, для яких дозволений вибір.
     */
    @Transactional
    public List<StudentEntity> updateStudentPlans(PlansEntity updatedPlan, List<Long> studentIds, List<StudentGroupEntity> allowedGroups) {
        return synchronizePlanAssignments(updatedPlan, studentIds, allowedGroups);
    }


    public void deleteStudentPlansByPlan(PlansEntity plan) {
        studentPlansRepository.deleteByPlan(plan);
    }

    @Transactional
    public void deleteByPlanId(Long planId) {
        studentPlansRepository.deleteByPlanId(planId); // Викликаємо кастомний запит
    }

    public List<StudentEntity> getStudentByPlan(PlansEntity plan) {
        return studentPlansRepository.findByPlan(plan)
                .stream()
                .map(StudentPlansEntity::getStudent)
                .collect(Collectors.toList());
    }

    public List<StudentEntity> getStudentsByPlanAndGroup(PlansEntity plan, StudentGroupEntity group) {
        List<StudentEntity> students = getStudentByPlan(plan);
        if (group == null || group.getId() == null) {
            return students;
        }
        return students.stream()
                .filter(student -> isStudentInGroup(student, group))
                .collect(Collectors.toList());
    }

    /**
     * Повертає усі записи student_plans для вказаного студента.
     *
     * @param student студент
     * @return список StudentPlansEntity
     */
    public List<StudentPlansEntity> getPlansForStudent(StudentEntity student) {
        if (student == null) {
            return new ArrayList<>();
        }
        return studentPlansRepository.findByStudent(student);
    }

    @Transactional
    public int synchronizeStudentWithCurrentGroupPlans(StudentEntity student) {
        if (student == null || student.getId() == null || student.getGroup() == null) {
            return 0;
        }

        List<PlansEntity> currentGroupPlans = planRepository.findNonElectiveByGroup(student.getGroup());
        int added = 0;
        for (PlansEntity plan : currentGroupPlans) {
            if (plan == null || plan.getId() == null) {
                continue;
            }
            boolean exists = studentPlansRepository.existsByStudentIdAndPlanId(student.getId(), plan.getId());
            if (exists) {
                continue;
            }
            StudentPlansEntity studentPlan = new StudentPlansEntity();
            studentPlan.setStudent(student);
            studentPlan.setPlan(plan);
            studentPlan.setId(new StudentPlansPK(student.getId(), plan.getId()));
            studentPlansRepository.save(studentPlan);
            marksInitializerService.initializeMarksForPlan(plan, List.of(student));
            added++;
        }
        return added;
    }

    @Transactional
    public List<StudentEntity> synchronizePlanAssignments(PlansEntity plan, List<Long> studentIds, List<StudentGroupEntity> allowedGroups) {
        if (plan == null || plan.getId() == null) {
            throw new IllegalArgumentException("План повинен бути заданий.");
        }

        Set<Long> allowedGroupIds = buildAllowedGroupIds(plan, allowedGroups);
        List<StudentEntity> targetStudents = resolveAndValidateStudents(studentIds, allowedGroupIds);
        return synchronizeInternal(plan, targetStudents);
    }

    @Transactional(readOnly = true)
    public List<Long> getStudentIdsByPlan(PlansEntity plan) {
        if (plan == null || plan.getId() == null) {
            return List.of();
        }
        return studentPlansRepository.findStudentIdsByPlanId(plan.getId());
    }

    private List<StudentEntity> synchronizeInternal(PlansEntity plan, List<StudentEntity> targetStudents) {
        List<StudentPlansEntity> existingLinks = studentPlansRepository.findByPlan(plan);
        List<StudentEntity> existingStudents = existingLinks.stream()
                .map(StudentPlansEntity::getStudent)
                .toList();

        for (StudentEntity student : targetStudents) {
            boolean exists = existingStudents.stream()
                    .anyMatch(s -> s.getId().equals(student.getId()));
            if (!exists) {
                StudentPlansEntity studentPlan = new StudentPlansEntity();
                studentPlan.setStudent(student);
                studentPlan.setPlan(plan);
                studentPlan.setId(new StudentPlansPK(student.getId(), plan.getId()));
                studentPlansRepository.save(studentPlan);
            }
        }

        List<Long> desiredIds = targetStudents.stream()
                .map(StudentEntity::getId)
                .toList();

        List<StudentEntity> toRemove = existingStudents.stream()
                .filter(s -> !desiredIds.contains(s.getId()))
                .toList();

        if (!toRemove.isEmpty()) {
            List<Long> removeIds = toRemove.stream().map(StudentEntity::getId).toList();
            studentPlansRepository.deleteByPlanIdAndStudentIds(plan.getId(), removeIds);
        }

        return targetStudents;
    }

    private List<StudentEntity> resolveAndValidateStudents(List<Long> studentIds, Set<Long> allowedGroupIds) {
        if (studentIds == null || studentIds.isEmpty()) {
            return List.of();
        }

        List<StudentEntity> students = studentService.getStudentsByIds(studentIds);
        Map<Long, StudentEntity> studentsById = students.stream()
                .collect(Collectors.toMap(StudentEntity::getId, Function.identity()));

        List<Long> missingStudents = studentIds.stream()
                .filter(id -> !studentsById.containsKey(id))
                .toList();

        if (!missingStudents.isEmpty()) {
            throw new IllegalArgumentException("Не знайдено студентів з ID: " + missingStudents);
        }

        if (!allowedGroupIds.isEmpty()) {
            List<StudentEntity> mismatchedStudents = studentsById.values().stream()
                    .filter(student -> student.getGroup() == null || !allowedGroupIds.contains(student.getGroup().getId()))
                    .toList();

            if (!mismatchedStudents.isEmpty()) {
                String invalidNames = mismatchedStudents.stream()
                        .map(StudentEntity::getFullName)
                        .collect(Collectors.joining(", "));
                throw new IllegalArgumentException("Деякі студенти не належать до вибраної групи: " + invalidNames);
            }
        }

        return studentIds.stream()
                .map(studentsById::get)
                .filter(Objects::nonNull)
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(LinkedHashSet::new),
                        ArrayList::new
                ));
    }

    private Set<Long> buildAllowedGroupIds(PlansEntity plan, List<StudentGroupEntity> allowedGroups) {
        if (allowedGroups != null && !allowedGroups.isEmpty()) {
            return allowedGroups.stream()
                    .filter(Objects::nonNull)
                    .map(StudentGroupEntity::getId)
                    .collect(Collectors.toCollection(HashSet::new));
        }
        if (plan.getGroups() == null || plan.getGroups().isEmpty()) {
            return Set.of();
        }
        return plan.getGroups().stream()
                .filter(Objects::nonNull)
                .map(StudentGroupEntity::getId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isStudentInGroup(StudentEntity student, StudentGroupEntity group) {
        if (student == null || student.getGroup() == null
                || student.getGroup().getId() == null || group == null || group.getId() == null) {
            return false;
        }
        return Objects.equals(student.getGroup().getId(), group.getId());
    }
}
