package com.esvar.dekanat.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<UserModel, Long> {
    List<UserModel> findAllByEmailIgnoreCaseOrderByEnabledDescIdDesc(String email);

    default Optional<UserModel> findByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return Optional.empty();
        }
        return findAllByEmailIgnoreCaseOrderByEnabledDescIdDesc(email.trim())
                .stream()
                .findFirst();
    }

    UserModel findByLastnameAndFirstnameAndPatronymic(String p, String i, String b);
}
