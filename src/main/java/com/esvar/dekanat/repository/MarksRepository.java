package com.esvar.dekanat.repository;

import com.esvar.dekanat.entity.ControlMethodEntity;
import com.esvar.dekanat.entity.MarksEntity;
import com.esvar.dekanat.entity.PlansEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MarksRepository extends JpaRepository<MarksEntity, Long> {

    /**
     * Перевіряє, чи існує запис у таблиці marks за комбінацією student_id, plan_id та control_method_id.
     *
     * @param studentId       ID студента.
     * @param planId          ID плану.
     * @param controlMethodId ID методу контролю.
     * @return true, якщо запис існує; false — якщо ні.
     */
    boolean existsByStudentIdAndPlanIdAndControlMethodId(Long studentId, Long planId, Long controlMethodId);

    List<MarksEntity> findAllByStudentIdAndPlanIdAndControlMethodIdOrderByIdDesc(Long studentId, Long planId, Long controlMethodId);

    default Optional<MarksEntity> findByStudentIdAndPlanIdAndControlMethodId(Long studentId, Long planId, Long controlMethodId) {
        return findAllByStudentIdAndPlanIdAndControlMethodIdOrderByIdDesc(studentId, planId, controlMethodId)
                .stream()
                .findFirst();
    }

    /**
     * Знайти оцінку за ID студента та ID плану.
     *
     * @param studentId ID студента.
     * @param planId    ID плану.
     * @return Optional<MarksEntity> - знайдена оцінка або порожній Optional.
     */
    @Query("""
        SELECT m FROM MarksEntity m
        WHERE m.student.id = :studentId AND m.plan.id = :planId
    """)
    Optional<MarksEntity> findByStudentIdAndPlanId(Long studentId, Long planId);
    @Query("SELECT MAX(m.id) FROM MarksEntity m")
    Optional<Long> findMaxId();

    List<MarksEntity> findByPlan(PlansEntity plansEntity);

    @Query("""
        SELECT m FROM MarksEntity m
        WHERE m.plan = :plansEntity
          AND m.controlMethod = :controlMethod
          AND m.id = (
              SELECT MAX(m2.id)
              FROM MarksEntity m2
              WHERE m2.student = m.student
                AND m2.plan = m.plan
                AND m2.controlMethod = m.controlMethod
          )
    """)
    List<MarksEntity> findByPlanAndControlMethod(@Param("plansEntity") PlansEntity plansEntity,
                                                 @Param("controlMethod") ControlMethodEntity controlMethod);

    List<MarksEntity> findByStudentId(Long studentId);

    @Query("SELECT DISTINCT m.student.id FROM MarksEntity m WHERE m.plan.id = :planId")
    List<Long> findDistinctStudentIdsByPlanId(@Param("planId") Long planId);

    @Modifying
    @Query("DELETE FROM MarksEntity m WHERE m.plan.id = :planId")
    void deleteByPlanId(@Param("planId") Long planId);

    @Modifying
    @Query("DELETE FROM MarksEntity m WHERE m.plan.id = :planId AND m.student.id IN :studentIds")
    void deleteByPlanIdAndStudentIds(@Param("planId") Long planId, @Param("studentIds") List<Long> studentIds);
}
