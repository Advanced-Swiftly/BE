package com.advancedswiftly.pawfit.admin.infrastructure;

import com.advancedswiftly.pawfit.admin.domain.EventDraw;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventDrawRepository extends JpaRepository<EventDraw, Long> {
}
