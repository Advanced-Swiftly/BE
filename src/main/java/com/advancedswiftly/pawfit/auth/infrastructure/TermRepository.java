package com.advancedswiftly.pawfit.auth.infrastructure;

import com.advancedswiftly.pawfit.auth.domain.Term;
import com.advancedswiftly.pawfit.auth.domain.UserRole;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TermRepository extends JpaRepository<Term, Long> {

    List<Term> findAllByRoleAndActiveTrueOrderById(UserRole role);
}
